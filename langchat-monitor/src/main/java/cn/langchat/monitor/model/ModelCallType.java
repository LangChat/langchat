package cn.langchat.monitor.model;

import java.util.Locale;

/**
 * 模型调用类型。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
public enum ModelCallType {

    /** 对话模型调用。 */
    CHAT,

    /** 向量模型调用。 */
    EMBEDDING,

    /** 文生图调用。 */
    IMAGE,

    /** 图像识别（OCR）调用。 */
    OCR;

    /**
     * 按名称解析，未知时返回 {@link #CHAT}。
     */
    public static ModelCallType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return CHAT;
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CHAT;
        }
    }
}
