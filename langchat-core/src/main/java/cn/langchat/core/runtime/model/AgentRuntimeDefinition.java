package cn.langchat.core.runtime.model;

import cn.langchat.aigc.biz.entity.AigcAgent;
import cn.langchat.aigc.biz.entity.AigcKnowledge;
import cn.langchat.aigc.biz.entity.AigcMcp;
import cn.langchat.aigc.biz.entity.AigcModel;
import cn.langchat.aigc.biz.entity.AigcSkill;
import java.util.List;

/**
 * Agent 运行时装配结果。
 *
 * @param agent Agent 配置
 * @param chatModelConfig 聊天模型配置
 * @param knowledges 已绑定知识库列表
 * @param skills 已启用 Skill 列表
 * @param mcps 已绑定 MCP 服务列表
 * @author LangChat Team
 * @since 2026/3/24
 */
public record AgentRuntimeDefinition(
        AigcAgent agent,
        AigcModel chatModelConfig,
        List<AigcKnowledge> knowledges,
        List<AigcSkill> skills,
        List<AigcMcp> mcps
) {
}
