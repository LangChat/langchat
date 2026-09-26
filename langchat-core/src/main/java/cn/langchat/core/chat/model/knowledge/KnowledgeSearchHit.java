package cn.langchat.core.chat.model.knowledge;

import java.util.Map;
import lombok.Data;

/**
 * 知识检索命中结果。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class KnowledgeSearchHit {

    /** 知识库 ID。 */
    private String knowledgeId;

    /** 知识库名称。 */
    private String knowledgeName;

    /** 文档 ID。 */
    private String docsId;

    /** 文档名称。 */
    private String docsName;

    /** 切片 ID。 */
    private String segmentId;

    /** 命中分数。 */
    private Double score;

    /** 命中内容。 */
    private String content;

    /** 元数据。 */
    private Map<String, Object> metadata;
}
