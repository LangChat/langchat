package cn.langchat.core.chat.service;

/**
 * 知识库异步索引服务。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public interface KnowledgeIndexAsyncService {

    /**
     * 异步处理单个文档向量化任务。
     */
    void submitDocumentIndex(String knowledgeId, String docsId, Integer chunkSize, Integer overlapSize);
}
