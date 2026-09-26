package cn.langchat.claw.common.exception;

import cn.langchat.claw.common.core.ErrorCode;

/**
 * 业务异常定义。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public class BizException extends RuntimeException {

    private final String code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.message());
        this.code = errorCode.code();
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 获取业务异常编码。
     */
    public String getCode() {
        return code;
    }
}
