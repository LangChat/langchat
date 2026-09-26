package cn.langchat.claw.common.auth;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 认证上下文工具类。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public final class AuthUtil {

    private AuthUtil() {
    }

    /**
     * 获取当前登录用户ID。
     */
    public static String getUserId() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return loginId == null ? null : String.valueOf(loginId);
    }

    /**
     * 判断当前请求是否已登录。
     */
    public static boolean isLogin() {
        return StpUtil.isLogin();
    }
}
