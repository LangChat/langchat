package cn.langchat.claw.core.support;

import cn.langchat.claw.common.core.ErrorCode;

/**
 * 核心运行时错误码定义。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public enum CoreErrorCode implements ErrorCode {

    /** Agent 不存在。 */
    AGENT_NOT_FOUND("CORE_1001", "Agent 不存在或已禁用"),

    /** 知识库不存在。 */
    KNOWLEDGE_NOT_FOUND("CORE_1010", "知识库不存在或不可用"),

    /** 模型不存在。 */
    MODEL_NOT_FOUND("CORE_1002", "模型配置不存在或不可用"),

    /** 向量库不存在。 */
    VECTOR_STORE_NOT_FOUND("CORE_1003", "向量库配置不存在或不可用"),

    /** 技能不存在。 */
    SKILL_NOT_FOUND("CORE_1004", "Skill 配置不存在或不可用"),

    /** MCP 配置不存在。 */
    MCP_NOT_FOUND("CORE_1011", "MCP 配置不存在或不可用"),

    /** 会话不存在。 */
    CONVERSATION_NOT_FOUND("CORE_1012", "会话不存在或无权访问"),

    /** 当前供应商不支持。 */
    UNSUPPORTED_PROVIDER("CORE_1005", "当前模型供应商暂不支持"),

    /** 当前向量库不支持。 */
    UNSUPPORTED_VECTOR_STORE("CORE_1006", "当前向量库供应商暂不支持"),

    /** 请求参数非法。 */
    INVALID_CHAT_REQUEST("CORE_1007", "聊天请求参数不合法"),

    /** 对话运行失败。 */
    CHAT_RUNTIME_ERROR("CORE_1008", "对话运行失败"),

    /** 对话流超时。 */
    CHAT_STREAM_TIMEOUT("CORE_1009", "对话流执行超时");

    private final String code;
    private final String message;

    CoreErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
