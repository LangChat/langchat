package cn.langchat.claw.core.image.controller;

import lombok.Data;

/**
 * 图片识别 (OCR) 请求。
 *
 * @author LangChat Team
 * @since 2026/8/27
 */
@Data
public class ImageRecognizeRequest {

    /** 视觉模型ID。 */
    private String modelId;

    /** 提示词。 */
    private String prompt;

    /** 图片 data URL，例如 {@code data:image/png;base64,xxxx}。 */
    private String image;
}
