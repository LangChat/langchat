package cn.langchat.common.ai.model;

/**
 * AI 聊天响应对象。
 *
 * @param content 响应内容
 * @param finishReason 停止原因
 * @param inputTokens 输入Token
 * @param outputTokens 输出Token
 * @param providerType 厂商类型
 * @author LangChat Team
 * @since 2026/3/24
 */
public record AiChatResponse(
        String content,
        String finishReason,
        Integer inputTokens,
        Integer outputTokens,
        String providerType
) {
}
