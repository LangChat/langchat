package cn.langchat.core.chat.model.response;

import java.util.List;
import lombok.Data;

/**
 * 知识库解析预览结果。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class KnowledgeParsePreviewResult {

    /** 知识库 ID。 */
    private String knowledgeId;

    /** 预览文档数。 */
    private Integer docsCount;

    /** 文档解析预览列表。 */
    private List<KnowledgeDocumentPreview> docs;
}
