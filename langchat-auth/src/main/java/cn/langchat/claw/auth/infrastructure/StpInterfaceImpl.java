package cn.langchat.claw.auth.infrastructure;

import cn.langchat.claw.auth.config.AuthProperties;
import cn.langchat.claw.auth.domain.model.AuthUserProfile;
import cn.langchat.claw.auth.entity.AigcMenu;
import cn.langchat.claw.auth.service.AigcMenuService;
import cn.langchat.claw.auth.service.AuthService;
import cn.dev33.satoken.stp.StpInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 角色与权限适配器。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final AuthService authService;
    private final LocalUserAccountRepository accountRepository;
    private final AigcMenuService menuService;
    private final AuthProperties authProperties;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Optional<AuthUserProfile> profile = findByUserId(String.valueOf(loginId));
        if (profile.isPresent() && isSuperAdmin(profile.get().username())) {
            List<String> permissions = new ArrayList<>();
            permissions.add("*");
            permissions.addAll(menuService.lambdaQuery()
                    .orderByAsc(AigcMenu::getPerms)
                    .list()
                    .stream()
                    .map(AigcMenu::getPerms)
                    .filter(perms -> perms != null && !perms.isBlank())
                    .distinct()
                    .toList());
            return permissions;
        }
        return profile
                .map(AuthUserProfile::permissions)
                .orElse(Collections.emptyList());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Optional<AuthUserProfile> profile = findByUserId(String.valueOf(loginId));
        if (profile.isPresent() && isSuperAdmin(profile.get().username())) {
            List<String> roles = new ArrayList<>(profile.get().roles());
            roles.add("SUPER_ADMIN");
            roles.add("*");
            return roles.stream().distinct().toList();
        }
        return profile.map(AuthUserProfile::roles).orElse(Collections.emptyList());
    }

    private Optional<AuthUserProfile> findByUserId(String userId) {
        return authService.findByUserId(userId)
                .or(() -> accountRepository.findByUserId(userId)
                        .map(account -> new AuthUserProfile(
                                account.userId(),
                                account.username(),
                                account.password(),
                                account.displayName(),
                                account.tenantId(),
                                account.roles(),
                                account.permissions()
                        )));
    }

    private boolean isSuperAdmin(String username) {
        String superAdminUsername = authProperties.getSuperAdminUsername();
        return username != null
                && superAdminUsername != null
                && !superAdminUsername.isBlank()
                && superAdminUsername.equals(username);
    }
}
