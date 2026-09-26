package cn.langchat.claw.auth.entity;

import cn.langchat.claw.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_menu")
@EqualsAndHashCode(callSuper = true)
public class AigcMenu extends BaseDO {

    /** 菜单主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** 菜单名称。 */
    private String name;
    /** 父级菜单 ID。 */
    private String parentId;
    /** 路由路径。 */
    private String path;
    /** 权限标识。 */
    private String perms;
    /** 菜单类型。 */
    private String type;
    /** 排序号。 */
    private Integer orderNo;
    /** 图标。 */
    private String icon;
    /** 前端组件路径。 */
    private String component;
    /** 是否禁用。 */
    private Boolean isDisabled;
    /** 是否外链。 */
    private Boolean isExt;
    /** 是否缓存。 */
    private Boolean isKeepalive;
    /** 是否显示。 */
    private Boolean isShow;
}
