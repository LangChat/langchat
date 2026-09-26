package cn.langchat.auth.entity;

import cn.langchat.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色菜单关联实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_role_menu")
@EqualsAndHashCode(callSuper = true)
public class AigcRoleMenu extends BaseDO {

    /** 角色 ID。 */
    private String roleId;
    /** 菜单 ID。 */
    private String menuId;
}
