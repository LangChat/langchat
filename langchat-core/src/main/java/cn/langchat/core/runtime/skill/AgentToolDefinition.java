package cn.langchat.core.runtime.skill;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;

/**
 * Agent 运行时工具定义。
 *
 * @param specification 工具声明
 * @param executor 工具执行器
 * @author LangChat Team
 * @since 2026/3/25
 */
public record AgentToolDefinition(
        ToolSpecification specification,
        ToolExecutor executor
) {
}
