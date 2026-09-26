package cn.langchat.claw.aigc.biz.entity;

import cn.langchat.claw.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对话消息事件实体。
 *
 * @author LangChat Team
 * @since 2026/4/6
 */
@Data
@TableName("aigc_message_event")
@EqualsAndHashCode(callSuper = true)
public class AigcMessageEvent extends BaseDO {

    /** 主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 会话ID。 */
    private String conversationId;

    /** 关联消息ID。 */
    private String messageId;

    /** 聊天链路ID。 */
    private String chatId;

    /** Agent ID。 */
    private String agentId;

    /** 事件顺序。 */
    private Integer eventIndex;

    /** 事件名称。 */
    private String eventName;

    /** 事件类型。 */
    private String eventType;

    /** 事件状态。 */
    private String eventStatus;

    /** 原始事件JSON。 */
    private String payloadJson;
}
