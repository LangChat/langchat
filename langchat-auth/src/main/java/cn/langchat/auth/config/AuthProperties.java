package cn.langchat.auth.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 鉴权配置属性。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@ConfigurationProperties(prefix = "langchat.auth")
public class AuthProperties {

    /**
     * 超级管理员账号（username）。
     *
     * 命中后默认拥有全量菜单与权限，不受 RBAC 拦截限制。
     */
    private String superAdminUsername;

    /**
     * 本地兜底账户列表。
     */
    private List<LocalUserProperties> localUsers = new ArrayList<>();

    @Data
    public static class LocalUserProperties {

        /** 用户 ID。 */
        private String userId;
        /** 用户名。 */
        private String username;
        /** 密码。 */
        private String password;
        /** 显示名称。 */
        private String displayName;
        /** 租户 ID。 */
        private String tenantId;
        /** 角色列表。 */
        private List<String> roles = new ArrayList<>();
        /** 权限列表。 */
        private List<String> permissions = new ArrayList<>();
    }
}
