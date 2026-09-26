package cn.langchat.claw.common.ai.enums;

import java.util.Locale;

/**
 * AI 厂商类型枚举。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public enum AiProviderType {

    /** OpenAI。 */
    OPENAI("OPENAI"),

    /** Azure OpenAI。 */
    AZURE_OPENAI("AZURE_OPENAI"),

    /** 阿里百炼。 */
    DASHSCOPE("DASHSCOPE"),

    /** Anthropic。 */
    ANTHROPIC("ANTHROPIC"),

    /** Google Gemini。 */
    GEMINI("GEMINI"),

    /** DeepSeek。 */
    DEEPSEEK("DEEPSEEK"),

    /** 智谱。 */
    ZHIPU("ZHIPU"),

    /** Ollama。 */
    OLLAMA("OLLAMA"),

    /** 自定义 OpenAI 兼容协议。 */
    OPENAI_COMPATIBLE("OPENAI_COMPATIBLE");

    private final String code;

    AiProviderType(String code) {
        this.code = code;
    }

    /**
     * 获取厂商编码。
     */
    public String code() {
        return code;
    }

    /**
     * 按数据库值解析厂商枚举。
     */
    public static AiProviderType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return OPENAI_COMPATIBLE;
        }
        String normalized = code.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "OPENAI" -> OPENAI;
            case "AZURE_OPENAI" -> AZURE_OPENAI;
            case "DASHSCOPE" -> DASHSCOPE;
            case "ANTHROPIC" -> ANTHROPIC;
            case "GEMINI" -> GEMINI;
            case "DEEPSEEK" -> DEEPSEEK;
            case "ZHIPU" -> ZHIPU;
            case "OLLAMA" -> OLLAMA;
            case "OPENAI_COMPATIBLE" -> OPENAI_COMPATIBLE;
            default -> OPENAI_COMPATIBLE;
        };
    }
}
