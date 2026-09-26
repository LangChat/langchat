package cn.langchat.core.runtime.skill;

import cn.langchat.aigc.biz.entity.AigcSkill;

/**
 * Agent Skill 工具提供器。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public interface AgentToolProvider {

    /**
     * 判断当前提供器是否支持指定 Skill。
     */
    boolean supports(AigcSkill skill);

    /**
     * 构建运行时工具定义。
     */
    AgentToolDefinition buildDefinition(AgentToolContext context);
}
