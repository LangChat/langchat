package cn.langchat.core.chat.service;

import cn.langchat.aigc.biz.entity.AigcSegment;
import cn.langchat.core.chat.model.request.SegmentContentUpdateRequest;
import cn.langchat.core.chat.model.request.SegmentDeleteRequest;
import cn.langchat.core.chat.model.request.SegmentEnabledRequest;
import java.util.List;

/**
 * 知识库分段运行时服务。
 *
 * @author LangChat Team
 * @since 2026/9/27
 */
public interface KnowledgeSegmentService {

    /**
     * 查询知识库下指定文档的分段列表，按分段位置升序。
     */
    List<AigcSegment> listSegments(String knowledgeId, String docsId);

    /**
     * 更新分段内容，仅写入数据库，向量数据重新向量化后生效。
     */
    void updateSegmentContent(String knowledgeId, String segmentId, SegmentContentUpdateRequest request);

    /**
     * 启停分段，同步维护向量库数据：停用时移除向量，启用时重新写入向量。
     */
    void updateSegmentEnabled(String knowledgeId, String segmentId, SegmentEnabledRequest request);

    /**
     * 对单个分段重新向量化。
     */
    void reindexSegment(String knowledgeId, String segmentId);

    /**
     * 删除分段并清理向量数据。
     */
    void deleteSegment(String knowledgeId, String segmentId);

    /**
     * 批量删除分段并清理向量数据。
     */
    void deleteSegments(String knowledgeId, SegmentDeleteRequest request);
}
