package cn.langchat.core.chat.enums;

import java.util.Locale;

/**
 * 知识文档类型枚举。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public enum KnowledgeDocumentTypeEnum {

    /** QA 对。 */
    QA("QA"),

    /** 纯文本。 */
    TEXT("TEXT"),

    /** Markdown。 */
    MARKDOWN("MARKDOWN"),

    /** 富文档。 */
    FILE("FILE"),

    /** 未知。 */
    UNKNOWN("UNKNOWN");

    private final String code;

    KnowledgeDocumentTypeEnum(String code) {
        this.code = code;
    }

    /**
     * 获取类型编码。
     */
    public String code() {
        return code;
    }

    /**
     * 按数据库值解析类型。
     */
    public static KnowledgeDocumentTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return UNKNOWN;
        }
        String normalized = code.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "QA", "FAQ", "QUESTION_ANSWER" -> QA;
            case "TEXT", "PLAIN_TEXT" -> TEXT;
            case "MARKDOWN", "MD" -> MARKDOWN;
            case "FILE", "DOC", "PDF", "OFFICE" -> FILE;
            default -> UNKNOWN;
        };
    }
}
