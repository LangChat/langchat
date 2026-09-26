package cn.langchat.core.runtime;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import java.util.List;

/**
 * Agent 对话 AI Service 定义。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface AgentChatAiService {

    /**
     * 发起流式对话。
     */
    @SystemMessage("{{systemPrompt}}")
    TokenStream chat(
            @MemoryId String conversationId,
            @V("systemPrompt") String systemPrompt,
            @UserMessage List<Content> contents
    );
}
