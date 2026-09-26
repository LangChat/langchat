package cn.langchat.auth.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.langchat.auth.model.request.AuthLoginRequest;
import cn.langchat.auth.model.response.AuthCurrentUserResponse;
import cn.langchat.auth.model.response.AuthLoginResponse;
import cn.langchat.auth.model.response.AuthRouteRecordResponse;
import cn.langchat.auth.service.AigcMenuService;
import cn.langchat.auth.service.AuthService;
import cn.langchat.common.auth.AuthUtil;
import cn.langchat.common.core.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final AigcMenuService aigcMenuService;

    @PostMapping("/login")
    public ApiResponse<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    @SaCheckLogin
    public ApiResponse<AuthCurrentUserResponse> me() {
        return ApiResponse.success(authService.currentUser());
    }

    /**
     * 查询当前登录用户的动态路由。
     */
    @GetMapping("/access-routes")
    @SaCheckLogin
    public ApiResponse<List<AuthRouteRecordResponse>> accessRoutes() {
        String userId = AuthUtil.getUserId();
        return ApiResponse.success(aigcMenuService.listCurrentUserRoutes(userId));
    }
}
