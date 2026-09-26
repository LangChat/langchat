package cn.langchat.claw.core.image;

import cn.hutool.core.util.StrUtil;
import cn.langchat.claw.aigc.biz.entity.AigcModel;
import cn.langchat.claw.aigc.biz.service.AigcModelService;
import cn.langchat.claw.common.ai.enums.AiProviderType;
import cn.langchat.claw.common.ai.model.ImageGenerationRequest;
import cn.langchat.claw.common.ai.model.ImageGenerationResult;
import cn.langchat.claw.common.ai.service.ImageAiService;
import cn.langchat.claw.common.exception.BizException;
import cn.langchat.claw.core.support.CoreErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiImageModel;
import dev.langchain4j.model.output.Response;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * OpenAI 兼容协议的图片 AI 实现。
 *
 * <p>按 modelId 从模型管理读取模型配置并构造底层模型客户端，
 * 图片生成使用 {@link OpenAiImageModel}，图片识别 (OCR) 使用
 * {@link OpenAiChatModel} 的多模态能力。</p>
 *
 * @author LangChat Team
 * @since 2026/8/27
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiImageService implements ImageAiService {

    private static final String EMPTY_API_KEY = "EMPTY_API_KEY";
    private static final String DEFAULT_IMAGE_SIZE = "1024x1024";
    private static final String DEFAULT_IMAGE_QUALITY = "standard";
    private static final int DEFAULT_IMAGE_COUNT = 1;
    private static final String DEFAULT_OCR_PROMPT = "请识别图片中的文字内容并原样输出。";

    private final AigcModelService aigcModelService;

    @Override
    public ImageGenerationResult generateImage(String modelId, ImageGenerationRequest request) {
        AigcModel model = loadModel(modelId);
        AiProviderType provider = AiProviderType.fromCode(model.getProvider());
        log.info("生成图片，modelId={}, provider={}, model={}, size={}, n={}",
                modelId, provider, model.getModel(), request.size(), request.n());
        return switch (provider) {
            case GEMINI -> geminiGenerateImage(model, request);
            case DASHSCOPE -> wanxGenerateImage(model, request);
            default -> openAiGenerateImage(model, request);
        };
    }

    private ImageGenerationResult openAiGenerateImage(AigcModel model, ImageGenerationRequest request) {
        OpenAiImageModel.OpenAiImageModelBuilder builder = OpenAiImageModel.builder()
                .modelName(model.getModel())
                .apiKey(defaultApiKey(model))
                .size(StrUtil.blankToDefault(request.size(), DEFAULT_IMAGE_SIZE))
                .quality(StrUtil.blankToDefault(request.quality(), DEFAULT_IMAGE_QUALITY))
                .timeout(timeout(model));
        applyBaseUrl(model, builder::baseUrl);
        OpenAiImageModel imageModel = builder.build();

        int count = request.n() == null || request.n() <= 0 ? DEFAULT_IMAGE_COUNT : request.n();
        Response<List<Image>> response = imageModel.generate(request.prompt(), count);
        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new BizException(CoreErrorCode.CHAT_RUNTIME_ERROR);
        }

        Image image = response.content().get(0);
        String url = image.url() == null ? null : image.url().toString();
        return new ImageGenerationResult(url, image.base64Data(), image.revisedPrompt());
    }

    /**
     * 通义万相文生图（DashScope 官方协议）。
     */
    private ImageGenerationResult wanxGenerateImage(AigcModel model, ImageGenerationRequest request) {
        int count = request.n() == null || request.n() <= 0 ? DEFAULT_IMAGE_COUNT : request.n();
        WanxImageModel imageModel = WanxImageModel.builder()
                .modelName(model.getModel())
                .apiKey(defaultApiKey(model))
                .build();
        Response<List<Image>> response = imageModel.generate(request.prompt(), count);
        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new BizException(CoreErrorCode.CHAT_RUNTIME_ERROR);
        }
        Image image = response.content().get(0);
        String url = image.url() == null ? null : image.url().toString();
        return new ImageGenerationResult(url, image.base64Data(), image.revisedPrompt());
    }

    /**
     * Gemini 文生图（generativelanguage :predict 标准协议，返回 base64）。
     */
    private ImageGenerationResult geminiGenerateImage(AigcModel model, ImageGenerationRequest request) {
        int count = request.n() == null || request.n() <= 0 ? DEFAULT_IMAGE_COUNT : request.n();
        String baseUrl = StrUtil.blankToDefault(
                model.getBaseUrl(), "https://generativelanguage.googleapis.com/v1beta");
        String endpoint = StrUtil.removeSuffix(baseUrl, "/")
                + "/models/"
                + model.getModel()
                + ":predict";

        Map<String, Object> body = Map.of(
                "instances", List.of(Map.of("prompt", request.prompt())),
                "parameters", Map.of("sampleCount", count)
        );

        JsonNode response = RestClient.create()
                .post()
                .uri(endpoint)
                .header("x-goog-api-key", defaultApiKey(model))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.has("predictions")
                || response.path("predictions").isEmpty()
                || response.path("predictions").get(0).path("bytesBase64Encoded").asText("").isBlank()) {
            throw new BizException(CoreErrorCode.CHAT_RUNTIME_ERROR);
        }
        JsonNode prediction = response.path("predictions").get(0);
        String base64 = prediction.path("bytesBase64Encoded").asText();
        String revisedPrompt = prediction.path("prompt").asText(null);
        return new ImageGenerationResult(null, base64, revisedPrompt);
    }

    @Override
    public String recognizeText(String modelId, String prompt, String imageDataUrl) {
        AigcModel model = loadModel(modelId);
        log.info("识别图片文字，modelId={}, model={}", modelId, model.getModel());

        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .modelName(model.getModel())
                .apiKey(defaultApiKey(model))
                .timeout(timeout(model));
        applyBaseUrl(model, builder::baseUrl);
        OpenAiChatModel chatModel = builder.build();

        String userPrompt = StrUtil.blankToDefault(prompt, DEFAULT_OCR_PROMPT);
        UserMessage userMessage = UserMessage.from(
                TextContent.from(userPrompt),
                ImageContent.from(imageDataUrl)
        );
        ChatResponse response = chatModel.chat(ChatRequest.builder()
                .messages(List.of(userMessage))
                .build());
        if (response == null || response.aiMessage() == null) {
            throw new BizException(CoreErrorCode.CHAT_RUNTIME_ERROR);
        }
        return response.aiMessage().text();
    }

    /**
     * 加载模型配置，不存在则抛出异常。
     */
    private AigcModel loadModel(String modelId) {
        if (StrUtil.isBlank(modelId)) {
            throw new BizException(CoreErrorCode.MODEL_NOT_FOUND);
        }
        AigcModel model = aigcModelService.getById(modelId);
        if (model == null) {
            throw new BizException(CoreErrorCode.MODEL_NOT_FOUND);
        }
        return model;
    }

    private String defaultApiKey(AigcModel model) {
        return StrUtil.isBlank(model.getApiKey()) ? EMPTY_API_KEY : model.getApiKey();
    }

    private Duration timeout(AigcModel model) {
        int minutes = model.getTimeout() == null || model.getTimeout() <= 0 ? 3 : model.getTimeout();
        return Duration.ofMinutes(minutes);
    }

    private void applyBaseUrl(AigcModel model, java.util.function.Consumer<String> consumer) {
        if (StrUtil.isNotBlank(model.getBaseUrl())) {
            consumer.accept(model.getBaseUrl());
        }
    }
}
