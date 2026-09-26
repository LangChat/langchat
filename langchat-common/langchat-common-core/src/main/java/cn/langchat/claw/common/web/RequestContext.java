package cn.langchat.claw.common.web;

/**
 * 请求上下文工具。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public final class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();

    private RequestContext() {
    }

    /**
     * 写入当前请求ID。
     */
    public static void setRequestId(String requestId) {
        REQUEST_ID_HOLDER.set(requestId);
    }

    /**
     * 获取当前请求ID。
     */
    public static String getRequestId() {
        return REQUEST_ID_HOLDER.get();
    }

    /**
     * 清理当前线程上下文。
     */
    public static void clear() {
        REQUEST_ID_HOLDER.remove();
    }
}
