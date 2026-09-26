package cn.langchat.core.chat.service.impl;

import cn.langchat.core.chat.service.KnowledgeDocumentIndexProcessor;
import cn.langchat.core.chat.service.KnowledgeIndexAsyncService;
import cn.langchat.core.runtime.config.KnowledgeIndexExecutorNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 知识库异步索引服务实现。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIndexAsyncServiceImpl implements KnowledgeIndexAsyncService {

    private final KnowledgeDocumentIndexProcessor knowledgeDocumentIndexProcessor;

    @Override
    @Async(KnowledgeIndexExecutorNames.DOCUMENT_INDEX_TASK_EXECUTOR)
    public void submitDocumentIndex(String knowledgeId, String docsId, Integer chunkSize, Integer overlapSize) {
        log.info("提交文档异步向量化任务，knowledgeId={}, docsId={}", knowledgeId, docsId);
        knowledgeDocumentIndexProcessor.processDocument(knowledgeId, docsId, chunkSize, overlapSize);
    }
}
