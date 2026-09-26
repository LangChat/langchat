package cn.langchat.claw.aigc.biz.entity;

import cn.langchat.claw.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_knowledge")
@EqualsAndHashCode(callSuper = true)
public class AigcKnowledge extends BaseDO {

    /** 知识库主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** 知识库名称。 */
    private String name;
    /** 封面地址。 */
    private String coverUrl;
    /** 最大召回数量。 */
    private Integer maxResults;
    /** 最低分数阈值。 */
    private Double minScore;
    /** 检索模型 ID。 */
    private String modelId;
    /** 视觉模型 ID。 */
    private String visionModelId;
    /** 向量库 ID。 */
    private String vectorStoreId;
    /** 向量模型 ID。 */
    private String vectorModelId;
    /** 是否开启重排。 */
    private Boolean rerank;
    /** 重排模型 ID。 */
    private String rerankModelId;
    /** 描述。 */
    private String description;
    /** 标签。 */
    private String tags;
}
