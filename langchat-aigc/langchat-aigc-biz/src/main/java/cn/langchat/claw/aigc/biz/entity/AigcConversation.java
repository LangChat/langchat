package cn.langchat.claw.aigc.biz.entity;

import cn.langchat.claw.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对话窗口实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_conversation")
@EqualsAndHashCode(callSuper = true)
public class AigcConversation extends BaseDO {

    /** 会话主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 关联 Agent ID。 */
    private String agentId;

    /** 分享窗口ID。 */
    private String shareId;

    /** 会话标题。 */
    private String title;
}
