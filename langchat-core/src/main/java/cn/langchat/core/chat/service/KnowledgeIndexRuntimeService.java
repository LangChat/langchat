package cn.langchat.core.chat.service;

import cn.langchat.core.chat.model.request.KnowledgeIndexRequest;
import cn.langchat.core.chat.model.request.KnowledgeParsePreviewRequest;
import cn.langchat.core.chat.model.response.KnowledgeIndexResult;
import cn.langchat.core.chat.model.response.KnowledgeIndexStatusResult;
import cn.langchat.core.chat.model.response.KnowledgeParsePreviewResult;
import java.util.List;

/**
 * 知识库索引运行时服务。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public interface KnowledgeIndexRuntimeService {

    /**
     * 触发知识库文档索引。
     */
    KnowledgeIndexResult indexKnowledge(String knowledgeId, KnowledgeIndexRequest request);

    /**
     * 预览知识库文档解析结果。
     */
    KnowledgeParsePreviewResult previewKnowledge(String knowledgeId, KnowledgeParsePreviewRequest request);

    /**
     * 查询知识库文档向量化状态。
     */
    KnowledgeIndexStatusResult queryIndexStatus(String knowledgeId, List<String> docsIds);
}
