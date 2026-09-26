package cn.langchat.core.runtime.factory;

import cn.langchat.aigc.biz.entity.AigcVectorStore;
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
        return switch (provider) {
            case PGVECTOR -> PgVectorEmbeddingStore.builder()
                    .host(vectorStore.getHost())
                    .port(vectorStore.getPort())
                    .user(vectorStore.getUsername())
                    .password(vectorStore.getPassword())
                    .database(vectorStore.getDatabaseName())
                    .table(vectorStore.getTableName())
                    .dimension(vectorStore.getDimension())
                    .useIndex(Boolean.TRUE)
                    .createTable(Boolean.FALSE)
                    .build();
            case MILVUS -> MilvusEmbeddingStore.builder()
                    .host(vectorStore.getHost())
                    .port(vectorStore.getPort())
                    .username(vectorStore.getUsername())
                    .password(vectorStore.getPassword())
                    .databaseName(vectorStore.getDatabaseName())
                    .collectionName(vectorStore.getTableName())
                    .dimension(vectorStore.getDimension())
                    .indexType(IndexType.IVF_FLAT)
                    .metricType(MetricType.COSINE)
                    .autoFlushOnInsert(Boolean.TRUE)
                    .build();
            default -> throw new BizException(CoreErrorCode.UNSUPPORTED_VECTOR_STORE);
        };
    }
}
