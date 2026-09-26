package cn.langchat.claw.common.ai.service;

/**
 * AI 模型客户端工厂。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface AiModelClientFactory {

    /**
     * 获取聊天模型客户端。
     */
    ChatModelClient getChatClient(String providerType);

    /**
     * 获取向量模型客户端。
     */
    EmbeddingModelClient getEmbeddingClient(String providerType);
}
