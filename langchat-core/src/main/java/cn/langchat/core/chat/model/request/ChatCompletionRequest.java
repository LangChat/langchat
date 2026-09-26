package cn.langchat.core.chat.model.request;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 统一聊天补全请求对象。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class ChatCompletionRequest {

    /** 智能体 ID。 */
    private String agentId;

    /** 会话 ID。 */
    private String conversationId;

    /** 已上传的附件元数据。 */
    private List<ChatAttachment> attachments;

    /** 消息列表。 */
    private List<ChatCompletionMessage> messages;

    /** 指定模型名称。 */
    private String model;

    /** 是否流式返回。 */
    private Boolean stream;

    /** 温度参数。 */
    private Double temperature;

    /** TopP 参数。 */
    private Double topP;

    /** Prompt 变量。 */
    private Map<String, Object> variables;
}
