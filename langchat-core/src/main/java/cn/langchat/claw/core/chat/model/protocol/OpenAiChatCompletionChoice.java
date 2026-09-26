package cn.langchat.claw.core.chat.model.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * OpenAI 风格选项对象。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class OpenAiChatCompletionChoice {

    /** 选项索引。 */
    private Integer index;

    /** 增量消息。 */
    private OpenAiChatCompletionDelta delta;

    /** 完成原因。 */
    @JsonProperty("finish_reason")
    private String finishReason;
}
