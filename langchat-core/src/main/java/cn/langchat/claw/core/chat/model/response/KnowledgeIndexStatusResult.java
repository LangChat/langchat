package cn.langchat.claw.core.chat.model.response;

import java.util.List;
import lombok.Data;

/**
 * 知识库向量化状态查询结果。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class KnowledgeIndexStatusResult {

    /** 知识库 ID。 */
    private String knowledgeId;

    /** 文档总数。 */
    private Integer docsCount;

    /** 待处理文档数。 */
    private Integer pendingCount;

    /** 执行中文档数。 */
    private Integer runningCount;

    /** 已完成文档数。 */
    private Integer completedCount;

    /** 已失败文档数。 */
    private Integer failedCount;

    /** 是否全部完成。 */
    private Boolean finished;

    /** 完成进度百分比。 */
    private Integer progressPercent;

    /** 文档状态明细。 */
    private List<KnowledgeDocumentIndexStatus> docs;
}
