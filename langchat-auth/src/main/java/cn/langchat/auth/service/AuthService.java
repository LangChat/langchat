package cn.langchat.auth.service;

import cn.langchat.auth.domain.model.AuthUserProfile;
import cn.langchat.auth.model.request.AuthLoginRequest;
import cn.langchat.auth.model.response.AuthCurrentUserResponse;
import cn.langchat.auth.model.response.AuthLoginResponse;
import java.util.Optional;

/**
 * 认证 Service 接口。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface AuthService {

    AuthLoginResponse login(AuthLoginRequest request);

    void logout();

    AuthCurrentUserResponse currentUser();

    Optional<AuthUserProfile> findByUserId(String userId);
}
