package cn.langchat.common.core;

/**
 * 通用错误码定义。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public enum CommonErrorCode implements ErrorCode {

    SUCCESS("SUCCESS", "success"),
    BAD_REQUEST("COMMON_400", "请求参数不合法"),
    UNAUTHORIZED("AUTH_401", "未登录或登录已过期"),
    FORBIDDEN("AUTH_403", "无权限访问"),
    INTERNAL_ERROR("COMMON_500", "系统异常"),
    AUTH_INVALID_CREDENTIALS("AUTH_1001", "用户名或密码错误");

    private final String code;
    private final String message;

    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
