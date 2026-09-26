package cn.langchat.auth.model.response;

import lombok.Data;

/**
 * 动态路由元数据响应对象。
 *
 * @author LangChat Team
 * @since 2026/3/27
 */
@Data
public class AuthRouteMetaResponse {

    /** 当前激活菜单路径。 */
    private String activePath;

    /** 图标。 */
    private String icon;

    /** 是否隐藏菜单。 */
    private Boolean hideInMenu;

    /** 是否开启缓存。 */
    private Boolean keepAlive;

    /** 外链地址。 */
    private String link;

    /** 是否新窗口打开。 */
    private Boolean openInNewWindow;

    /** 排序号。 */
    private Integer order;

    /** 菜单标题。 */
    private String title;
}
