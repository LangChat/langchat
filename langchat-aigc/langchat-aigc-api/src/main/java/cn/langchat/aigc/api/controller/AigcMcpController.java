package cn.langchat.aigc.api.controller;

import cn.langchat.aigc.biz.entity.AigcMcp;
import cn.langchat.aigc.biz.event.McpConfigChangedEvent;
import cn.langchat.aigc.biz.service.AigcMcpService;
import cn.langchat.common.core.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP 管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/aigc/mcp")
@RequiredArgsConstructor
@Slf4j
public class AigcMcpController {

    private final AigcMcpService aigcMcpService;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping
    public ApiResponse<List<AigcMcp>> list() {
        return ApiResponse.success(aigcMcpService.lambdaQuery().orderByDesc(AigcMcp::getCreateTime).list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcMcp> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcMcpService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcMcp mcp) {
        log.info("新增MCP配置，name={}", mcp.getName());
        return ApiResponse.success(aigcMcpService.save(mcp));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcMcp mcp) {
        mcp.setId(id);
        log.info("更新MCP配置，id={}", id);
        boolean updated = aigcMcpService.updateById(mcp);
        // 通知运行时立即停用旧连接（主动暂停），下次调用按新配置重建
        eventPublisher.publishEvent(new McpConfigChangedEvent(id));
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除MCP配置，id={}", id);
        boolean removed = aigcMcpService.removeById(id);
        eventPublisher.publishEvent(new McpConfigChangedEvent(id));
        return ApiResponse.success(removed);
    }
}
