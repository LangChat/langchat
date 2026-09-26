package cn.langchat.claw.core.chat.controller;

import cn.langchat.claw.aigc.biz.entity.AigcAgentApiKey;
import cn.langchat.claw.aigc.biz.entity.AigcMessage;
import cn.langchat.claw.aigc.biz.service.AigcAgentApiKeyService;
import cn.langchat.claw.aigc.biz.service.AigcMessageService;
import cn.langchat.claw.core.chat.model.request.AgentChatStreamRequest;
import cn.langchat.claw.core.chat.model.request.ChatCompletionMessage;
import cn.langchat.claw.core.chat.model.request.ChatCompletionRequest;
import cn.langchat.claw.core.chat.service.AgentChatRuntimeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * OpenAI 兼容接口。
 *
 * <p>外部系统通过 {@code POST /v1/chat/completions} + {@code Authorization: Bearer <apiKey>}
 * 与指定 Agent 应用交互，接口形态与 OpenAI Chat Completions 保持一致。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class OpenAiAgentChatController {

    private static final String DONE_MARKER = "[DONE]";

    private final AigcAgentApiKeyService aigcAgentApiKeyService;
    private final AigcMessageService aigcMessageService;
    private final AgentChatRuntimeService agentChatRuntimeService;
    private final ObjectMapper objectMapper;

    /**
     * OpenAI 兼容聊天补全。
     */
    @PostMapping(value = "/chat/completions")
    public Object chatCompletions(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ChatCompletionRequest request
    ) {
        AuthResult auth = authorize(authorization);
        if (auth.error != null) {
            return auth.error;
        }
        AigcAgentApiKey apiKey = auth.apiKey;

        String userMessage = resolveLatestUserMessage(request);
        if (userMessage == null || userMessage.isBlank()) {
            return errorResponse(HttpStatus.BAD_REQUEST, "messages 中缺少用户消息");
        }

        AgentChatStreamRequest streamRequest = new AgentChatStreamRequest();
        // 每个 API Key 使用固定会话，便于按密钥查看消息日志
        streamRequest.setConversationId("apikey-" + apiKey.getId());
        streamRequest.setMessage(userMessage);
        String agentId = apiKey.getAgentId();

        boolean stream = Boolean.TRUE.equals(request.getStream());
        log.info("OpenAI 兼容调用，agentId={}, apiKeyId={}, stream={}", agentId, apiKey.getId(), stream);

        Flux<ServerSentEvent<String>> eventFlux = agentChatRuntimeService.streamChat(agentId, streamRequest);

        if (stream) {
            return eventFlux.doOnComplete(() -> recordCall(apiKey.getId()));
        }
        return aggregateCompletion(eventFlux, apiKey.getId(), request);
    }

    /**
     * 校验 API Key 并确保启用。
     */
    private AuthResult authorize(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return AuthResult.fail(errorResponse(HttpStatus.UNAUTHORIZED, "缺少 API Key，请在 Authorization: Bearer <key> 中携带"));
        }
        String token = authorization.substring("Bearer ".length()).trim();
        AigcAgentApiKey apiKey = aigcAgentApiKeyService.getByApiKey(token);
        if (apiKey == null) {
            return AuthResult.fail(errorResponse(HttpStatus.UNAUTHORIZED, "API Key 无效"));
        }
        if (!"ENABLED".equalsIgnoreCase(apiKey.getStatus())) {
            return AuthResult.fail(errorResponse(HttpStatus.FORBIDDEN, "API Key 已停用"));
        }
        return AuthResult.ok(apiKey);
    }

    /**
     * 聚合流式事件为一次完整的 OpenAI 补全响应。
     */
    private ResponseEntity<Map<String, Object>> aggregateCompletion(
            Flux<ServerSentEvent<String>> eventFlux,
            String apiKeyId,
            ChatCompletionRequest request
    ) {
        StringBuilder content = new StringBuilder();
        AtomicReference<String> completionId =
                new AtomicReference<>("chatcmpl-" + Long.toHexString(System.currentTimeMillis()));
        AtomicReference<String> model = new AtomicReference<>(request.getModel() == null ? "" : request.getModel());
        AtomicReference<String> finishReason = new AtomicReference<>("stop");

        List<String> lines = eventFlux
                .map(ServerSentEvent::data)
                .collectList()
                .block(Duration.ofMinutes(5));
        if (lines == null) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "模型未返回任何内容");
        }
        for (String line : lines) {
            if (line == null || line.isBlank() || DONE_MARKER.equals(line)) {
                continue;
            }
            JsonNode chunk = readTree(line);
            if (chunk == null) {
                continue;
            }
            if (chunk.path("id").asText("").startsWith("chatcmpl-")) {
                completionId.set(chunk.path("id").asText());
            }
            if (!chunk.path("model").asText("").isBlank()) {
                model.set(chunk.path("model").asText());
            }
            JsonNode choices = chunk.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode first = choices.get(0);
                String deltaContent = first.path("delta").path("content").asText("");
                if (!deltaContent.isEmpty()) {
                    content.append(deltaContent);
                }
                String reason = first.path("finish_reason").asText("");
                if (!reason.isEmpty()) {
                    finishReason.set(reason);
                }
            }
        }

        recordCall(apiKeyId);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", content.toString());

        Map<String, Object> choice = new LinkedHashMap<>();
        choice.put("index", 0);
        choice.put("message", message);
        choice.put("finish_reason", finishReason.get());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", completionId.get());
        body.put("object", "chat.completion");
        body.put("created", System.currentTimeMillis() / 1000);
        body.put("model", model.get());
        body.put("choices", List.of(choice));
        body.put("usage", buildUsage(apiKeyId));
        return ResponseEntity.ok(body);
    }

    /**
     * 记录密钥调用统计（Token 取该会话最后一条 assistant 消息）。
     */
    private void recordCall(String apiKeyId) {
        try {
            AigcMessage last = lastAssistantMessage(apiKeyId);
            if (last != null) {
                aigcAgentApiKeyService.recordCall(
                        apiKeyId,
                        last.getInputToken() == null ? 0L : last.getInputToken().longValue(),
                        last.getOutputToken() == null ? 0L : last.getOutputToken().longValue());
            } else {
                aigcAgentApiKeyService.recordCall(apiKeyId, 0L, 0L);
            }
        } catch (Exception ex) {
            log.warn("记录 API Key 调用统计失败，apiKeyId={}", apiKeyId, ex);
        }
    }

    /**
     * 组装 usage 统计。
     */
    private Map<String, Object> buildUsage(String apiKeyId) {
        AigcMessage last = lastAssistantMessage(apiKeyId);
        long inputTokens = last == null || last.getInputToken() == null ? 0 : last.getInputToken();
        long outputTokens = last == null || last.getOutputToken() == null ? 0 : last.getOutputToken();
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("prompt_tokens", inputTokens);
        usage.put("completion_tokens", outputTokens);
        usage.put("total_tokens", inputTokens + outputTokens);
        return usage;
    }

    /**
     * 查询密钥固定会话的最后一条 assistant 消息。
     */
    private AigcMessage lastAssistantMessage(String apiKeyId) {
        return aigcMessageService.lambdaQuery()
                .eq(AigcMessage::getConversationId, "apikey-" + apiKeyId)
                .eq(AigcMessage::getRole, "assistant")
                .orderByDesc(AigcMessage::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    /**
     * 取最后一条用户消息。
     */
    private String resolveLatestUserMessage(ChatCompletionRequest request) {
        List<ChatCompletionMessage> messages = request.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatCompletionMessage message = messages.get(i);
            if ("user".equalsIgnoreCase(message.getRole())) {
                return message.getContent();
            }
        }
        return null;
    }

    /**
     * 解析 JSON。
     */
    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * OpenAI 风格错误响应。
     */
    private ResponseEntity<Map<String, Object>> errorResponse(HttpStatus status, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("message", message);
        error.put("type", "invalid_request_error");
        return ResponseEntity.status(status).body(Map.of("error", error));
    }

    /**
     * 鉴权结果。
     */
    private record AuthResult(AigcAgentApiKey apiKey, ResponseEntity<Map<String, Object>> error) {

        static AuthResult ok(AigcAgentApiKey apiKey) {
            return new AuthResult(apiKey, null);
        }

        static AuthResult fail(ResponseEntity<Map<String, Object>> error) {
            return new AuthResult(null, error);
        }
    }
}
