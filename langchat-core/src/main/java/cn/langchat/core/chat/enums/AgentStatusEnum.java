package cn.langchat.core.chat.enums;

import java.util.Locale;

/**
 * Agent 状态枚举。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public enum AgentStatusEnum {

    /** 启用。 */
    ENABLED("ENABLED"),

    /** 草稿。 */
    DRAFT("DRAFT"),

    /** 禁用。 */
    DISABLED("DISABLED");

    private final String code;

    AgentStatusEnum(String code) {
        this.code = code;
    }

    /**
     * 获取状态编码。
     */
    public String code() {
        return code;
    }

    /**
     * 判断是否为启用状态。
     */
    public boolean isEnabled() {
        return this == ENABLED;
    }

    /**
     * 按数据库值解析状态。
     */
    public static AgentStatusEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ENABLED;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ENABLED", "PUBLISHED" -> ENABLED;
            case "DISABLED", "ARCHIVED" -> DISABLED;
            default -> DRAFT;
        };
    }
}
