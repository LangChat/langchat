package cn.langchat.auth.domain.account;

import java.util.List;

/**
 * 本地配置用户账户。
 *
 * 用于数据库尚未初始化时的最小登录兜底。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public record LocalUserAccount(
        String userId,
        String username,
        String password,
        String displayName,
        String tenantId,
        List<String> roles,
        List<String> permissions
) {
}
