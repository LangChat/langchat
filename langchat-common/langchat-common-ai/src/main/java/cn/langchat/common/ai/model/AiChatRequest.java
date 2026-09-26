package cn.langchat.common.ai.model;

import java.util.Map;

/**
 * AI 聊天请求对象。
 *
 * @param modelId 模型ID
 * @param userPrompt 用户输入
 * @param systemPrompt 系统提示词
 * @param stream 是否流式输出
 * @param options 附加配置
 * @author LangChat Team
 * @since 2026/3/24
 */
public record AiChatRequest(
        String modelId,
        String userPrompt,
        String systemPrompt,
        boolean stream,
        Map<String, Object> options
) {
}
