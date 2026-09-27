package cn.langchat.core.chat.service.impl;

import cn.langchat.aigc.biz.entity.AigcDocs;
import cn.langchat.aigc.biz.service.AigcDocsService;
import cn.langchat.core.chat.enums.DocumentIndexStatusEnum;
import cn.langchat.core.runtime.config.KnowledgeIndexAsyncProperties;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 知识库向量化任务状态守护器。
 *
 * <p>定期收敛因服务中断、线程异常等原因长期停留在等待中/执行中的任务，
 * 同时清理历史完成记录中残留的错误信息。</p>
 *
 * @author LangChat Team
 * @since 2026/9/27
 */
@Component
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class KnowledgeIndexTaskWatchdog {

    private static final String TIMEOUT_ERROR =
            "向量化任务执行超时，可能因服务中断或任务阻塞，已自动标记为失败";

    private final AigcDocsService aigcDocsService;
    private final KnowledgeIndexAsyncProperties properties;

    /**
     * 后台扫描所有尚未结束的任务。
     */
    @Scheduled(fixedDelayString = "#{@knowledgeIndexAsyncProperties.staleTaskSweepInterval.toMillis()}")
    public void expireStaleTasks() {
        List<AigcDocs> activeDocs = aigcDocsService.lambdaQuery()
                .and(status -> status
                        .in(AigcDocs::getEmbedStatus, activeStatusCodes())
                        .or()
                        .and(legacy -> legacy
                                .isNull(AigcDocs::getEmbedStatus)
                                .in(AigcDocs::getIndexingStatus, activeDbStatuses())))
                .list();
        long now = System.currentTimeMillis();
        int expiredCount = 0;
        for (AigcDocs docs : activeDocs) {
            if (expireIfStale(docs, now)) {
                expiredCount++;
            }
        }
        aigcDocsService.lambdaQuery()
                .eq(AigcDocs::getEmbedStatus, DocumentIndexStatusEnum.COMPLETED.code())
                .isNotNull(AigcDocs::getEmbedError)
                .list()
                .forEach(docs -> clearCompletedError(docs, now));
        if (expiredCount > 0) {
            log.warn("知识库向量化超时任务已收敛，expiredCount={}", expiredCount);
        }
    }

    /** 将超时文档更新为失败，返回是否实际完成更新。 */
    private boolean expireIfStale(AigcDocs docs, long now) {
        if (!isActive(docs) || !isExpired(docs, now)) {
            return false;
        }
        boolean updated = aigcDocsService.lambdaUpdate()
                .eq(AigcDocs::getId, docs.getId())
                .and(status -> status
                        .in(AigcDocs::getEmbedStatus, activeStatusCodes())
                        .or()
                        .and(legacy -> legacy
                                .isNull(AigcDocs::getEmbedStatus)
                                .in(AigcDocs::getIndexingStatus, activeDbStatuses())))
                .set(AigcDocs::getIndexingStatus, DocumentIndexStatusEnum.FAILED.dbStatus())
                .set(AigcDocs::getEmbedStatus, DocumentIndexStatusEnum.FAILED.code())
                .set(AigcDocs::getEmbedError, TIMEOUT_ERROR)
                .set(AigcDocs::getEmbedEndTime, now)
                .set(AigcDocs::getUpdateTime, now)
                .update();
        if (updated) {
            docs.setIndexingStatus(DocumentIndexStatusEnum.FAILED.dbStatus());
            docs.setEmbedStatus(DocumentIndexStatusEnum.FAILED.code());
            docs.setEmbedError(TIMEOUT_ERROR);
            docs.setEmbedEndTime(now);
            docs.setUpdateTime(now);
            log.warn("知识库文档向量化任务超时，docsId={}, startTime={}", docs.getId(), taskStartTime(docs));
        }
        return updated;
    }

    private void clearCompletedError(AigcDocs docs, long now) {
        if (docs.getEmbedError() == null || docs.getEmbedError().isBlank()) {
            return;
        }
        boolean updated = aigcDocsService.lambdaUpdate()
                .eq(AigcDocs::getId, docs.getId())
                .eq(AigcDocs::getEmbedStatus, DocumentIndexStatusEnum.COMPLETED.code())
                .set(AigcDocs::getEmbedError, null)
                .set(AigcDocs::getUpdateTime, now)
                .update();
        if (updated) {
            docs.setEmbedError(null);
            docs.setUpdateTime(now);
        }
    }

    private boolean isActive(AigcDocs docs) {
        if (docs.getEmbedStatus() != null && !docs.getEmbedStatus().isBlank()) {
            return activeStatusCodes().contains(docs.getEmbedStatus());
        }
        return activeDbStatuses().contains(docs.getIndexingStatus());
    }

    private boolean isExpired(AigcDocs docs, long now) {
        Duration timeout = properties.getTaskTimeout();
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return false;
        }
        Long startTime = taskStartTime(docs);
        return startTime != null && now - startTime >= timeout.toMillis();
    }

    private Long taskStartTime(AigcDocs docs) {
        if (docs.getEmbedStartTime() != null) {
            return docs.getEmbedStartTime();
        }
        if (docs.getUpdateTime() != null) {
            return docs.getUpdateTime();
        }
        return docs.getCreateTime();
    }

    private List<String> activeStatusCodes() {
        return List.of(
                DocumentIndexStatusEnum.PENDING.code(),
                DocumentIndexStatusEnum.RUNNING.code()
        );
    }

    private List<Integer> activeDbStatuses() {
        return List.of(
                DocumentIndexStatusEnum.PENDING.dbStatus(),
                DocumentIndexStatusEnum.RUNNING.dbStatus()
        );
    }
}
