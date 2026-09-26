package cn.langchat.core.chat.model.request;

import java.util.List;
import lombok.Data;

/**
 * 知识库索引请求。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class KnowledgeIndexRequest {

    /** 文档 ID 列表，为空时处理当前知识库下全部文档。 */
    private List<String> docsIds;

    /** 切片大小。 */
    private Integer chunkSize;

    /** 切片重叠大小。 */
    private Integer overlapSize;
}
