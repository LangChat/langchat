package cn.langchat.claw.core.runtime.mcp;

import cn.langchat.claw.aigc.biz.entity.AigcMcp;
import cn.langchat.claw.aigc.biz.event.McpConfigChangedEvent;
import cn.langchat.claw.common.exception.BizException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * MCP 客户端管理器：基于 langchain4j-mcp 实现 MCP 服务的动态创建、复用与回收。
 *
 * <p>关键设计——MCP 客户端（及 Stdio 模式下启动的 MCP 服务进程）按 {@code mcpId}
 * 缓存并设置 TTL（默认 1 小时）：缓存窗口内的多次 Agent 调用直接复用同一个连接，
 * 不重复启动 MCP 服务；过期后由调度任务回收，下次调用时再重新创建。
 * 配置指纹变化（如修改了地址/镜像）或收到配置变更事件（主动暂停、删除）时立即失效。
 * Transport 支持 SSE、Streamable HTTP 与 Stdio（通过 Docker 启动镜像）。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableScheduling
public class McpClientManager implements DisposableBean {

    private static final TypeReference<Map<String, String>> HEADERS_TYPE = new TypeReference<>() {
    };

    private final McpProperties mcpProperties;
    private final ObjectMapper objectMapper;

    /** mcpId -> 会话。 */
    private final ConcurrentHashMap<String, McpSession> sessions = new ConcurrentHashMap<>();

    /**
     * 为 Agent 运行时构建 MCP 工具提供器（langchain4j ToolProvider）。
     * 单个 MCP 创建失败只跳过该服务，不影响其他 MCP 与本次对话。
     */
    public ToolProvider buildToolProvider(List<AigcMcp> mcps) {
        List<McpClient> clients = new ArrayList<>();
        for (AigcMcp mcp : mcps) {
            try {
                clients.add(getOrCreateClient(mcp));
            } catch (Exception exception) {
                log.error("MCP 客户端创建失败，已跳过，mcpId={}, name={}", mcp.getId(), mcp.getName(), exception);
            }
        }
        if (clients.isEmpty()) {
            return null;
        }
        return McpToolProvider.builder()
                .mcpClients(clients)
                .failIfOneServerFails(false)
                .build();
    }

    /**
     * 获取或创建 MCP 客户端：命中缓存且未过期、配置未变化时直接复用。
     */
    public McpClient getOrCreateClient(AigcMcp mcp) {
        String key = requireMcpId(mcp);
        String fingerprint = fingerprint(mcp);
        McpSession session = sessions.get(key);
        if (session != null && session.fingerprint().equals(fingerprint) && !isExpired(session)) {
            session.touch();
            return session.client();
        }
        if (session != null) {
            closeQuietly(session);
            sessions.remove(key, session);
        }
        McpClient client = createClient(mcp);
        sessions.put(key, new McpSession(client, fingerprint));
        log.info("MCP 客户端已创建，mcpId={}, name={}, transport={}", key, mcp.getName(), mcp.getTransport());
        return client;
    }

    /**
     * 主动停止指定 MCP 的客户端连接（配置变更 / 主动暂停）。
     */
    public void stopClient(String mcpId) {
        if (!StringUtils.hasText(mcpId)) {
            return;
        }
        McpSession session = sessions.remove(mcpId);
        if (session != null) {
            closeQuietly(session);
            log.info("MCP 客户端已停止，mcpId={}", mcpId);
        }
    }

    @EventListener
    public void onMcpConfigChanged(McpConfigChangedEvent event) {
        stopClient(event.mcpId());
    }

    /**
     * 定期回收超过 TTL 的会话，释放本地 MCP 服务进程与网络连接。
     */
    @Scheduled(fixedDelayString = "#{@mcpProperties.sweepInterval.toMillis()}")
    public void evictExpiredSessions() {
        sessions.forEach((key, session) -> {
            if (isExpired(session)) {
                log.info("MCP 会话超过 TTL，自动回收，mcpId={}", key);
                stopClient(key);
            }
        });
    }

    @Override
    public void destroy() {
        sessions.forEach((key, session) -> closeQuietly(session));
        sessions.clear();
    }

