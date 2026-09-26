package cn.langchat.claw.core.chat.model.request;

import lombok.Data;

/**
 * 统一聊天补全消息对象。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class ChatCompletionMessage {

    /** 消息角色。 */
    private String role;

    /** 消息名称。 */
    private String name;

    /** 消息内容。 */
    private String content;
}
