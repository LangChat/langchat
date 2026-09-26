package cn.langchat.core.image.controller;

import lombok.Data;

/**
 * 图片生成请求。
 *
 * @author LangChat Team
 * @since 2026/8/27
 */
@Data
public class ImageGenerateRequest {

    /** 模型ID。 */
    private String modelId;

    /** 提示词。 */
    private String prompt;

    /** 图片尺寸，例如 {@code 1024x1024}。 */
    private String size;

    /** 图片质量，例如 {@code standard}、{@code high}。 */
    private String quality;

    /** 生成数量。 */
    private Integer n;
}
