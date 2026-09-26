package cn.langchat.aigc.biz.entity;

import cn.langchat.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_docs")
@EqualsAndHashCode(callSuper = true)
public class AigcDocs extends BaseDO {

    /** 文档主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** 文件资源 ID。 */
    private String ossId;
    /** 知识库 ID。 */
    private String knowledgeId;
    /** 父节点 ID。 */
    private String parentId;
    /** 是否启用。 */
    private Boolean enabled;
    /** 索引状态。 */
    private Integer indexingStatus;
    /** 向量化状态。 */
    private String embedStatus;
    /** 向量化错误信息。 */
    private String embedError;
    /** 向量化开始时间。 */
    private Long embedStartTime;
    /** 向量化结束时间。 */
    private Long embedEndTime;
    /** 导入配置。 */
    private String ingestionConfig;
    /** 文档类型。 */
    private String type;
    /** 文档名称。 */
    private String name;
    /** 文件后缀。 */
    private String ext;
    /** 文档地址。 */
    private String url;
    /** PDF 地址。 */
    private String pdfUrl;
    /** 文档内容。 */
    private String content;
    /** 文件大小。 */
    private Integer size;
}
