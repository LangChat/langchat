package cn.langchat.claw.auth.model.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求对象。
 *
 * @param username 用户名
 * @param password 密码
 * @author LangChat Team
 * @since 2026/3/24
 */
public record AuthLoginRequest(
        @NotBlank(message = "username 不能为空") String username,
        @NotBlank(message = "password 不能为空") String password
) {
}
