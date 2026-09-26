package cn.langchat.claw.core.chat.controller;

import cn.langchat.claw.core.chat.model.request.AgentChatStreamRequest;
import cn.langchat.claw.core.chat.service.AgentChatRuntimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Agent 对话控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/core/agents")
@RequiredArgsConstructor
@Slf4j
public class AgentChatController {

    private final AgentChatRuntimeService agentChatRuntimeService;

    /**
     * 以 SSE 方式流式返回对话结果。
     */
    @PostMapping(value = "/{agentId}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(
            @PathVariable("agentId") String agentId,
            @RequestBody AgentChatStreamRequest request
    ) {
        log.info("接收 Agent 流式对话请求，agentId={}, conversationId={}", agentId, request.getConversationId());
        return agentChatRuntimeService.streamChat(agentId, request);
    }
}
