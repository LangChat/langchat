package cn.langchat.core.chat.enums;

/**
 * 对话消息类型枚举。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public enum ChatMessageTypeEnum {

    /** 文本消息。 */
    TEXT("TEXT");

    private final String code;

    ChatMessageTypeEnum(String code) {
        this.code = code;
    }

    /**
     * 获取消息类型编码。
     */
    public String code() {
        return code;
    }
}
