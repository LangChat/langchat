package cn.langchat.claw.auth.controller;

import cn.langchat.claw.auth.entity.AigcUser;
import cn.langchat.claw.auth.service.AigcUserService;
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
 * 用户管理控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/auth/users")
@RequiredArgsConstructor
@Slf4j
public class AigcUserController {

    private final AigcUserService aigcUserService;

    @GetMapping
    public ApiResponse<List<AigcUser>> list() {
        return ApiResponse.success(aigcUserService.lambdaQuery().orderByDesc(AigcUser::getCreateTime).list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AigcUser> detail(@PathVariable("id") String id) {
        return ApiResponse.success(aigcUserService.getById(id));
    }

    @PostMapping
    public ApiResponse<Boolean> create(@RequestBody AigcUser user) {
        log.info("新增用户，username={}", user.getUsername());
        return ApiResponse.success(aigcUserService.save(user));
    }

    @PutMapping("/{id}")
    public ApiResponse<Boolean> update(@PathVariable("id") String id, @RequestBody AigcUser user) {
        user.setId(id);
        log.info("更新用户，id={}", id);
        return ApiResponse.success(aigcUserService.updateById(user));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> remove(@PathVariable("id") String id) {
        log.info("删除用户，id={}", id);
        return ApiResponse.success(aigcUserService.removeById(id));
    }
}
