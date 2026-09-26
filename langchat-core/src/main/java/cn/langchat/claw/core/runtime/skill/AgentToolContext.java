package cn.langchat.claw.core.runtime.skill;

import cn.langchat.claw.aigc.biz.entity.AigcAgent;
import cn.langchat.claw.aigc.biz.entity.AigcKnowledge;
import cn.langchat.claw.aigc.biz.entity.AigcSkill;
import java.util.List;

/**
 * Agent Skill 构建上下文。
 *
 * @param agent Agent 配置
 * @param conversationId 会话 ID
 * @param userId 当前用户 ID
 * @param skill Skill 配置
 * @param knowledges 已绑定知识库
 * @author LangChat Team
 * @since 2026/3/24
 */
public record AgentToolContext(
        AigcAgent agent,
        String conversationId,
        String userId,
        AigcSkill skill,
        List<AigcKnowledge> knowledges
) {
}
