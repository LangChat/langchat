package cn.langchat.auth.model.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 动态路由响应对象。
 *
 * @author LangChat Team
 * @since 2026/3/27
 */
@Data
public class AuthRouteRecordResponse {

    /** 子路由列表。 */
    private List<AuthRouteRecordResponse> children = new ArrayList<>();

    /** 组件标识。 */
    private String component;

    /** 路由元数据。 */
    private AuthRouteMetaResponse meta;

    /** 路由名称。 */
    private String name;

    /** 路由路径。 */
    private String path;

    /** 重定向地址。 */
    private String redirect;
}
