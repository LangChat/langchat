package cn.langchat.common.ai.service;

import cn.langchat.common.ai.model.ImageGenerationRequest;
import cn.langchat.common.ai.model.ImageGenerationResult;

/**
 * 图片 AI 服务接口。
 *
 * <p>实现按 modelId 从模型管理读取模型配置（baseUrl / apiKey / model），
 * 两个场景只需传入模型 ID 与请求参数即可复用底层调用。</p>
 *
 * @author LangChat Team
 * @since 2026/8/27
 */
public interface ImageAiService {

    /**
     * 根据提示词生成图片。
     */
    ImageGenerationResult generateImage(String modelId, ImageGenerationRequest request);

    /**
     * 识别图片文字 (OCR)。
     *
     * @param modelId 视觉模型ID
     * @param prompt 提示词
     * @param imageDataUrl 图片 data URL (e.g. {@code data:image/png;base64,xxxx})
     * @return 识别出的文本
     */
    String recognizeText(String modelId, String prompt, String imageDataUrl);
}
