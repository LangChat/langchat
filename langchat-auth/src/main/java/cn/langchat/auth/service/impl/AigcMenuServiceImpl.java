package cn.langchat.auth.service.impl;

import cn.langchat.auth.config.AuthProperties;
import cn.langchat.auth.entity.AigcMenu;
import cn.langchat.auth.entity.AigcRoleMenu;
import cn.langchat.auth.entity.AigcUser;
import cn.langchat.auth.entity.AigcUserRole;
import cn.langchat.auth.infrastructure.LocalUserAccountRepository;
import cn.langchat.auth.mapper.AigcMenuMapper;
import cn.langchat.auth.model.response.AigcMenuTreeNode;
import cn.langchat.auth.model.response.AuthRouteMetaResponse;
import cn.langchat.auth.model.response.AuthRouteRecordResponse;
import cn.langchat.auth.service.AigcMenuService;
import cn.langchat.auth.service.AigcRoleMenuService;
import cn.langchat.auth.service.AigcUserRoleService;
import cn.langchat.auth.service.AigcUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单 Service 实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AigcMenuServiceImpl extends ServiceImpl<AigcMenuMapper, AigcMenu> implements AigcMenuService {

    private static final String COMPONENT_BASIC_LAYOUT = "BasicLayout";
    private static final String MENU_TYPE_BUTTON = "BUTTON";
    private static final String MENU_TYPE_CATALOG = "CATALOG";
    private static final String LEGACY_LANGCHAT_PREFIX = "/langchat";
    private static final String LEGACY_AUTH_PREFIX = "/langchat/auth";
    private static final String PERMISSION_PREFIX = "/permissions";

    private final AigcUserRoleService aigcUserRoleService;
    private final AigcRoleMenuService aigcRoleMenuService;
    private final AigcUserService aigcUserService;
    private final LocalUserAccountRepository accountRepository;
    private final AuthProperties authProperties;

    @Override
    public List<AigcMenuTreeNode> listTree(String keyword, String type) {
        List<AigcMenu> allMenus = lambdaQuery()
                .orderByAsc(AigcMenu::getOrderNo)
                .orderByAsc(AigcMenu::getCreateTime)
                .list();
        log.info("查询菜单树，keyword={}, type={}, menuSize={}", keyword, type, allMenus.size());
        List<AigcMenu> filteredMenus = filterMenus(allMenus, keyword, type);
        return buildTree(filteredMenus);
    }

    @Override
    public List<AuthRouteRecordResponse> listCurrentUserRoutes(String userId) {
        List<AigcMenu> allMenus = lambdaQuery()
                .orderByAsc(AigcMenu::getOrderNo)
                .orderByAsc(AigcMenu::getCreateTime)
                .list();
        if (isSuperAdmin(userId)) {
            List<AigcMenu> accessibleMenus = allMenus.stream()
                    .filter(this::isRouteMenu)
                    .toList();
            return buildRouteTree(accessibleMenus);
        }
        List<String> roleIds = aigcUserRoleService.lambdaQuery()
                .eq(AigcUserRole::getUserId, userId)
                .list()
                .stream()
                .map(AigcUserRole::getRoleId)
                .filter(roleId -> !isBlank(roleId))
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            log.info("当前用户未绑定角色，返回空路由，userId={}", userId);
            return List.of();
        }
        Set<String> routeMenuIds = aigcRoleMenuService.lambdaQuery()
                .in(AigcRoleMenu::getRoleId, roleIds)
                .list()
                .stream()
                .map(AigcRoleMenu::getMenuId)
                .filter(menuId -> !isBlank(menuId))
                .collect(Collectors.toSet());
        if (routeMenuIds.isEmpty()) {
            log.info("当前用户未绑定菜单，返回空路由，userId={}", userId);
            return List.of();
        }
        List<AigcMenu> accessibleMenus = includeAncestors(allMenus, routeMenuIds).stream()
                .filter(this::isRouteMenu)
                .filter(menu -> !Boolean.TRUE.equals(menu.getIsDisabled()))
                .toList();
        return buildRouteTree(accessibleMenus);
    }

    /**
     * 过滤菜单并补齐父节点。
     */
    private List<AigcMenu> filterMenus(List<AigcMenu> allMenus, String keyword, String type) {
        if (isBlank(keyword) && isBlank(type)) {
            return allMenus;
        }
        Map<String, AigcMenu> menuMap = allMenus.stream()
                .filter(menu -> !isBlank(menu.getId()))
                .collect(Collectors.toMap(AigcMenu::getId, menu -> menu, (left, right) -> left, LinkedHashMap::new));
        Set<String> matchedIds = allMenus.stream()
                .filter(menu -> matches(menu, keyword, type))
                .map(AigcMenu::getId)
                .filter(id -> !isBlank(id))
                .collect(Collectors.toSet());
        Set<String> treeIds = matchedIds.stream().collect(Collectors.toSet());
        for (String matchedId : matchedIds) {
            AigcMenu current = menuMap.get(matchedId);
            while (current != null && !isBlank(current.getParentId())) {
                treeIds.add(current.getParentId());
                current = menuMap.get(current.getParentId());
            }
        }
        return allMenus.stream()
                .filter(menu -> !isBlank(menu.getId()) && treeIds.contains(menu.getId()))
                .toList();
    }

    /**
     * 补齐父级菜单。
     */
    private List<AigcMenu> includeAncestors(List<AigcMenu> allMenus, Set<String> menuIds) {
        Map<String, AigcMenu> menuMap = allMenus.stream()
                .filter(menu -> !isBlank(menu.getId()))
                .collect(Collectors.toMap(AigcMenu::getId, menu -> menu, (left, right) -> left, LinkedHashMap::new));
        Set<String> treeIds = menuIds.stream().collect(Collectors.toSet());
        for (String menuId : menuIds) {
            AigcMenu current = menuMap.get(menuId);
            while (current != null && !isBlank(current.getParentId())) {
                treeIds.add(current.getParentId());
                current = menuMap.get(current.getParentId());
            }
        }
        return allMenus.stream()
                .filter(menu -> !isBlank(menu.getId()) && treeIds.contains(menu.getId()))
                .toList();
    }

    /**
     * 构建树结构。
     */
    private List<AigcMenuTreeNode> buildTree(List<AigcMenu> menus) {
        Map<String, AigcMenuTreeNode> nodeMap = new LinkedHashMap<>();
        for (AigcMenu menu : menus) {
            nodeMap.put(menu.getId(), toNode(menu));
        }
        List<AigcMenuTreeNode> roots = new ArrayList<>();
        for (AigcMenu menu : menus) {
            AigcMenuTreeNode current = nodeMap.get(menu.getId());
            if (current == null) {
                continue;
            }
            if (isBlank(menu.getParentId()) || !nodeMap.containsKey(menu.getParentId())) {
                roots.add(current);
                continue;
            }
            nodeMap.get(menu.getParentId()).getChildren().add(current);
        }
        sortTree(roots);
        return roots;
    }

    /**
     * 构建动态路由树。
     */
    private List<AuthRouteRecordResponse> buildRouteTree(List<AigcMenu> menus) {
        Map<String, AigcMenu> menuMap = menus.stream()
                .filter(menu -> !isBlank(menu.getId()))
                .collect(Collectors.toMap(AigcMenu::getId, menu -> menu, (left, right) -> left, LinkedHashMap::new));
        Map<String, AuthRouteRecordResponse> routeMap = new LinkedHashMap<>();
        for (AigcMenu menu : menus) {
            if (isVirtualRoot(menu, menus)) {
                continue;
            }
            routeMap.put(menu.getId(), toRoute(menu));
        }
        List<AuthRouteRecordResponse> roots = new ArrayList<>();
        for (AigcMenu menu : menus) {
            AuthRouteRecordResponse current = routeMap.get(menu.getId());
            if (current == null) {
                continue;
            }
            AigcMenu parentMenu = menuMap.get(menu.getParentId());
            if (shouldAttachAsChild(parentMenu, routeMap)) {
                routeMap.get(parentMenu.getId()).getChildren().add(current);
                continue;
            }
            if (parentMenu != null && !Boolean.TRUE.equals(menu.getIsShow())) {
                current.getMeta().setActivePath(normalizeRoutePath(parentMenu.getPath()));
            }
            roots.add(current);
        }
        sortRoutes(roots);
        return roots;
    }

    /**
     * 递归排序树节点。
     */
    private void sortTree(List<AigcMenuTreeNode> nodes) {
        nodes.sort(Comparator
                .comparing((AigcMenuTreeNode node) -> node.getOrderNo() == null ? 0 : node.getOrderNo())
                .thenComparing(node -> node.getName() == null ? "" : node.getName()));
        for (AigcMenuTreeNode node : nodes) {
            if (!node.getChildren().isEmpty()) {
                sortTree(node.getChildren());
            }
        }
    }

    /**
     * 递归排序动态路由。
     */
    private void sortRoutes(List<AuthRouteRecordResponse> routes) {
        routes.sort(Comparator
                .comparing((AuthRouteRecordResponse route) -> route.getMeta() == null || route.getMeta().getOrder() == null
                        ? 0
                        : route.getMeta().getOrder())
                .thenComparing(route -> route.getMeta() == null || route.getMeta().getTitle() == null
                        ? ""
                        : route.getMeta().getTitle()));
        for (AuthRouteRecordResponse route : routes) {
            if (!route.getChildren().isEmpty()) {
                sortRoutes(route.getChildren());
            }
        }
    }

    /**
     * 判断菜单是否命中过滤条件。
     */
    private boolean matches(AigcMenu menu, String keyword, String type) {
        boolean matchType = isBlank(type) || type.equals(menu.getType());
        if (!matchType) {
            return false;
        }
        if (isBlank(keyword)) {
            return true;
        }
        String query = keyword.trim().toLowerCase();
        return contains(menu.getName(), query)
                || contains(menu.getPath(), query)
                || contains(menu.getPerms(), query)
                || contains(menu.getComponent(), query);
    }

    /**
     * 判断是否为路由菜单。
     */
    private boolean isRouteMenu(AigcMenu menu) {
        return !MENU_TYPE_BUTTON.equals(menu.getType());
    }

    /**
     * 判断是否应作为子级挂载。
     */
    private boolean shouldAttachAsChild(AigcMenu parentMenu, Map<String, AuthRouteRecordResponse> routeMap) {
        return parentMenu != null
                && routeMap.containsKey(parentMenu.getId())
                && MENU_TYPE_CATALOG.equals(parentMenu.getType());
    }

    /**
     * 判断是否为需要提升子级的旧版虚拟根节点。
     */
    private boolean isVirtualRoot(AigcMenu menu, List<AigcMenu> menus) {
        boolean hasChildren = menus.stream().anyMatch(candidate -> menu.getId() != null && menu.getId().equals(candidate.getParentId()));
        return isBlank(menu.getParentId())
                && MENU_TYPE_CATALOG.equals(menu.getType()) == false
                && COMPONENT_BASIC_LAYOUT.equals(menu.getComponent())
                && hasChildren;
    }

    /**
     * 转换为树节点。
     */
    private AigcMenuTreeNode toNode(AigcMenu menu) {
        AigcMenuTreeNode node = new AigcMenuTreeNode();
        node.setId(menu.getId());
        node.setName(menu.getName());
        node.setParentId(menu.getParentId());
        node.setPath(menu.getPath());
        node.setPerms(menu.getPerms());
        node.setType(menu.getType());
        node.setOrderNo(menu.getOrderNo());
        node.setIcon(menu.getIcon());
        node.setComponent(menu.getComponent());
        node.setIsDisabled(menu.getIsDisabled());
        node.setIsExt(menu.getIsExt());
        node.setIsKeepalive(menu.getIsKeepalive());
        node.setIsShow(menu.getIsShow());
        node.setCreator(menu.getCreator());
        node.setUpdater(menu.getUpdater());
        node.setCreateTime(menu.getCreateTime());
        node.setUpdateTime(menu.getUpdateTime());
        return node;
    }

    /**
     * 转换为动态路由。
     */
    private AuthRouteRecordResponse toRoute(AigcMenu menu) {
        AuthRouteMetaResponse meta = new AuthRouteMetaResponse();
        meta.setHideInMenu(Boolean.FALSE.equals(menu.getIsShow()));
        meta.setIcon(menu.getIcon());
        meta.setKeepAlive(Boolean.TRUE.equals(menu.getIsKeepalive()));
        meta.setLink(Boolean.TRUE.equals(menu.getIsExt()) ? menu.getPath() : null);
        meta.setOpenInNewWindow(Boolean.TRUE.equals(menu.getIsExt()));
        meta.setOrder(menu.getOrderNo());
        meta.setTitle(menu.getName());

        AuthRouteRecordResponse route = new AuthRouteRecordResponse();
        route.setComponent(resolveRouteComponent(menu));
        route.setMeta(meta);
        route.setName(menu.getId());
        route.setPath(normalizeRoutePath(menu.getPath()));
        return route;
    }

    /**
     * 解析动态路由组件。
     */
    private String resolveRouteComponent(AigcMenu menu) {
        if (MENU_TYPE_CATALOG.equals(menu.getType())) {
            return COMPONENT_BASIC_LAYOUT;
        }
        if (!isBlank(menu.getComponent())) {
            return menu.getComponent();
        }
        return COMPONENT_BASIC_LAYOUT;
    }

    /**
     * 统一兼容旧版菜单路径。
     */
    private String normalizeRoutePath(String path) {
        if (isBlank(path)) {
            return path;
        }
        if (path.equals(LEGACY_AUTH_PREFIX)) {
            return PERMISSION_PREFIX;
        }
        if (path.startsWith(LEGACY_AUTH_PREFIX + "/")) {
            return PERMISSION_PREFIX + path.substring(LEGACY_AUTH_PREFIX.length());
        }
        if (path.startsWith(LEGACY_LANGCHAT_PREFIX + "/")) {
            return path.substring(LEGACY_LANGCHAT_PREFIX.length());
        }
        return path;
    }

    /**
     * 判断字符串是否包含关键字。
     */
    private boolean contains(String source, String query) {
        return source != null && source.toLowerCase().contains(query);
    }

    /**
     * 判断文本是否为空。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isSuperAdmin(String userId) {
        String superAdminUsername = authProperties.getSuperAdminUsername();
        if (isBlank(superAdminUsername) || isBlank(userId)) {
            return false;
        }
        AigcUser dbUser = aigcUserService.getById(userId);
        if (dbUser != null && superAdminUsername.equals(dbUser.getUsername())) {
            return true;
        }
        return accountRepository.findByUserId(userId)
                .map(account -> superAdminUsername.equals(account.username()))
                .orElse(false);
    }
}
