package cn.langchat.core.chat.service.impl;

import cn.langchat.aigc.biz.entity.AigcDocs;
import cn.langchat.aigc.biz.entity.AigcKnowledge;
import cn.langchat.aigc.biz.entity.AigcModel;
import cn.langchat.aigc.biz.entity.AigcSegment;
import cn.langchat.aigc.biz.entity.AigcVectorStore;
import cn.langchat.aigc.biz.service.AigcDocsService;
import cn.langchat.aigc.biz.service.AigcKnowledgeService;
import cn.langchat.aigc.biz.service.AigcModelService;
import cn.langchat.aigc.biz.service.AigcSegmentService;
import cn.langchat.aigc.biz.service.AigcVectorStoreService;
import cn.langchat.aigc.biz.support.ModelConfigSupport;
import cn.langchat.common.exception.BizException;
import cn.langchat.core.chat.enums.DocumentIndexStatusEnum;
import cn.langchat.core.chat.model.parse.ParsedDocumentContent;
import cn.langchat.core.chat.service.KnowledgeDocumentIndexProcessor;
import cn.langchat.core.runtime.factory.LangChain4jModelFactory;
import cn.langchat.core.runtime.factory.VectorStoreFactory;
import cn.langchat.core.runtime.rag.DocumentChunkingService;
import cn.langchat.core.runtime.rag.DocumentContentParseService;
import cn.langchat.core.runtime.rag.SegmentMetadataFactory;
import cn.langchat.core.support.CoreErrorCode;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 单文档知识索引处理器实现。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeDocumentIndexProcessorImpl implements KnowledgeDocumentIndexProcessor {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final AigcKnowledgeService aigcKnowledgeService;
    private final AigcDocsService aigcDocsService;
    private final AigcSegmentService aigcSegmentService;
    private final AigcVectorStoreService aigcVectorStoreService;
    private final AigcModelService aigcModelService;
    private final DocumentChunkingService documentChunkingService;
    private final DocumentContentParseService documentContentParseService;
    private final SegmentMetadataFactory segmentMetadataFactory;
    private final LangChain4jModelFactory langChain4jModelFactory;
    private final VectorStoreFactory vectorStoreFactory;

    @Override
    public void processDocument(String knowledgeId, String docsId, Integer chunkSize, Integer overlapSize) {
        long startTime = System.currentTimeMillis();
        AigcDocs docs = requireDocs(docsId, knowledgeId);
        log.info("开始异步处理文档向量化任务，knowledgeId={}, docsId={}", knowledgeId, docsId);
        updateDocsStatus(docs, DocumentIndexStatusEnum.RUNNING, null, startTime, null);
        try {
            AigcKnowledge knowledge = requireKnowledge(knowledgeId);
            AigcVectorStore vectorStore = loadVectorStore(knowledge);
            AigcModel embeddingModelConfig = loadEmbeddingModel(knowledge);
            validateEmbeddingDimension(embeddingModelConfig, vectorStore);
            EmbeddingStore<TextSegment> embeddingStore = vectorStoreFactory.getStore(vectorStore);
            EmbeddingModel embeddingModel = langChain4jModelFactory.getEmbeddingModel(embeddingModelConfig);

            aigcSegmentService.lambdaUpdate().eq(AigcSegment::getDocsId, docsId).remove();
            List<AigcSegment> segments = createSegments(docs, chunkSize, overlapSize);
            aigcSegmentService.saveBatch(segments);
            embedSegments(knowledge, docs, segments, embeddingModel, embeddingStore);

            long endTime = System.currentTimeMillis();
            updateDocsStatus(docs, DocumentIndexStatusEnum.COMPLETED, null, startTime, endTime);
            log.info(
                    "文档向量化完成，knowledgeId={}, docsId={}, segmentCount={}, costMs={}",
                    knowledgeId,
                    docsId,
                    segments.size(),
                    endTime - startTime
            );
        } catch (Exception ex) {
            long endTime = System.currentTimeMillis();
            String errorMessage = resolveErrorMessage(ex);
            updateDocsStatus(docs, DocumentIndexStatusEnum.FAILED, errorMessage, startTime, endTime);
            log.error(
                    "文档向量化失败，knowledgeId={}, docsId={}, costMs={}, error={}",
                    knowledgeId,
                    docsId,
                    endTime - startTime,
                    errorMessage,
                    ex
            );
            throw ex;
        }
    }

    private AigcDocs requireDocs(String docsId, String knowledgeId) {
        AigcDocs docs = aigcDocsService.getById(docsId);
        if (docs == null || !Objects.equals(knowledgeId, docs.getKnowledgeId())) {
            throw new BizException(CoreErrorCode.KNOWLEDGE_NOT_FOUND.code(), "文档不存在或不属于当前知识库");
        }
        return docs;
    }

    private AigcKnowledge requireKnowledge(String knowledgeId) {
        AigcKnowledge knowledge = aigcKnowledgeService.getById(knowledgeId);
        if (knowledge == null) {
            throw new BizException(CoreErrorCode.KNOWLEDGE_NOT_FOUND);
        }
        return knowledge;
    }

    private AigcVectorStore loadVectorStore(AigcKnowledge knowledge) {
        if (knowledge.getVectorStoreId() == null || knowledge.getVectorStoreId().isBlank()) {
            throw new BizException(CoreErrorCode.VECTOR_STORE_NOT_FOUND);
        }
        AigcVectorStore vectorStore = aigcVectorStoreService.getById(knowledge.getVectorStoreId());
        if (vectorStore == null) {
            throw new BizException(CoreErrorCode.VECTOR_STORE_NOT_FOUND);
        }
        return vectorStore;
    }

    private AigcModel loadEmbeddingModel(AigcKnowledge knowledge) {
        String modelId = knowledge.getVectorModelId();
        if (modelId == null || modelId.isBlank()) {
            throw new BizException(CoreErrorCode.MODEL_NOT_FOUND.code(), "知识库缺少向量模型配置");
        }
        AigcModel model = aigcModelService.getById(modelId);
        if (model == null) {
            throw new BizException(CoreErrorCode.MODEL_NOT_FOUND);
        }
        return model;
    }

    private void validateEmbeddingDimension(AigcModel model, AigcVectorStore vectorStore) {
        Integer modelDimension = ModelConfigSupport.resolveDimension(model);
        if (modelDimension == null) {
            throw new BizException(
                    CoreErrorCode.INVALID_MODEL_CONFIG.code(),
                    "向量模型 configJson.dimension 必须配置为大于 0 的整数"
            );
        }
        Integer storeDimension = vectorStore.getDimension();
        if (!Objects.equals(modelDimension, storeDimension)) {
            throw new BizException(
                    CoreErrorCode.EMBEDDING_DIMENSION_MISMATCH.code(),
                    "向量模型维度 " + modelDimension + " 与向量库维度 " + storeDimension + " 不一致"
            );
        }
    }

    private List<AigcSegment> createSegments(AigcDocs docs, Integer chunkSize, Integer overlapSize) {
        ParsedDocumentContent parsedDocument = documentContentParseService.parse(docs);
        List<String> chunks = new ArrayList<>();
        for (String section : parsedDocument.getSections()) {
            chunks.addAll(documentChunkingService.chunk(section, chunkSize, overlapSize));
        }
        if (chunks.isEmpty()) {
            throw new BizException(CoreErrorCode.KNOWLEDGE_NOT_FOUND.code(), "文档解析结果为空，无法生成向量切片");
        }
        List<AigcSegment> segments = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            AigcSegment segment = new AigcSegment();
            segment.setId(UUID.randomUUID().toString());
            segment.setDocsId(docs.getId());
            segment.setKnowledgeId(docs.getKnowledgeId());
            segment.setEnabled(Boolean.TRUE);
            segment.setName(docs.getName());
            segment.setPosition(index);
            segment.setContent(chunks.get(index));
            segment.setIndexHash(Integer.toHexString(Objects.hash(docs.getId(), index, chunks.get(index))));
            segment.setStatus(1);
            segments.add(segment);
        }
        return segments;
    }

    private void embedSegments(
            AigcKnowledge knowledge,
            AigcDocs docs,
            List<AigcSegment> segments,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore
    ) {
        List<TextSegment> textSegments = segments.stream()
                .map(segment -> TextSegment.from(segment.getContent(), segmentMetadataFactory.build(knowledge, docs, segment)))
                .toList();
        Response<List<Embedding>> response = embeddingModel.embedAll(textSegments);
        List<Embedding> embeddings = response.content();
        List<String> ids = segments.stream().map(AigcSegment::getId).toList();
        embeddingStore.removeAll(ids);
        embeddingStore.addAll(ids, embeddings, textSegments);
    }

    private void updateDocsStatus(
            AigcDocs docs,
            DocumentIndexStatusEnum status,
            String errorMessage,
            Long startTime,
            Long endTime
    ) {
        long now = System.currentTimeMillis();
        docs.setIndexingStatus(status.dbStatus());
        docs.setEmbedStatus(status.code());
        docs.setEmbedError(errorMessage);
        docs.setEmbedStartTime(startTime);
        docs.setEmbedEndTime(endTime);
        docs.setUpdateTime(now);
        aigcDocsService.lambdaUpdate()
                .eq(AigcDocs::getId, docs.getId())
                .set(AigcDocs::getIndexingStatus, status.dbStatus())
                .set(AigcDocs::getEmbedStatus, status.code())
                .set(AigcDocs::getEmbedError, errorMessage)
                .set(AigcDocs::getEmbedStartTime, startTime)
                .set(AigcDocs::getEmbedEndTime, endTime)
                .set(AigcDocs::getUpdateTime, now)
                .update();
    }

    private String resolveErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "文档向量化失败";
        }
        if (message.length() <= MAX_ERROR_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_LENGTH);
    }
}
