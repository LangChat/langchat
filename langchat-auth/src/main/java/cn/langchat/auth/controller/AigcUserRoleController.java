package cn.langchat.auth.controller;

import cn.langchat.auth.entity.AigcUserRole;
import cn.langchat.auth.service.AigcUserRoleService;
import cn.langchat.common.core.ApiResponse;
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
 * 用户角色关联控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/auth/user-roles")
@RequiredArgsConstructor
@Slf4j
public class AigcUserRoleController {

    private final AigcUserRoleService aigcUserRoleService;

    @GetMapping
    public ApiResponse<List<AigcUserRole>> list() {
        return ApiResponse.success(aigcUserRoleService.list());
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcUserRole userRole) {
        log.info("新增用户角色关联，userId={}, roleId={}", userRole.getUserId(), userRole.getRoleId());
        return ApiResponse.success(aigcUserRoleService.save(userRole));
    }

    @DeleteMapping
    public ApiResponse<Boolean> remove(@RequestBody AigcUserRole userRole) {
        log.info("删除用户角色关联，userId={}, roleId={}", userRole.getUserId(), userRole.getRoleId());
        return ApiResponse.success(aigcUserRoleService.lambdaUpdate()
                .eq(AigcUserRole::getUserId, userRole.getUserId())
                .eq(AigcUserRole::getRoleId, userRole.getRoleId())
                .remove());
    }
}
