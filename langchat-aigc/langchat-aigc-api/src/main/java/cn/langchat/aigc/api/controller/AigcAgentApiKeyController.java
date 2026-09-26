package cn.langchat.aigc.api.controller;

import cn.langchat.aigc.biz.entity.AigcAgentApiKey;
import cn.langchat.aigc.biz.entity.AigcMessage;
import cn.langchat.aigc.biz.service.AigcAgentApiKeyService;
import cn.langchat.aigc.biz.service.AigcMessageService;
import cn.langchat.common.core.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent API Key 管理控制器。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@RestController
@RequestMapping("/api/v1/aigc/agents")
@RequiredArgsConstructor
@Slf4j
public class AigcAgentApiKeyController {

    private final AigcAgentApiKeyService aigcAgentApiKeyService;
    private final AigcMessageService aigcMessageService;

    /**
     * 查询指定 Agent 的 API Key 列表。
     */
    @GetMapping("/{agentId}/api-keys")
    public ApiResponse<List<AigcAgentApiKey>> list(@PathVariable("agentId") String agentId) {
        return ApiResponse.success(aigcAgentApiKeyService.lambdaQuery()
                .eq(AigcAgentApiKey::getAgentId, agentId)
                .orderByDesc(AigcAgentApiKey::getCreateTime)
                .list());
    }

    /**
     * 新建 API Key（服务端生成密钥值）。
     */
    @PostMapping("/{agentId}/api-keys")
    public ApiResponse<AigcAgentApiKey> create(
            @PathVariable("agentId") String agentId,
            @RequestBody AigcAgentApiKey payload
    ) {
        AigcAgentApiKey apiKey = new AigcAgentApiKey();
        apiKey.setAgentId(agentId);
        apiKey.setName(payload.getName());
        apiKey.setRemark(payload.getRemark());
        apiKey.setStatus("ENABLED");
        apiKey.setApiKey(aigcAgentApiKeyService.generateApiKey());
        apiKey.setCallCount(0);
        apiKey.setInputTokens(0L);
        apiKey.setOutputTokens(0L);
        log.info("新建 Agent API Key，agentId={}, name={}", agentId, payload.getName());
        aigcAgentApiKeyService.save(apiKey);
        return ApiResponse.success(apiKey);
    }

    /**
     * 更新 API Key（名称、备注、启停状态）。
     */
    @PutMapping("/api-keys/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcAgentApiKey payload) {
        AigcAgentApiKey apiKey = new AigcAgentApiKey();
        apiKey.setId(id);
        apiKey.setName(payload.getName());
        apiKey.setRemark(payload.getRemark());
        apiKey.setStatus(payload.getStatus());
        log.info("更新 Agent API Key，id={}, status={}", id, payload.getStatus());
        return ApiResponse.success(aigcAgentApiKeyService.updateById(apiKey));
    }

    /**
     * 删除 API Key。
     */
    @DeleteMapping("/api-keys/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除 Agent API Key，id={}", id);
        return ApiResponse.success(aigcAgentApiKeyService.removeById(id));
    }

    /**
     * 查询 API Key 维度的消息日志（固定会话：apikey-{keyId}）。
     */
    @GetMapping("/api-keys/{id}/messages")
    public ApiResponse<List<AigcMessage>> messages(@PathVariable("id") String id) {
        return ApiResponse.success(aigcMessageService.lambdaQuery()
                .eq(AigcMessage::getConversationId, "apikey-" + id)
                .orderByAsc(AigcMessage::getCreateTime)
                .list());
    }
}
