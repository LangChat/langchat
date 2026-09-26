package cn.langchat.claw.core.chat.enums;

/**
 * Agent 流式事件类型枚举。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public enum AgentStreamEventTypeEnum {

    /** 消息开始。 */
    MESSAGE_START("message.start"),

    /** 消息增量。 */
    MESSAGE_DELTA("message.delta"),

    /** RAG 检索完成。 */
    RAG_RETRIEVED("rag.retrieved"),

    /** Tool 调用前。 */
    TOOL_BEFORE("tool.before"),

    /** Tool 调用完成。 */
    TOOL_EXECUTED("tool.executed"),

    /** 执行日志增量。 */
    LOG_DELTA("log.delta"),

    /** 智能问数表格结果。 */
    ANALYSIS_TABLE("analysis.table"),

    /** 智能问数 ECharts 配置。 */
    ANALYSIS_ECHART("analysis.echart"),

    /** 消息完成。 */
    MESSAGE_COMPLETED("message.completed"),

    /** 消息停止。 */
    MESSAGE_STOP("message.stop"),

    /** 流超时。 */
    TIMEOUT("timeout"),

    /** 流异常。 */
    ERROR("error"),

    /** 流结束。 */
    DONE("done");

    private final String code;

    AgentStreamEventTypeEnum(String code) {
        this.code = code;
    }

    /**
     * 获取 SSE 事件编码。
     */
    public String code() {
        return code;
    }
}
