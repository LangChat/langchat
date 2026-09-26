package cn.langchat.claw.common.core;

import cn.langchat.claw.common.web.RequestContext;

/**
 * 通用接口响应体。
 *
 * @param code 响应编码
 * @param message 响应消息
 * @param data 响应数据
 * @param requestId 请求链路ID
 * @author LangChat Team
 * @since 2026/3/24
 */
public record ApiResponse<T>(String code, String message, T data, String requestId) {

    /**
     * 构建成功响应。
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                CommonErrorCode.SUCCESS.code(),
                CommonErrorCode.SUCCESS.message(),
                data,
                RequestContext.getRequestId()
        );
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return failure(errorCode, null);
    }

    /**
     * 构建失败响应。
     */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, T data) {
        return new ApiResponse<>(
                errorCode.code(),
                errorCode.message(),
                data,
                RequestContext.getRequestId()
        );
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(code, message, null, RequestContext.getRequestId());
    }
}
