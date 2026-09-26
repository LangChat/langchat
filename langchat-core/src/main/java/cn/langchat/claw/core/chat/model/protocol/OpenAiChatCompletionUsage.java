package cn.langchat.claw.core.chat.model.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * OpenAI 风格 Token 用量对象。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class OpenAiChatCompletionUsage {

    /** 输入 Token 数。 */
    @JsonProperty("prompt_tokens")
    private Integer promptTokens;

    /** 输出 Token 数。 */
    @JsonProperty("completion_tokens")
    private Integer completionTokens;

    /** 总 Token 数。 */
    @JsonProperty("total_tokens")
    private Integer totalTokens;
}
