package cn.langchat.claw.auth.controller;

import cn.langchat.claw.auth.entity.AigcRoleMenu;
import cn.langchat.claw.auth.service.AigcRoleMenuService;
import cn.langchat.claw.common.core.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色菜单关联控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/auth/role-menus")
@RequiredArgsConstructor
@Slf4j
public class AigcRoleMenuController {

    private final AigcRoleMenuService aigcRoleMenuService;

    @GetMapping
    public ApiResponse<List<AigcRoleMenu>> list() {
        return ApiResponse.success(aigcRoleMenuService.list());
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcRoleMenu roleMenu) {
        log.info("新增角色菜单关联，roleId={}, menuId={}", roleMenu.getRoleId(), roleMenu.getMenuId());
        return ApiResponse.success(aigcRoleMenuService.save(roleMenu));
    }

    @DeleteMapping
    public ApiResponse<Boolean> remove(@RequestBody AigcRoleMenu roleMenu) {
        log.info("删除角色菜单关联，roleId={}, menuId={}", roleMenu.getRoleId(), roleMenu.getMenuId());
        return ApiResponse.success(aigcRoleMenuService.lambdaUpdate()
                .eq(AigcRoleMenu::getRoleId, roleMenu.getRoleId())
                .eq(AigcRoleMenu::getMenuId, roleMenu.getMenuId())
                .remove());
    }
}
