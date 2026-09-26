package cn.langchat.claw.auth.service;

import cn.langchat.claw.auth.domain.model.AuthUserProfile;
import cn.langchat.claw.auth.model.request.AuthLoginRequest;
import cn.langchat.claw.auth.model.response.AuthCurrentUserResponse;
import cn.langchat.claw.auth.model.response.AuthLoginResponse;
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
