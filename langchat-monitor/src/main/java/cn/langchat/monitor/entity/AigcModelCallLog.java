package cn.langchat.monitor.entity;

import cn.langchat.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型调用日志实体。
 *
 * <p>记录每一次模型调用的元信息与消耗，是模型调用监控报表的数据来源。
 * 统计字段（模型名、供应商、类型）冗余存储，避免报表查询时回表关联模型配置。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Data
@TableName("aigc_model_call_log")
@EqualsAndHashCode(callSuper = true)
public class AigcModelCallLog extends BaseDO {

    /** 主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** 模型配置 ID。 */
    private String modelId;
    /** 模型名称。 */
    private String modelName;
    /** 供应商。 */
    private String provider;
    /** 调用类型：CHAT / EMBEDDING / IMAGE / OCR。 */
    private String callType;
    /** 调用场景：AGENT_CHAT / KNOWLEDGE_INDEX / DATA_ANALYSIS / IMAGE_GENERATE / IMAGE_OCR。 */
    private String scene;
    /** 调用状态：SUCCESS / ERROR。 */
    private String status;
    /** 输入 Token 数。 */
    private Integer inputToken;
    /** 输出 Token 数。 */
    private Integer outputToken;
    /** 总 Token 数。 */
    private Integer totalToken;
    /** 调用耗时，单位毫秒。 */
    private Long duration;
    /** 处理条目数：向量化条数 / 生成图片张数等。 */
    private Integer itemCount;
    /** 错误信息，成功时为空。 */
    private String errorMessage;
}
