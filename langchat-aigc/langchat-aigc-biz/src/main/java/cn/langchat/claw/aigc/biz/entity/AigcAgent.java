package cn.langchat.claw.aigc.biz.entity;

import cn.langchat.claw.common.persistence.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 主配置实体。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Data
@TableName("aigc_agent")
@EqualsAndHashCode(callSuper = true)
public class AigcAgent extends BaseDO {

    /** Agent 主键。 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** Agent 名称。 */
    private String agentName;

    /** Agent 描述。 */
    private String description;

    /** Agent 状态。 */
    private String status;

    /** 系统提示词。 */
    private String systemPrompt;

    /** 推理模型 ID。 */
    private String reasoningModelId;

    /** 关联知识库 ID 列表。 */
    private String knowledgeIds;

    /** 关联技能 ID 列表。 */
    private String skillIds;

    /** 关联 MCP 服务 ID 列表。 */
    private String mcpIds;

    /** 模型配置 JSON（温度、TopP、最大输出等）。 */
    private String modelConfigJson;

    /** Agent 头像。 */
    private String avatar;

    /** 扩展元数据 JSON。 */
    private String metaJson;
}
