package cn.langchat.core.chat.model.response;

import java.util.List;
import lombok.Data;

/**
 * 单篇文档解析预览结果。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class KnowledgeDocumentPreview {

    /** 文档 ID。 */
    private String docsId;

    /** 文档标题。 */
    private String title;

    /** 实际命中的解析器名称。 */
    private String parserName;

    /** 文本总长度。 */
    private Integer contentLength;

    /** 解析后的逻辑分段总数。 */
    private Integer sectionCount;

    /** 切片总数。 */
    private Integer chunkCount;

    /** 分段预览内容。 */
    private List<String> sections;

    /** 切片预览内容。 */
    private List<String> chunks;
}
