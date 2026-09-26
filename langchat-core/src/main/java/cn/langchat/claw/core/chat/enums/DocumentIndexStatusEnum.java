package cn.langchat.claw.core.chat.enums;

/**
 * 文档索引状态枚举。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public enum DocumentIndexStatusEnum {

    /** 待处理。 */
    PENDING(0, "pending"),

    /** 执行中。 */
    RUNNING(1, "running"),

    /** 已完成。 */
    COMPLETED(2, "completed"),

    /** 已失败。 */
    FAILED(3, "failed");

    private final Integer dbStatus;
    private final String code;

    DocumentIndexStatusEnum(Integer dbStatus, String code) {
        this.dbStatus = dbStatus;
        this.code = code;
    }

    /**
     * 获取数据库状态值。
     */
    public Integer dbStatus() {
        return dbStatus;
    }

    /**
     * 获取状态编码。
     */
    public String code() {
        return code;
    }
}
