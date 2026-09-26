package cn.langchat.core.chat.model.protocol;

import java.util.Map;
import lombok.Data;

/**
 * OpenAI 风格增量消息对象。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class OpenAiChatCompletionDelta {

    /** 消息角色。 */
    private String role;

    /** 增量内容。 */
    private String content;

    /** 扩展事件对象（tools/rag/log/error 等）。 */
    private Map<String, Object> event;
}