    private McpClient createClient(AigcMcp mcp) {
        McpTransport transport = buildTransport(mcp);
        DefaultMcpClient client = DefaultMcpClient.builder()
                .transport(transport)
                .toolExecutionTimeout(mcp.getTimeout() == null
                        ? mcpProperties.getToolExecutionTimeout()
                        : Duration.ofSeconds(mcp.getTimeout()))
                .cacheToolList(true)
                .build();
        try {
            // 主动触发一次初始化与工具发现，尽早暴露连接问题并预热连接
            int toolCount = client.listTools().size();
            log.info("MCP 服务握手成功，mcpId={}, name={}, tools={}", mcp.getId(), mcp.getName(), toolCount);
        } catch (RuntimeException exception) {
            closeQuietly(new McpSession(client, ""));
            throw exception;
        }
        return client;
    }

    private McpTransport buildTransport(AigcMcp mcp) {
        String transport = StringUtils.hasText(mcp.getTransport())
                ? mcp.getTransport().trim().toUpperCase()
                : "SSE";
        Duration timeout = Duration.ofSeconds(mcp.getTimeout() == null ? 30 : mcp.getTimeout());
        Map<String, String> headers = parseHeaders(mcp.getHeaders());
        return switch (transport) {
            // langchain4j 1.19 起移除了旧的 HttpMcpTransport（HTTP+SSE 协议），
            // MCP 规范已统一到 Streamable HTTP：单端点、POST 请求体内按需返回 SSE 流。
            case "SSE", "HTTP" -> {
                if (!StringUtils.hasText(mcp.getSseUrl())) {
                    throw new BizException("MCP_CONFIG_INVALID", "网络模式必须配置服务端点地址");
                }
                yield StreamableHttpMcpTransport.builder()
                        .url(mcp.getSseUrl().trim())
                        .customHeaders(headers)
                        .timeout(timeout)
                        .build();
            }
            case "STDIO" -> {
                if (!StringUtils.hasText(mcp.getDockerImage())) {
                    throw new BizException("MCP_CONFIG_INVALID", "STDIO 模式必须配置 Docker 镜像");
                }
                yield StdioMcpTransport.builder()
                        .command(buildDockerCommand(mcp))
                        .build();
            }
            default -> throw new BizException("MCP_CONFIG_INVALID", "不支持的 MCP 传输类型：" + transport);
        };
    }

    private List<String> buildDockerCommand(AigcMcp mcp) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        if (StringUtils.hasText(mcp.getDockerHost())) {
            command.add("-H");
            command.add(mcp.getDockerHost().trim());
        }
        command.add("run");
        command.add("--rm");
        command.add("-i");
        command.add(mcp.getDockerImage().trim());
        return command;
    }

    private Map<String, String> parseHeaders(String headersJson) {
        if (!StringUtils.hasText(headersJson)) {
            return Map.of();
        }
        try {
            Map<String, String> headers = objectMapper.readValue(headersJson, HEADERS_TYPE);
            return headers == null ? Map.of() : headers;
        } catch (Exception exception) {
            log.warn("MCP 请求头解析失败，忽略自定义请求头，headers={}", headersJson);
            return Map.of();
        }
    }

    private String requireMcpId(AigcMcp mcp) {
        if (!StringUtils.hasText(mcp.getId())) {
            throw new BizException("MCP_CONFIG_INVALID", "MCP 配置缺少 id");
        }
        return mcp.getId();
    }

    /**
     * 配置指纹：任一连接参数变化即视为新配置，重建客户端。
     */
    private String fingerprint(AigcMcp mcp) {
        return String.join("|",
                nvl(mcp.getTransport()),
                nvl(mcp.getSseUrl()),
                nvl(mcp.getHeaders()),
                nvl(mcp.getDockerImage()),
                nvl(mcp.getDockerHost()),
                String.valueOf(mcp.getTimeout() == null ? -1 : mcp.getTimeout()));
    }

    private boolean isExpired(McpSession session) {
        return session.isExpired(mcpProperties.getSessionTtl().toMillis());
    }

    private void closeQuietly(McpSession session) {
        try {
            session.client().close();
        } catch (Exception exception) {
            log.warn("MCP 客户端关闭失败", exception);
        }
    }

    private String nvl(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 会话：客户端 + 配置指纹 + 最近访问时间（volatile，惰性回收与并发访问共享）。
     */
    private static final class McpSession {

        private final McpClient client;
        private final String fingerprint;
        private volatile long lastAccess;

        private McpSession(McpClient client, String fingerprint) {
            this.client = client;
            this.fingerprint = fingerprint;
            this.lastAccess = System.currentTimeMillis();
        }

        private McpClient client() {
            return client;
        }

        private String fingerprint() {
            return fingerprint;
        }

        private void touch() {
            this.lastAccess = System.currentTimeMillis();
        }

        private boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - lastAccess >= ttlMillis;
        }
    }
}
