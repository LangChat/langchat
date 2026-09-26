package cn.langchat.core.chat.enums;

/**
 * OpenAI 协议对象类型枚举。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public enum OpenAiObjectTypeEnum {

    /** 流式聊天分片。 */
    CHAT_COMPLETION_CHUNK("chat.completion.chunk");

    private final String code;

    OpenAiObjectTypeEnum(String code) {
        this.code = code;
    }

    /**
     * 获取对象类型编码。
     */
    public String code() {
        return code;
    }
}
