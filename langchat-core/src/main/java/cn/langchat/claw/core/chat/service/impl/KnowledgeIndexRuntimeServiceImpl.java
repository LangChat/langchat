package cn.langchat.claw.core.chat.service.impl;

import cn.langchat.claw.aigc.biz.entity.AigcDocs;
import cn.langchat.claw.aigc.biz.entity.AigcKnowledge;
import cn.langchat.claw.aigc.biz.service.AigcDocsService;
import cn.langchat.claw.aigc.biz.service.AigcKnowledgeService;
import cn.langchat.claw.common.exception.BizException;
import cn.langchat.claw.core.chat.enums.DocumentIndexStatusEnum;
import cn.langchat.claw.core.chat.model.parse.ParsedDocumentContent;
import cn.langchat.claw.core.chat.model.request.KnowledgeIndexRequest;
import cn.langchat.claw.core.chat.model.request.KnowledgeParsePreviewRequest;
import cn.langchat.claw.core.chat.model.response.KnowledgeDocumentPreview;
import cn.langchat.claw.core.chat.model.response.KnowledgeIndexResult;
import cn.langchat.claw.core.chat.model.response.KnowledgeDocumentIndexStatus;
import cn.langchat.claw.core.chat.model.response.KnowledgeIndexStatusResult;
import cn.langchat.claw.core.chat.model.response.KnowledgeParsePreviewResult;
import cn.langchat.claw.core.chat.service.KnowledgeIndexAsyncService;
import cn.langchat.claw.core.chat.service.KnowledgeIndexRuntimeService;
import cn.langchat.claw.core.runtime.rag.DocumentContentParseService;
import cn.langchat.claw.core.runtime.rag.DocumentChunkingService;
import cn.langchat.claw.core.support.CoreErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 知识库索引运行时服务实现。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIndexRuntimeServiceImpl implements KnowledgeIndexRuntimeService {

    private static final int DEFAULT_PREVIEW_SECTION_LIMIT = 5;
    private static final int DEFAULT_PREVIEW_CHUNK_LIMIT = 10;
    private static final String CONFIG_CHUNK_SIZE = "chunkSize";
    private static final String CONFIG_CHUNK_SIZE_ALT = "chunk_size";
    private static final String CONFIG_OVERLAP_SIZE = "overlapSize";
    private static final String CONFIG_OVERLAP_SIZE_ALT = "overlap_size";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AigcKnowledgeService aigcKnowledgeService;
    private final AigcDocsService aigcDocsService;
    private final DocumentChunkingService documentChunkingService;
    private final DocumentContentParseService documentContentParseService;
    private final KnowledgeIndexAsyncService knowledgeIndexAsyncService;
    private final ObjectMapper objectMapper;

    @Override
    public KnowledgeIndexResult indexKnowledge(String knowledgeId, KnowledgeIndexRequest request) {
        AigcKnowledge knowledge = aigcKnowledgeService.getById(knowledgeId);
        if (knowledge == null) {
            throw new BizException(CoreErrorCode.KNOWLEDGE_NOT_FOUND);
        }
        List<AigcDocs> docsList = loadDocs(knowledgeId, request);
        for (AigcDocs docs : docsList) {
            DocumentSplitConfig splitConfig = resolveSplitConfig(docs, request.getChunkSize(), request.getOverlapSize());
            markDocsQueued(docs);
            knowledgeIndexAsyncService.submitDocumentIndex(
                    knowledgeId,
                    docs.getId(),
                    splitConfig.chunkSize(),
                    splitConfig.overlapSize()
            );
            log.info(
                    "提交知识库文档索引任务，knowledgeId={}, docsId={}, chunkSize={}, overlapSize={}",
                    knowledgeId,
                    docs.getId(),
                    splitConfig.chunkSize(),
                    splitConfig.overlapSize()
            );
        }

        KnowledgeIndexResult result = new KnowledgeIndexResult();
        result.setKnowledgeId(knowledgeId);
        result.setDocsCount(docsList.size());
        result.setSubmittedCount(docsList.size());
        result.setAsync(Boolean.TRUE);
        result.setTaskStatus(DocumentIndexStatusEnum.PENDING.code());
        result.setSegmentCount(0);
        result.setEmbeddedCount(0);
        log.info("知识库索引任务已异步提交，knowledgeId={}, docsCount={}", knowledgeId, docsList.size());
        return result;
    }

    @Override
    public KnowledgeParsePreviewResult previewKnowledge(String knowledgeId, KnowledgeParsePreviewRequest request) {
        AigcKnowledge knowledge = aigcKnowledgeService.getById(knowledgeId);
        if (knowledge == null) {
            throw new BizException(CoreErrorCode.KNOWLEDGE_NOT_FOUND);
        }
        List<AigcDocs> docsList = loadDocs(knowledgeId, buildIndexRequest(request));
        List<KnowledgeDocumentPreview> previews = docsList.stream()
                .map(docs -> buildPreview(docs, request))
                .toList();
        KnowledgeParsePreviewResult result = new KnowledgeParsePreviewResult();
        result.setKnowledgeId(knowledgeId);
        result.setDocsCount(previews.size());
        result.setDocs(previews);
        log.info("知识库解析预览完成，knowledgeId={}, docsCount={}", knowledgeId, previews.size());
        return result;
    }

    @Override
    public KnowledgeIndexStatusResult queryIndexStatus(String knowledgeId, List<String> docsIds) {
        AigcKnowledge knowledge = aigcKnowledgeService.getById(knowledgeId);
        if (knowledge == null) {
            throw new BizException(CoreErrorCode.KNOWLEDGE_NOT_FOUND);
        }
        List<AigcDocs> docsList = aigcDocsService.lambdaQuery()
                .eq(AigcDocs::getKnowledgeId, knowledgeId)
                .in(docsIds != null && !docsIds.isEmpty(), AigcDocs::getId, docsIds)
                .orderByDesc(AigcDocs::getUpdateTime)
                .list();
        long now = System.currentTimeMillis();
        List<KnowledgeDocumentIndexStatus> docStatuses = docsList.stream()
                .map(docs -> buildDocumentIndexStatus(docs, now))
                .toList();

        int pendingCount = countByStatus(docStatuses, DocumentIndexStatusEnum.PENDING.code());
        int runningCount = countByStatus(docStatuses, DocumentIndexStatusEnum.RUNNING.code());
        int completedCount = countByStatus(docStatuses, DocumentIndexStatusEnum.COMPLETED.code());
        int failedCount = countByStatus(docStatuses, DocumentIndexStatusEnum.FAILED.code());

        KnowledgeIndexStatusResult result = new KnowledgeIndexStatusResult();
        result.setKnowledgeId(knowledgeId);
        result.setDocsCount(docStatuses.size());
        result.setPendingCount(pendingCount);
        result.setRunningCount(runningCount);
        result.setCompletedCount(completedCount);
        result.setFailedCount(failedCount);
        result.setFinished(docStatuses.isEmpty() || (pendingCount == 0 && runningCount == 0));
        result.setProgressPercent(calculateProgressPercent(docStatuses.size(), completedCount, failedCount));
        result.setDocs(docStatuses);
        log.info(
                "知识库索引状态查询完成，knowledgeId={}, docsCount={}, pendingCount={}, runningCount={}, completedCount={}, failedCount={}",
                knowledgeId,
                docStatuses.size(),
                pendingCount,
                runningCount,
                completedCount,
                failedCount
        );
        return result;
    }

    private List<AigcDocs> loadDocs(String knowledgeId, KnowledgeIndexRequest request) {
        return aigcDocsService.lambdaQuery()
                .eq(AigcDocs::getKnowledgeId, knowledgeId)
                .and(query -> query.eq(AigcDocs::getEnabled, Boolean.TRUE).or().isNull(AigcDocs::getEnabled))
                .in(request.getDocsIds() != null && !request.getDocsIds().isEmpty(), AigcDocs::getId, request.getDocsIds())
                .orderByDesc(AigcDocs::getUpdateTime)
                .list();
    }

    private KnowledgeDocumentPreview buildPreview(AigcDocs docs, KnowledgeParsePreviewRequest request) {
        ParsedDocumentContent parsedDocument = documentContentParseService.parse(docs);
        DocumentSplitConfig splitConfig = resolveSplitConfig(docs, request.getChunkSize(), request.getOverlapSize());
        List<String> chunks = chunkDocument(parsedDocument, splitConfig.chunkSize(), splitConfig.overlapSize());
        int sectionLimit = sanitizeLimit(request.getSectionLimit(), DEFAULT_PREVIEW_SECTION_LIMIT);
        int chunkLimit = sanitizeLimit(request.getChunkLimit(), DEFAULT_PREVIEW_CHUNK_LIMIT);

        KnowledgeDocumentPreview preview = new KnowledgeDocumentPreview();
        preview.setDocsId(docs.getId());
        preview.setTitle(parsedDocument.getTitle());
        preview.setParserName(parsedDocument.getParserName());
        preview.setSectionCount(parsedDocument.getSections().size());
        preview.setChunkCount(chunks.size());
        preview.setContentLength(parsedDocument.getPlainText() == null ? 0 : parsedDocument.getPlainText().length());
        preview.setSections(limitList(parsedDocument.getSections(), sectionLimit));
        preview.setChunks(limitList(chunks, chunkLimit));
        log.info(
                "完成知识库文档解析预览，docsId={}, parserName={}, sectionCount={}, chunkCount={}, chunkSize={}, overlapSize={}",
                docs.getId(),
                parsedDocument.getParserName(),
                preview.getSectionCount(),
                preview.getChunkCount(),
                splitConfig.chunkSize(),
                splitConfig.overlapSize()
        );
        return preview;
    }

    private List<String> chunkDocument(ParsedDocumentContent parsedDocument, Integer chunkSize, Integer overlapSize) {
        List<String> chunks = new ArrayList<>();
        for (String section : parsedDocument.getSections()) {
            chunks.addAll(documentChunkingService.chunk(section, chunkSize, overlapSize));
        }
        return chunks;
    }

    private KnowledgeIndexRequest buildIndexRequest(KnowledgeParsePreviewRequest request) {
        KnowledgeIndexRequest indexRequest = new KnowledgeIndexRequest();
        indexRequest.setDocsIds(request.getDocsIds());
        indexRequest.setChunkSize(request.getChunkSize());
        indexRequest.setOverlapSize(request.getOverlapSize());
        return indexRequest;
    }

    private DocumentSplitConfig resolveSplitConfig(AigcDocs docs, Integer requestChunkSize, Integer requestOverlapSize) {
        Integer chunkSize = requestChunkSize;
        Integer overlapSize = requestOverlapSize;
        if (chunkSize != null && overlapSize != null) {
            return new DocumentSplitConfig(chunkSize, overlapSize);
        }
        Map<String, Object> ingestionConfig = parseIngestionConfig(docs);
        if (chunkSize == null) {
            chunkSize = integerValue(
                    ingestionConfig.get(CONFIG_CHUNK_SIZE),
                    ingestionConfig.get(CONFIG_CHUNK_SIZE_ALT)
            );
        }
        if (overlapSize == null) {
            overlapSize = integerValue(
                    ingestionConfig.get(CONFIG_OVERLAP_SIZE),
                    ingestionConfig.get(CONFIG_OVERLAP_SIZE_ALT)
            );
        }
        return new DocumentSplitConfig(chunkSize, overlapSize);
    }

    private Map<String, Object> parseIngestionConfig(AigcDocs docs) {
        if (docs.getIngestionConfig() == null || docs.getIngestionConfig().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(docs.getIngestionConfig(), MAP_TYPE);
        } catch (Exception ex) {
            log.warn("解析文档 ingestionConfig 失败，docsId={}, ingestionConfig={}", docs.getId(), docs.getIngestionConfig(), ex);
            return Map.of();
        }
    }

    private Integer integerValue(Object... values) {
        for (Object value : values) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String stringValue && !stringValue.isBlank()) {
                try {
                    return Integer.parseInt(stringValue.trim());
                } catch (NumberFormatException ignored) {
                    log.debug("忽略无法转换的切片配置值，value={}", stringValue);
                }
            }
        }
        return null;
    }

    private int sanitizeLimit(Integer limit, int defaultValue) {
        if (limit == null || limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, 50);
    }

    private List<String> limitList(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().limit(limit).toList();
    }

    private void markDocsQueued(AigcDocs docs) {
        docs.setIndexingStatus(DocumentIndexStatusEnum.PENDING.dbStatus());
        docs.setEmbedStatus(DocumentIndexStatusEnum.PENDING.code());
        docs.setEmbedError(null);
        docs.setEmbedStartTime(null);
        docs.setEmbedEndTime(null);
        aigcDocsService.updateById(docs);
    }

    private KnowledgeDocumentIndexStatus buildDocumentIndexStatus(AigcDocs docs, long now) {
        KnowledgeDocumentIndexStatus status = new KnowledgeDocumentIndexStatus();
        status.setDocsId(docs.getId());
        status.setName(docs.getName());
        status.setExt(docs.getExt());
        status.setIndexingStatus(docs.getIndexingStatus());
        status.setEmbedStatus(resolveEmbedStatus(docs));
        status.setEmbedError(docs.getEmbedError());
        status.setEmbedStartTime(docs.getEmbedStartTime());
        status.setEmbedEndTime(docs.getEmbedEndTime());
        status.setCostMs(calculateCostMs(docs, now));
        status.setUpdateTime(docs.getUpdateTime());
        return status;
    }

    private String resolveEmbedStatus(AigcDocs docs) {
        if (docs.getEmbedStatus() != null && !docs.getEmbedStatus().isBlank()) {
            return docs.getEmbedStatus();
        }
        if (Objects.equals(docs.getIndexingStatus(), DocumentIndexStatusEnum.RUNNING.dbStatus())) {
            return DocumentIndexStatusEnum.RUNNING.code();
        }
        if (Objects.equals(docs.getIndexingStatus(), DocumentIndexStatusEnum.COMPLETED.dbStatus())) {
            return DocumentIndexStatusEnum.COMPLETED.code();
        }
        if (Objects.equals(docs.getIndexingStatus(), DocumentIndexStatusEnum.FAILED.dbStatus())) {
            return DocumentIndexStatusEnum.FAILED.code();
        }
        return DocumentIndexStatusEnum.PENDING.code();
    }

    private Long calculateCostMs(AigcDocs docs, long now) {
        if (docs.getEmbedStartTime() == null) {
            return null;
        }
        long endTime = docs.getEmbedEndTime() == null ? now : docs.getEmbedEndTime();
        return Math.max(0L, endTime - docs.getEmbedStartTime());
    }

    private int countByStatus(List<KnowledgeDocumentIndexStatus> docs, String status) {
        return (int) docs.stream()
                .filter(item -> Objects.equals(status, item.getEmbedStatus()))
                .count();
    }

    private int calculateProgressPercent(int total, int completedCount, int failedCount) {
        if (total <= 0) {
            return 100;
        }
        return Math.min(100, (completedCount + failedCount) * 100 / total);
    }

    /**
     * 文档切片配置。
     *
     * @param chunkSize 切片大小
     * @param overlapSize 重叠大小
     * @author LangChat Team
     * @since 2026/3/25
     */
    private record DocumentSplitConfig(
            Integer chunkSize,
            Integer overlapSize
    ) {
    }
}
