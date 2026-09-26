package cn.langchat.core.chat.model.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * OpenAI 风格完成响应对象。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class OpenAiChatCompletionResult {

    /** Completion ID。 */
    private String id;

    /** 会话 ID。 */
    @JsonProperty("conversation_id")
    private String conversationId;

    /** 消息 ID。 */
    @JsonProperty("message_id")
    private String messageId;

    /** 模型名称。 */
    private String model;

    /** 完成原因。 */
    @JsonProperty("finish_reason")
    private String finishReason;

    /** Token 用量。 */
    private OpenAiChatCompletionUsage usage;
}
