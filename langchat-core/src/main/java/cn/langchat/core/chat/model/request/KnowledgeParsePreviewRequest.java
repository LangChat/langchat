package cn.langchat.core.chat.model.request;

import java.util.List;
import lombok.Data;

/**
 * 知识库解析预览请求。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class KnowledgeParsePreviewRequest {

    /** 文档 ID 列表，为空时预览当前知识库下全部文档。 */
    private List<String> docsIds;

    /** 切片大小。 */
    private Integer chunkSize;

    /** 切片重叠大小。 */
    private Integer overlapSize;

    /** 返回的分段预览条数。 */
    private Integer sectionLimit;

    /** 返回的切片预览条数。 */
    private Integer chunkLimit;
}
