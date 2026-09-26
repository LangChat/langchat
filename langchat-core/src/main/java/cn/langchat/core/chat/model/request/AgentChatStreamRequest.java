package cn.langchat.core.chat.model.request;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * Agent 流式对话请求。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
public class AgentChatStreamRequest {

    /** 会话 ID，为空时自动创建新会话。 */
    private String conversationId;

    /** 用户输入消息。 */
    private String message;

    /** 已上传的附件元数据。 */
    private List<ChatAttachment> attachments;

    /** Prompt 变量值。 */
    private Map<String, Object> variables;
}
