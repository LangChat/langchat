package cn.langchat.claw.common.core;

/**
 * 错误码协议定义。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface ErrorCode {

    /**
     * 获取错误编码。
     */
    String code();

    /**
     * 获取错误消息。
     */
    String message();
}
