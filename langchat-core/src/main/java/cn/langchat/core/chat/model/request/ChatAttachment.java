package cn.langchat.core.chat.model.request;

import lombok.Data;

/**
 * 已上传的聊天附件元数据。
 *
 * <p>模型调用时只信任 {@code id}，其余字段用于前端展示和消息记录。</p>
 *
 * @author LangChat Team
 * @since 2026/9/27
 */
@Data
public class ChatAttachment {

    /** OSS 资源 ID。 */
    private String id;

    /** 原始文件名。 */
    private String name;

    /** 文件内容类型。 */
    private String contentType;

    /** 文件大小。 */
    private Long size;

    /** 文件访问地址。 */
    private String url;
}
