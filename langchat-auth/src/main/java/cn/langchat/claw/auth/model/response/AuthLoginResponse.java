package cn.langchat.claw.auth.model.response;

import java.util.List;

/**
 * 登录成功响应对象。
 *
 * @param userId 用户ID
 * @param username 用户名
 * @param displayName 展示名称
 * @param tenantId 租户ID
 * @param tokenName Token名称
 * @param tokenValue Token值
 * @param roles 角色编码列表
 * @param permissions 权限标识列表
 * @author LangChat Team
 * @since 2026/3/24
 */
public record AuthLoginResponse(
        String userId,
        String username,
        String displayName,
        String tenantId,
        String tokenName,
        String tokenValue,
        List<String> roles,
        List<String> permissions
) {
}
