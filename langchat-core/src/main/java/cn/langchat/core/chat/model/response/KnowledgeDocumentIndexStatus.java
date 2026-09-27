package cn.langchat.core.chat.model.response;

import lombok.Data;

/**
 * 单篇文档向量化状态。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Data
public class KnowledgeDocumentIndexStatus {

    /** 文档 ID。 */
    private String docsId;

    /** 文档名称。 */
    private String name;

    /** 文档后缀。 */
    private String ext;

    /** 数据库索引状态值。 */
    private Integer indexingStatus;

    /** 向量化状态编码。 */
    private String embedStatus;

    /** 向量化错误信息。 */
    private String embedError;

    /** 向量化开始时间。 */
    private Long embedStartTime;

    /** 向量化结束时间。 */
    private Long embedEndTime;

    /** 当前耗时，单位毫秒。 */
    private Long costMs;

    /** 已生成分段条数。 */
    private Integer segmentCount;

    /** 已生成分段字符数。 */
    private Long charCount;

    /** 最后更新时间。 */
    private Long updateTime;
}
