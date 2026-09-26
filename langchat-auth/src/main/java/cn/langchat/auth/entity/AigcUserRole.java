package cn.langchat.auth.entity;

import cn.langchat.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户角色关联实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_user_role")
@EqualsAndHashCode(callSuper = true)
public class AigcUserRole extends BaseDO {

    /** 用户 ID。 */
    private String userId;
    /** 角色 ID。 */
    private String roleId;
}
