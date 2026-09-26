package cn.langchat.aigc.biz.entity;

import cn.langchat.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent API Key 实体。
 *
 * <p>用于将 Agent 应用通过 OpenAI 兼容接口对外提供，密钥维度记录调用统计。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Data
@TableName("aigc_agent_api_key")
@EqualsAndHashCode(callSuper = true)
public class AigcAgentApiKey extends BaseDO {

    /** 主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 关联 Agent ID。 */
    private String agentId;

    /** 密钥名称。 */
    private String name;

    /** API Key。 */
    private String apiKey;

    /** 状态：ENABLED / DISABLED。 */
    private String status;

    /** 备注。 */
    private String remark;

    /** 累计调用次数。 */
    private Integer callCount;

    /** 累计输入 Token。 */
    private Long inputTokens;

    /** 累计输出 Token。 */
    private Long outputTokens;

    /** 最后调用时间。 */
    private Long lastCallTime;
}
