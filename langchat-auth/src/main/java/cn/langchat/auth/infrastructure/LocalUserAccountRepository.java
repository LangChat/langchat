package cn.langchat.auth.infrastructure;

import cn.langchat.auth.config.AuthProperties;
import cn.langchat.auth.domain.account.LocalUserAccount;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * 本地配置账户仓储。
 *
 * 该仓储只作为数据库账户体系未初始化时的临时兜底实现。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Repository
public class LocalUserAccountRepository {

    private final Map<String, LocalUserAccount> userIdIndex;
    private final Map<String, LocalUserAccount> usernameIndex;

    public LocalUserAccountRepository(AuthProperties authProperties) {
        List<LocalUserAccount> accounts = authProperties.getLocalUsers().stream()
                .map(user -> new LocalUserAccount(
                        user.getUserId(),
                        user.getUsername(),
                        user.getPassword(),
                        user.getDisplayName(),
                        user.getTenantId(),
                        List.copyOf(user.getRoles()),
                        List.copyOf(user.getPermissions())
                ))
                .toList();
        this.userIdIndex = accounts.stream().collect(Collectors.toUnmodifiableMap(LocalUserAccount::userId, Function.identity()));
        this.usernameIndex = accounts.stream().collect(Collectors.toUnmodifiableMap(LocalUserAccount::username, Function.identity()));
    }

    public Optional<LocalUserAccount> findByUsername(String username) {
        return Optional.ofNullable(usernameIndex.get(username));
    }

    public Optional<LocalUserAccount> findByUserId(String userId) {
        return Optional.ofNullable(userIdIndex.get(userId));
    }
}
