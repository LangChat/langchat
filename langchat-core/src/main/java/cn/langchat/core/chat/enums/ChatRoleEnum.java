package cn.langchat.core.chat.enums;

import java.util.Locale;

/**
 * 对话角色枚举。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public enum ChatRoleEnum {

    /** 系统角色。 */
    SYSTEM("system"),

    /** 用户角色。 */
    USER("user"),

    /** 助手角色。 */
    ASSISTANT("assistant");

    private final String code;

    ChatRoleEnum(String code) {
        this.code = code;
    }

    /**
     * 获取角色编码。
     */
    public String code() {
        return code;
    }

    /**
     * 按数据库值解析角色。
     */
    public static ChatRoleEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return USER;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "system" -> SYSTEM;
            case "assistant", "ai" -> ASSISTANT;
            default -> USER;
        };
    }
}
