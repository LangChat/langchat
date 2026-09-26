package cn.langchat.claw.auth.controller;

import cn.langchat.claw.auth.entity.AigcRole;
import cn.langchat.claw.auth.service.AigcRoleService;
import cn.langchat.claw.common.core.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/auth/roles")
@RequiredArgsConstructor
@Slf4j
public class AigcRoleController {

    private final AigcRoleService aigcRoleService;

    @GetMapping
    public ApiResponse<List<AigcRole>> list() {
        return ApiResponse.success(aigcRoleService.lambdaQuery().orderByDesc(AigcRole::getCreateTime).list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcRole> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcRoleService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcRole role) {
        log.info("新增角色，code={}", role.getCode());
        return ApiResponse.success(aigcRoleService.save(role));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcRole role) {
        role.setId(id);
        log.info("更新角色，id={}", id);
        return ApiResponse.success(aigcRoleService.updateById(role));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除角色，id={}", id);
        return ApiResponse.success(aigcRoleService.removeById(id));
    }
}
