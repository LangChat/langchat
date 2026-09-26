package cn.langchat.core.runtime.mcp;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MCP 运行时配置。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Data
@Component
@ConfigurationProperties(prefix = "langchat.mcp")
public class McpProperties {

    /**
     * MCP 客户端会话缓存时长。缓存窗口内（默认 1 小时）同一个 MCP 服务保持连接，
     * 多次 Agent 调用直接复用，不重复启动 MCP 服务；过期后由调度任务回收，
     * 下次调用时再重新创建。配置变更或主动暂停时立即失效。
     */
    private Duration sessionTtl = Duration.ofHours(1);

    /**
     * 过期会话回收间隔。
     */
    private Duration sweepInterval = Duration.ofMinutes(5);

    /**
     * 默认工具执行超时（MCP 未配置超时时间时使用）。
     */
    private Duration toolExecutionTimeout = Duration.ofSeconds(30);
}
