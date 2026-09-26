package cn.langchat.claw.core.chat.service;

import cn.langchat.claw.core.chat.model.request.AgentChatStreamRequest;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * Agent 对话运行时服务。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface AgentChatRuntimeService {

    /**
     * 发起流式对话。
     */
    Flux<ServerSentEvent<String>> streamChat(String agentId, AgentChatStreamRequest request);
}
