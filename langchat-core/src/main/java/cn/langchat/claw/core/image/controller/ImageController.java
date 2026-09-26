package cn.langchat.claw.core.image.controller;

import cn.langchat.claw.common.ai.model.ImageGenerationRequest;
import cn.langchat.claw.common.ai.model.ImageGenerationResult;
import cn.langchat.claw.common.ai.service.ImageAiService;
import cn.langchat.claw.common.core.ApiResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图片 AI 控制器。
 *
 * <p>两个场景（图片生成 / 图片识别）均由前端传入模型 ID 与请求参数，
 * 底层复用 {@link ImageAiService} 公共封装。</p>
 *
 * @author LangChat Team
 * @since 2026/8/27
 */
@RestController
@RequestMapping("/api/v1/image")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final ImageAiService imageAiService;

    /**
     * 图片生成。
     */
    @PostMapping("/generate")
    public ApiResponse<ImageGenerationResult> generate(@RequestBody ImageGenerateRequest request) {
        log.info("图片生成请求，modelId={}, size={}, n={}", request.getModelId(), request.getSize(), request.getN());
        ImageGenerationRequest payload = new ImageGenerationRequest(
                request.getModelId(),
                request.getPrompt(),
                request.getSize(),
                request.getQuality(),
                request.getN()
        );
        return ApiResponse.success(imageAiService.generateImage(request.getModelId(), payload));
    }

    /**
     * 图片识别 (OCR)。
     */
    @PostMapping("/recognition")
    public ApiResponse<Map<String, String>> recognition(@RequestBody ImageRecognizeRequest request) {
        log.info("图片识别请求，modelId={}", request.getModelId());
        String text = imageAiService.recognizeText(request.getModelId(), request.getPrompt(), request.getImage());
        return ApiResponse.success(Map.of("text", text));
    }
}
