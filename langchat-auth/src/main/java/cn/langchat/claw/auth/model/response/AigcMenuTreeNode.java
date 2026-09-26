package cn.langchat.claw.auth.model.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 菜单树节点响应对象。
 *
 * @author LangChat Team
 * @since 2026/3/27
 */
@Data
public class AigcMenuTreeNode {

    /** 菜单主键。 */
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

    /** 创建人。 */
    private String creator;

    /** 更新人。 */
    private String updater;

    /** 创建时间。 */
    private Long createTime;

    /** 更新时间。 */
    private Long updateTime;

    /** 子节点列表。 */
    private List<AigcMenuTreeNode> children = new ArrayList<>();
}
