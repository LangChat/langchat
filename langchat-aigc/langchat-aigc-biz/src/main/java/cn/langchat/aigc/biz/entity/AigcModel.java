package cn.langchat.aigc.biz.entity;

import cn.langchat.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_model")
@EqualsAndHashCode(callSuper = true)
public class AigcModel extends BaseDO {

    /** 模型主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** 模型类型。 */
    private String type;
    /** 模型名称。 */
    private String model;
    /** 模型供应商。 */
    private String provider;
    /** 模型别名。 */
    private String name;
    /** 最大输出 Token。 */
    private Integer maxToken;
    /** 温度参数。 */
    private Double temperature;
    /** TopP 参数。 */
    private Double topP;
    /** API Key。 */
    private String apiKey;
    /** 超时时间。 */
    private Integer timeout;
    /** 模型基础地址。 */
    private String baseUrl;
    /** 向量维度。 */
    private Integer dimension;
}
