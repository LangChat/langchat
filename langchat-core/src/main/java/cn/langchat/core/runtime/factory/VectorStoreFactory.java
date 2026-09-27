package cn.langchat.core.runtime.factory;

import cn.langchat.aigc.biz.entity.AigcVectorStore;
import cn.langchat.aigc.biz.support.VectorStoreSupport;
import cn.langchat.core.chat.enums.VectorStoreProviderEnum;
import cn.langchat.common.exception.BizException;
import cn.langchat.core.support.CoreErrorCode;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 向量库工厂。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Component
@Slf4j
public class VectorStoreFactory {

    private final ConcurrentMap<String, EmbeddingStore<TextSegment>> vectorStoreCache = new ConcurrentHashMap<>();

    /**
     * 获取向量库实例。
     */
    public EmbeddingStore<TextSegment> getStore(AigcVectorStore vectorStore) {
        String cacheKey = vectorStore.getId() + ":" + vectorStore.getUpdateTime();
        return vectorStoreCache.computeIfAbsent(cacheKey, key -> createStore(vectorStore));
    }

    private EmbeddingStore<TextSegment> createStore(AigcVectorStore vectorStore) {
        VectorStoreProviderEnum provider = VectorStoreProviderEnum.fromCode(vectorStore.getProvider());
        log.info("构建向量库实例，vectorStoreId={}, provider={}", vectorStore.getId(), provider);
        requireStoreConfig(vectorStore);
        return switch (provider) {
            case PGVECTOR -> PgVectorEmbeddingStore.builder()
                    .host(vectorStore.getHost())
                    .port(vectorStore.getPort())
                    .user(vectorStore.getUsername())
                    .password(vectorStore.getPassword())
                    .database(vectorStore.getDatabaseName())
                    .table(VectorStoreSupport.resolveTableName(vectorStore))
                    .dimension(vectorStore.getDimension())
                    // 不创建 IVFFlat 索引：pgvector 建索引时要按至少 10000 个采样向量分配内存
                    // （1024 维约 40MB），受限于库服务器的 maintenance_work_mem；
                    // 且建库时表通常还是空的，空表上建出的索引检索质量反而差。
                    // 需要索引时等数据灌完再手工建。
                    .useIndex(Boolean.FALSE)
                    // 目标表通常还没有创建，CREATE TABLE IF NOT EXISTS 本身是幂等的
                    .createTable(Boolean.TRUE)
                    .build();
            case MILVUS -> MilvusEmbeddingStore.builder()
                    .host(vectorStore.getHost())
                    .port(vectorStore.getPort())
                    .username(vectorStore.getUsername())
                    .password(vectorStore.getPassword())
                    .databaseName(vectorStore.getDatabaseName())
                    .collectionName(VectorStoreSupport.resolveTableName(vectorStore))
                    .dimension(vectorStore.getDimension())
                    .indexType(IndexType.IVF_FLAT)
                    .metricType(MetricType.COSINE)
                    .autoFlushOnInsert(Boolean.TRUE)
                    .build();
            default -> throw new BizException(CoreErrorCode.UNSUPPORTED_VECTOR_STORE);
        };
    }

    /**
     * 构建前校验向量库配置，把缺失配置转成可定位的中文错误，
     * 避免底层 SDK 抛出难以理解的 "table cannot be null or blank"。
     */
    private void requireStoreConfig(AigcVectorStore vectorStore) {
        String vectorStoreName = vectorStore.getName() == null || vectorStore.getName().isBlank()
                ? vectorStore.getId()
                : vectorStore.getName();
        if (VectorStoreSupport.requiresTableName(vectorStore)) {
            throw new BizException(CoreErrorCode.VECTOR_STORE_NOT_FOUND.code(),
                    "向量库「" + vectorStoreName + "」未配置表名/集合名，请在向量库配置中补充");
        }
        if (vectorStore.getDimension() == null || vectorStore.getDimension() <= 0) {
            throw new BizException(CoreErrorCode.VECTOR_STORE_NOT_FOUND.code(),
                    "向量库「" + vectorStoreName + "」未配置向量维度，请在向量库配置中补充");
        }
    }
}
