package cn.langchat.aigc.biz.support;

import cn.langchat.aigc.biz.entity.AigcVectorStore;
import java.util.Locale;
import java.util.Set;

/**
 * 向量库配置的通用规则。
 *
 * <p>PGVector 的表名可以由后端兜底，集合型存储（如 Milvus）必须由用户指定集合名。
 * 控制层与运行层都以此为准，避免两边校验不一致。
 *
 * @author LangChat Team
 * @since 2026/9/27
 */
public final class VectorStoreSupport {

    /** PGVector 未指定表名时使用的默认表名。 */
    public static final String DEFAULT_TABLE = "embedding_store";

    private static final Set<String> PGVECTOR_CODES =
            Set.of("PGVECTOR", "PG_VECTOR", "PGVETOR", "");

    private VectorStoreSupport() {}

    /**
     * 是否必须由用户指定表名/集合名。
     */
    public static boolean requiresTableName(AigcVectorStore vectorStore) {
        return !isPgVector(vectorStore.getProvider());
    }

    /**
     * 解析表名/集合名，PGVector 未填写时回退默认表名。
     */
    public static String resolveTableName(AigcVectorStore vectorStore) {
        String tableName = trimToEmpty(vectorStore.getTableName());
        if (!tableName.isEmpty()) {
            return tableName;
        }
        return isPgVector(vectorStore.getProvider()) ? DEFAULT_TABLE : "";
    }

    private static boolean isPgVector(String provider) {
        String normalized = trimToEmpty(provider).replace('-', '_').toUpperCase(Locale.ROOT);
        return PGVECTOR_CODES.contains(normalized);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
