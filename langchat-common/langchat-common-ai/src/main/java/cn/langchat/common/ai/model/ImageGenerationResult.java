package cn.langchat.common.ai.model;

/**
 * 图片生成结果对象。
 *
 * @param url 图片链接
 * @param base64Data base64 编码的图片数据
 * @param revisedPrompt 由模型修正后的提示词
 * @author LangChat Team
 * @since 2026/8/27
 */
public record ImageGenerationResult(
        String url,
        String base64Data,
        String revisedPrompt
) {
}
