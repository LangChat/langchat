package cn.langchat.claw.auth.domain.model;

import java.util.List;

/**
 * 当前认证用户画像。
 *
 * @param userId 用户ID
 * @param username 用户名
 * @param password 密码
 * @param displayName 展示名称
 * @param tenantId 租户ID
 * @param roles 角色编码列表
 * @param permissions 权限标识列表
 * @author LangChat Team
 * @since 2026/3/24
 */
public record AuthUserProfile(
        String userId,
        String username,
        String password,
        String displayName,
        String tenantId,
        List<String> roles,
        List<String> permissions
) {
}
