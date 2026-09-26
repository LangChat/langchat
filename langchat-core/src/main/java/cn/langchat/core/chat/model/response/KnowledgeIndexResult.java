package cn.langchat.core.chat.model.response;

import lombok.Data;

/**
 * 知识库索引结果。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class KnowledgeIndexResult {

    /** 知识库 ID。 */
    private String knowledgeId;

    /** 提交文档数。 */
    private Integer docsCount;

    /** 已提交异步任务数。 */
    private Integer submittedCount;

    /** 当前是否异步执行。 */
    private Boolean async;

    /** 当前任务状态。 */
    private String taskStatus;

    /** 已生成切片数。异步提交阶段固定为 0。 */
    private Integer segmentCount;

    /** 写入向量条数。异步提交阶段固定为 0。 */
    private Integer embeddedCount;
}
