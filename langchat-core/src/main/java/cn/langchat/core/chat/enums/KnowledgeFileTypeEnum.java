package cn.langchat.core.chat.enums;

import java.util.Locale;

/**
 * 知识文件类型枚举。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public enum KnowledgeFileTypeEnum {

    /** PDF。 */
    PDF,

    /** Word。 */
    WORD,

    /** Excel。 */
    EXCEL,

    /** PPT。 */
    PPT,

    /** Markdown。 */
    MARKDOWN,

    /** 文本。 */
    TEXT,

    /** 未知。 */
    UNKNOWN;

    /**
     * 按扩展名解析文件类型。
     */
    public static KnowledgeFileTypeEnum fromExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return UNKNOWN;
        }
        String normalized = extension.trim().replace(".", "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "pdf" -> PDF;
            case "doc", "docx" -> WORD;
            case "xls", "xlsx", "csv" -> EXCEL;
            case "ppt", "pptx" -> PPT;
            case "md", "markdown" -> MARKDOWN;
            case "txt", "text" -> TEXT;
            default -> UNKNOWN;
        };
    }
}
