package cn.langchat.claw.aigc.biz.event;

/**
 * MCP 配置变更事件。MCP 配置被更新或删除时发布，
 * 运行时监听后立即停止对应的 MCP 客户端连接（主动暂停）。
 *
 * @param mcpId MCP 主键
 * @author LangChat Team
 * @since 2026/9/26
 */
public record McpConfigChangedEvent(String mcpId) {
}
