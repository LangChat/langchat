package cn.langchat.core.chat.model.protocol;

import java.util.List;
import lombok.Data;

/**
 * OpenAI 风格流式聊天分片。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class OpenAiChatCompletionChunk {

    /** Completion ID。 */
    private String id;

    /** 对象类型。 */
    private String object;

    /** 创建时间。 */
    private Long created;

    /** 模型名称。 */
    private String model;

    /** 选项列表。 */
    private List<OpenAiChatCompletionChoice> choices;
}
