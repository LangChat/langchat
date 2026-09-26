package cn.langchat.claw.aigc.biz.entity;

import cn.langchat.claw.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对话消息实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_message")
@EqualsAndHashCode(callSuper = true)
public class AigcMessage extends BaseDO {

    /** 消息主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 会话ID。 */
    private String conversationId;

    /** 消息链路ID。 */
    private String chatId;

    /** 关联 Agent ID。 */
    private String agentId;

    /** 消息角色。 */
    private String role;

    /** 使用模型。 */
    private String model;

    /** 消息内容。 */
    private String message;

    /** 消息类型。 */
    private String type;

    /** 完成原因。 */
    private String finishReason;

    /** 跟踪信息。 */
    private String traceInfo;

    /** 附件信息。 */
    private String attachments;

    /** 输入Token数。 */
    private Integer inputToken;

    /** 输出Token数。 */
    private Integer outputToken;

    /** 是否点赞。 */
    private Boolean likes;

    /** 执行耗时毫秒。 */
    private Integer duration;
}
