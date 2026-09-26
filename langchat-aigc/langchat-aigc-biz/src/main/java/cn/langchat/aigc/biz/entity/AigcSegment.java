package cn.langchat.aigc.biz.entity;

import cn.langchat.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档切片实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_segment")
@EqualsAndHashCode(callSuper = true)
public class AigcSegment extends BaseDO {

    /** 切片主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 索引哈希。 */
    private String indexHash;

    /** 文档ID。 */
    private String docsId;

    /** 知识库ID。 */
    private String knowledgeId;

    /** 是否启用。 */
    private Boolean enabled;

    /** 切片名称。 */
    private String name;

    /** 切片位置。 */
    private Integer position;

    /** 切片内容。 */
    private String content;

    /** 处理状态。 */
    private Integer status;
}
