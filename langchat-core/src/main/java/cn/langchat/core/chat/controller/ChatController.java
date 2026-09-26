package cn.langchat.core.chat.controller;

import cn.langchat.aigc.biz.entity.AigcConversation;
import cn.langchat.aigc.biz.entity.AigcMessage;
import cn.langchat.aigc.biz.service.AigcConversationService;
import cn.langchat.aigc.biz.service.AigcMessageService;
import cn.langchat.common.auth.AuthUtil;
import cn.langchat.common.core.ApiResponse;
import cn.langchat.common.exception.BizException;
import cn.langchat.core.chat.enums.ChatRoleEnum;
import cn.langchat.core.chat.model.request.AgentChatStreamRequest;
import cn.langchat.core.chat.model.request.ChatCompletionMessage;
import cn.langchat.core.chat.model.request.ChatCompletionRequest;
import cn.langchat.core.chat.service.AgentChatRuntimeService;
import cn.langchat.core.support.CoreErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 前端统一聊天控制器。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final AigcConversationService aigcConversationService;
    private final AigcMessageService aigcMessageService;
    private final AgentChatRuntimeService agentChatRuntimeService;

    /**
     * 查询当前用户的会话列表。
     */
    @GetMapping("/conversations")
    public ApiResponse<List<AigcConversation>> listConversations(
            @RequestParam(value = "agentId", required = false) String agentId
    ) {
        String userId = AuthUtil.getUserId();
        log.info("查询统一会话列表，userId={}, agentId={}", userId, agentId);
        return ApiResponse.success(aigcConversationService.lambdaQuery()
                .eq(hasText(userId), AigcConversation::getCreator, userId)
                .eq(hasText(agentId), AigcConversation::getAgentId, agentId)
                .orderByDesc(AigcConversation::getUpdateTime)
                .orderByDesc(AigcConversation::getCreateTime)
                .list());
    }

    /**
     * 查询指定会话的消息列表。
     *
     * <p>会话不存在时返回空列表（例如首次进入 Agent 构建页，固定会话尚未创建）。
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<AigcMessage>> listMessages(@PathVariable("conversationId") String conversationId) {
        if (!hasText(conversationId)) {
            throw new BizException(CoreErrorCode.INVALID_CHAT_REQUEST);
        }
        AigcConversation conversation = aigcConversationService.getById(conversationId);
        if (conversation == null) {
            return ApiResponse.success(List.of());
        }
        String userId = AuthUtil.getUserId();
        if (hasText(userId) && hasText(conversation.getCreator()) && !userId.equals(conversation.getCreator())) {
            throw new BizException(CoreErrorCode.CONVERSATION_NOT_FOUND);
        }
        log.info("查询统一会话消息，conversationId={}, agentId={}", conversationId, conversation.getAgentId());
        return ApiResponse.success(aigcMessageService.lambdaQuery()
                .eq(AigcMessage::getConversationId, conversationId)
                .orderByAsc(AigcMessage::getCreateTime)
                .list());
    }

    /**
     * 创建一次统一聊天补全流。
     */
    @PostMapping(value = "/completions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> createCompletion(@RequestBody ChatCompletionRequest request) {
        String agentId = resolveAgentId(request);
        AgentChatStreamRequest streamRequest = new AgentChatStreamRequest();
        streamRequest.setAttachments(request.getAttachments());
        streamRequest.setConversationId(request.getConversationId());
        streamRequest.setVariables(request.getVariables());
        streamRequest.setMessage(resolveLatestUserMessage(request));
        log.info("创建统一聊天补全请求，agentId={}, conversationId={}, stream={}",
                agentId,
                request.getConversationId(),
                request.getStream());
        return agentChatRuntimeService.streamChat(agentId, streamRequest);
    }

    /**
     * 校验会话访问权限。
     */
    private AigcConversation assertConversationAccess(String conversationId) {
        if (!hasText(conversationId)) {
            throw new BizException(CoreErrorCode.INVALID_CHAT_REQUEST);
        }
        AigcConversation conversation = aigcConversationService.getById(conversationId);
        if (conversation == null) {
            throw new BizException(CoreErrorCode.CONVERSATION_NOT_FOUND);
        }
        String userId = AuthUtil.getUserId();
        if (hasText(userId) && hasText(conversation.getCreator()) && !userId.equals(conversation.getCreator())) {
            throw new BizException(CoreErrorCode.CONVERSATION_NOT_FOUND);
        }
        return conversation;
    }

    /**
     * 解析智能体 ID。
     */
    private String resolveAgentId(ChatCompletionRequest request) {
        if (request == null || !hasText(request.getAgentId())) {
            throw new BizException(CoreErrorCode.INVALID_CHAT_REQUEST);
        }
        return request.getAgentId();
    }

    /**
     * 获取最后一条用户消息内容。
     */
    private String resolveLatestUserMessage(ChatCompletionRequest request) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new BizException(CoreErrorCode.INVALID_CHAT_REQUEST);
        }
        return request.getMessages().stream()
                .filter(message -> ChatRoleEnum.USER.code().equalsIgnoreCase(message.getRole()))
                .reduce((first, second) -> second)
                .map(ChatCompletionMessage::getContent)
                .filter(this::hasText)
                .orElseThrow(() -> new BizException(CoreErrorCode.INVALID_CHAT_REQUEST));
    }

    /**
     * 判断文本是否有值。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
