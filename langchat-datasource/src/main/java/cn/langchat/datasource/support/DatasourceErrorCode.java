package cn.langchat.datasource.support;

import cn.langchat.common.core.ErrorCode;

/**
 * 数据源模块错误码。
 *
 * @author LangChat Team
 * @since 2026/8/27
 */
public enum DatasourceErrorCode implements ErrorCode {

    /** 数据源不存在。 */
    DATASOURCE_NOT_FOUND("DS_1001", "数据源不存在或不可用"),

    /** 连接失败。 */
    CONNECTION_FAILED("DS_1002", "数据库连接失败"),

    /** 内省失败。 */
    INTROSPECT_FAILED("DS_1003", "表结构内省失败"),

    /** 智能问数只读查询失败。 */
    QUERY_FAILED("DS_1004", "数据查询失败");

    private final String code;
    private final String message;

    DatasourceErrorCode(String code, String message) {
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
