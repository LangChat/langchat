package cn.langchat.auth.service;

import cn.langchat.auth.entity.AigcMenu;
import cn.langchat.auth.model.response.AigcMenuTreeNode;
import cn.langchat.auth.model.response.AuthRouteRecordResponse;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

/**
 * 菜单 Service 接口。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface AigcMenuService extends IService<AigcMenu> {

    /**
     * 按树形结构查询菜单。
     */
    List<AigcMenuTreeNode> listTree(String keyword, String type);

    /**
     * 查询当前用户可访问的动态路由。
     */
    List<AuthRouteRecordResponse> listCurrentUserRoutes(String userId);
}
