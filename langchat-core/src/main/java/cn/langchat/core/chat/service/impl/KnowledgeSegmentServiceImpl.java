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
import cn.langchat.common.exception.BizException;
import cn.langchat.core.chat.model.request.SegmentContentUpdateRequest;
import cn.langchat.core.chat.model.request.SegmentDeleteRequest;
import cn.langchat.core.chat.model.request.SegmentEnabledRequest;
import cn.langchat.core.chat.service.KnowledgeSegmentService;
import cn.langchat.core.runtime.factory.LangChain4jModelFactory;
import cn.langchat.core.runtime.factory.VectorStoreFactory;
import cn.langchat.core.runtime.rag.SegmentMetadataFactory;
import cn.langchat.core.support.CoreErrorCode;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 知识库分段运行时服务实现。
 *
 * <p>分段检索直接命中向量库，因此分段的启停、编辑与删除都必须同步维护向量数据，
 * 否则数据库与向量库会出现不一致。</p>
 *
 * @author LangChat Team
 * @since 2026/9/27
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeSegmentServiceImpl implements KnowledgeSegmentService {

    private final AigcKnowledgeService aigcKnowledgeService;
    private final AigcDocsService aigcDocsService;
    private final AigcSegmentService aigcSegmentService;
    private final AigcModelService aigcModelService;
    private final AigcVectorStoreService aigcVectorStoreService;
    private final LangChain4jModelFactory langChain4jModelFactory;
    private final VectorStoreFactory vectorStoreFactory;
    private final SegmentMetadataFactory segmentMetadataFactory;

    @Override
    public List<AigcSegment> listSegments(String knowledgeId, String docsId) {
        AigcDocs docs = requireDocs(knowledgeId, docsId);
        log.info("查询文档分段列表，knowledgeId={}, docsId={}", knowledgeId, docs.getId());
        return aigcSegmentService.lambdaQuery()
                .eq(AigcSegment::getDocsId, docs.getId())
                .orderByAsc(AigcSegment::getPosition)
                .list();
    }

    @Override
    public void updateSegmentContent(String knowledgeId, String segmentId, SegmentContentUpdateRequest request) {
        AigcSegment segment = requireSegment(knowledgeId, segmentId);
        String content = request == null ? null : request.getContent();
        if (content == null || content.isBlank()) {
            throw new BizException(CoreErrorCode.INVALID_SEGMENT_REQUEST.code(), "分段内容不能为空");
        }
        aigcSegmentService.lambdaUpdate()
                .eq(AigcSegment::getId, segment.getId())
                .set(AigcSegment::getContent, content)
                .update();
        log.info("更新分段内容，docsId={}, segmentId={}, contentLength={}", segment.getDocsId(), segmentId, content.length());
    }

    @Override
    public void updateSegmentEnabled(String knowledgeId, String segmentId, SegmentEnabledRequest request) {
        AigcSegment segment = requireSegment(knowledgeId, segmentId);
        boolean enabled = request == null || request.getEnabled() == null ? Boolean.TRUE : request.getEnabled();
        SegmentEmbeddingContext context = loadEmbeddingContext(knowledgeId, segment.getDocsId());
        aigcSegmentService.lambdaUpdate()
                .eq(AigcSegment::getId, segment.getId())
                .set(AigcSegment::getEnabled, enabled)
                .update();
        if (enabled) {
            embedSegments(context, List.of(segment));
            log.info("启用分段并写入向量，knowledgeId={}, segmentId={}", knowledgeId, segmentId);
            return;
        }
        context.embeddingStore().removeAll(List.of(segmentId));
        log.info("停用分段并移除向量，knowledgeId={}, segmentId={}", knowledgeId, segmentId);
    }

    @Override
    public void reindexSegment(String knowledgeId, String segmentId) {
        AigcSegment segment = requireSegment(knowledgeId, segmentId);
        if (!Objects.equals(Boolean.TRUE, segment.getEnabled())) {
            throw new BizException(CoreErrorCode.INVALID_SEGMENT_REQUEST.code(), "分段已停用，请先启用后再重新向量化");
        }
        SegmentEmbeddingContext context = loadEmbeddingContext(knowledgeId, segment.getDocsId());
        embedSegments(context, List.of(segment));
        log.info("完成单个分段重新向量化，knowledgeId={}, docsId={}, segmentId={}", knowledgeId, segment.getDocsId(), segmentId);
    }

    @Override
    public void deleteSegment(String knowledgeId, String segmentId) {
        requireSegment(knowledgeId, segmentId);
        SegmentDeleteRequest request = new SegmentDeleteRequest();
        request.setSegmentIds(List.of(segmentId));
        deleteSegments(knowledgeId, request);
    }

    @Override
    public void deleteSegments(String knowledgeId, SegmentDeleteRequest request) {
        List<String> segmentIds = request == null || request.getSegmentIds() == null
                ? List.of()
                : request.getSegmentIds();
        Set<String> uniqueIds = new LinkedHashSet<>(segmentIds);
        uniqueIds.removeIf(id -> id == null || id.isBlank());
        if (uniqueIds.isEmpty()) {
            throw new BizException(CoreErrorCode.INVALID_SEGMENT_REQUEST.code(), "请先选择要删除的分段");
        }
        List<AigcSegment> segments = aigcSegmentService.lambdaQuery()
                .eq(AigcSegment::getKnowledgeId, knowledgeId)
                .in(AigcSegment::getId, uniqueIds)
                .list();
        if (segments.isEmpty()) {
            throw new BizException(CoreErrorCode.SEGMENT_NOT_FOUND.code(), "分段不存在或不属于当前知识库");
        }
        List<String> removedIds = segments.stream().map(AigcSegment::getId).toList();
        // 先清理向量再删除记录：向量库不可用时保留数据库记录，用户可以重试，不会出现库里有数据但检索不到
        SegmentEmbeddingContext context = loadEmbeddingContext(knowledgeId, segments.get(0).getDocsId());
        context.embeddingStore().removeAll(removedIds);
        aigcSegmentService.removeByIds(removedIds);
        log.info("删除分段并清理向量，knowledgeId={}, segmentCount={}", knowledgeId, removedIds.size());
    }

    private AigcDocs requireDocs(String knowledgeId, String docsId) {
        AigcKnowledge knowledge = aigcKnowledgeService.getById(knowledgeId);
        if (knowledge == null) {
            throw new BizException(CoreErrorCode.KNOWLEDGE_NOT_FOUND);
        }
        AigcDocs docs = aigcDocsService.getById(docsId);
        if (docs == null || !Objects.equals(knowledgeId, docs.getKnowledgeId())) {
            throw new BizException(CoreErrorCode.KNOWLEDGE_NOT_FOUND.code(), "文档不存在或不属于当前知识库");
        }
        return docs;
    }

    private AigcSegment requireSegment(String knowledgeId, String segmentId) {
        AigcSegment segment = aigcSegmentService.getById(segmentId);
        if (segment == null || !Objects.equals(knowledgeId, segment.getKnowledgeId())) {
            throw new BizException(CoreErrorCode.SEGMENT_NOT_FOUND.code(), "分段不存在或不属于当前知识库");
        }
        return segment;
    }

    private SegmentEmbeddingContext loadEmbeddingContext(String knowledgeId, String docsId) {
        AigcKnowledge knowledge = aigcKnowledgeService.getById(knowledgeId);
        if (knowledge == null) {
            throw new BizException(CoreErrorCode.KNOWLEDGE_NOT_FOUND);
        }
        AigcDocs docs = aigcDocsService.getById(docsId);
        if (docs == null) {
            throw new BizException(CoreErrorCode.KNOWLEDGE_NOT_FOUND.code(), "文档不存在或已被删除");
        }
        if (knowledge.getVectorStoreId() == null || knowledge.getVectorStoreId().isBlank()) {
            throw new BizException(CoreErrorCode.VECTOR_STORE_NOT_FOUND);
        }
        AigcVectorStore vectorStore = aigcVectorStoreService.getById(knowledge.getVectorStoreId());
        if (vectorStore == null) {
            throw new BizException(CoreErrorCode.VECTOR_STORE_NOT_FOUND);
        }
        if (knowledge.getVectorModelId() == null || knowledge.getVectorModelId().isBlank()) {
            throw new BizException(CoreErrorCode.MODEL_NOT_FOUND.code(), "知识库缺少向量模型配置");
        }
        AigcModel embeddingModelConfig = aigcModelService.getById(knowledge.getVectorModelId());
        if (embeddingModelConfig == null) {
            throw new BizException(CoreErrorCode.MODEL_NOT_FOUND);
        }
        if (!Objects.equals("EMBEDDING", embeddingModelConfig.getType())) {
            throw new BizException(
                    CoreErrorCode.MODEL_NOT_FOUND.code(),
                    "知识库绑定的「" + embeddingModelConfig.getName() + "」不是向量模型，请先在模型管理中创建向量模型并重新绑定"
            );
        }
        return new SegmentEmbeddingContext(
                knowledge,
                docs,
                vectorStoreFactory.getStore(vectorStore),
                langChain4jModelFactory.getEmbeddingModel(embeddingModelConfig)
        );
    }

    private void embedSegments(SegmentEmbeddingContext context, List<AigcSegment> segments) {
        if (segments.isEmpty()) {
            return;
        }
        List<TextSegment> textSegments = new ArrayList<>(segments.size());
        for (AigcSegment segment : segments) {
            textSegments.add(TextSegment.from(segment.getContent(), segmentMetadataFactory.build(context.knowledge(), context.docs(), segment)));
        }
        List<Embedding> embeddings = new ArrayList<>(segments.size());
        for (TextSegment textSegment : textSegments) {
            embeddings.add(context.embeddingModel().embed(textSegment).content());
        }
        List<String> ids = segments.stream().map(AigcSegment::getId).toList();
        context.embeddingStore().removeAll(ids);
        context.embeddingStore().addAll(ids, embeddings, textSegments);
    }

    /**
     * 分段向量化上下文。
     *
     * @param knowledge 知识库
     * @param docs 文档
     * @param embeddingStore 向量库实例
     * @param embeddingModel 向量模型实例
     * @author LangChat Team
     * @since 2026/9/27
     */
    private record SegmentEmbeddingContext(
            AigcKnowledge knowledge,
            AigcDocs docs,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel
    ) {
    }
}
