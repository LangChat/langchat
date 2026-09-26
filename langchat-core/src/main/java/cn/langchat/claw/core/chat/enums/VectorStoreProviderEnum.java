package cn.langchat.claw.core.chat.enums;

import java.util.Locale;

/**
 * 向量库供应商枚举。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public enum VectorStoreProviderEnum {

    /** PgVector。 */
    PGVECTOR("PGVECTOR"),

    /** Milvus。 */
    MILVUS("MILVUS");

    private final String code;

    VectorStoreProviderEnum(String code) {
        this.code = code;
    }

    /**
     * 获取供应商编码。
     */
    public String code() {
        return code;
    }

    /**
     * 按数据库值解析供应商。
     */
    public static VectorStoreProviderEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return PGVECTOR;
        }
        String normalized = code.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PGVECTOR", "PGVETOR" -> PGVECTOR;
            case "MILVUS", "MINVUS" -> MILVUS;
            default -> PGVECTOR;
        };
    }
}
