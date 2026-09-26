package cn.langchat.common.ai.model;

/**
 * 图片生成请求对象。
 *
 * @param modelId 模型ID
 * @param prompt 提示词
 * @param size 图片尺寸，例如 {@code 1024x1024}、{@code 1536x1024}
 * @param quality 图片质量，例如 {@code standard}、{@code high}
 * @param n 生成数量
 * @author LangChat Team
 * @since 2026/8/27
 */
public record ImageGenerationRequest(
        String modelId,
        String prompt,
        String size,
        String quality,
        Integer n
) {
}
