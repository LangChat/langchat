package cn.langchat.core.chat.service;

/**
 * 单文档知识索引处理器。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public interface KnowledgeDocumentIndexProcessor {

    /**
     * 同步执行单个文档的解析、切片与向量化。
     */
    void processDocument(String knowledgeId, String docsId, Integer chunkSize, Integer overlapSize);
}
