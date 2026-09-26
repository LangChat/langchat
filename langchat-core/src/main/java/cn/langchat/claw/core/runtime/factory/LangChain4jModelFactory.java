package cn.langchat.claw.core.runtime.factory;

import cn.hutool.core.util.StrUtil;
import cn.langchat.claw.aigc.biz.entity.AigcAgent;
import cn.langchat.claw.aigc.biz.entity.AigcModel;
import cn.langchat.claw.common.ai.enums.AiProviderType;
import cn.langchat.claw.common.exception.BizException;
import cn.langchat.claw.core.support.CoreErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LangChain4j 模型工厂。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Component
@Slf4j
public class LangChain4jModelFactory {

    private static final String EMPTY_API_KEY = "EMPTY_API_KEY";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ConcurrentMap<String, StreamingChatModel> chatModelCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, EmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();

    /**
     * 构建流式聊天模型。
     */
    public StreamingChatModel getStreamingChatModel(AigcModel model, AigcAgent agent) {
        AgentModelConfig config = parseAgentModelConfig(agent);
        String cacheKey = String.join(
                ":",
                model.getId(),
                String.valueOf(model.getUpdateTime()),
                String.valueOf(temperature(config, model)),
                String.valueOf(topP(config, model)),
                String.valueOf(maxTokens(config, model))
        );
        return chatModelCache.computeIfAbsent(cacheKey, key -> createStreamingChatModel(model, config));
    }

    /**
     * 构建向量模型。
     */
    public EmbeddingModel getEmbeddingModel(AigcModel model) {
        String cacheKey = model.getId() + ":" + model.getUpdateTime();
        return embeddingModelCache.computeIfAbsent(cacheKey, key -> createEmbeddingModel(model));
    }

    private StreamingChatModel createStreamingChatModel(AigcModel model, AgentModelConfig config) {
        AiProviderType provider = AiProviderType.fromCode(model.getProvider());
        log.info("构建流式聊天模型，modelId={}, provider={}, model={}", model.getId(), provider, model.getModel());
        return switch (provider) {
            case OPENAI, DEEPSEEK, OPENAI_COMPATIBLE -> openAiCompatibleStreamingModel(model, config);
            case OLLAMA -> ollamaStreamingModel(model, config);
            case DASHSCOPE -> dashScopeStreamingModel(model, config);
            case ZHIPU -> zhipuStreamingModel(model, config);
            case GEMINI -> geminiStreamingModel(model, config);
            default -> throw new BizException(CoreErrorCode.UNSUPPORTED_PROVIDER);
        };
    }

    private EmbeddingModel createEmbeddingModel(AigcModel model) {
        AiProviderType provider = AiProviderType.fromCode(model.getProvider());
        log.info("构建向量模型，modelId={}, provider={}, model={}", model.getId(), provider, model.getModel());
        return switch (provider) {
            case OPENAI, DEEPSEEK, OPENAI_COMPATIBLE -> openAiCompatibleEmbeddingModel(model);
            case OLLAMA -> ollamaEmbeddingModel(model);
            case DASHSCOPE -> dashScopeEmbeddingModel(model);
            case ZHIPU -> zhipuEmbeddingModel(model);
            case GEMINI -> geminiEmbeddingModel(model);
            default -> throw new BizException(CoreErrorCode.UNSUPPORTED_PROVIDER);
        };
    }

    private StreamingChatModel openAiCompatibleStreamingModel(AigcModel model, AgentModelConfig config) {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .modelName(model.getModel())
                .apiKey(defaultApiKey(model))
                .temperature(temperature(config, model))
                .topP(topP(config, model))
                .maxTokens(maxTokens(config, model))
                .timeout(timeout(model));
        applyBaseUrl(model, builder::baseUrl);
        return builder.build();
    }

    private StreamingChatModel ollamaStreamingModel(AigcModel model, AgentModelConfig config) {
        OllamaStreamingChatModel.OllamaStreamingChatModelBuilder builder = OllamaStreamingChatModel.builder()
                .modelName(model.getModel())
                .temperature(temperature(config, model))
                .topP(topP(config, model))
                .numPredict(maxTokens(config, model))
                .timeout(timeout(model));
        applyBaseUrl(model, builder::baseUrl);
        return builder.build();
    }

    private StreamingChatModel dashScopeStreamingModel(AigcModel model, AgentModelConfig config) {
        QwenStreamingChatModel.QwenStreamingChatModelBuilder builder = QwenStreamingChatModel.builder()
                .modelName(model.getModel())
                .apiKey(defaultApiKey(model))
                .temperature(toFloat(temperature(config, model)))
                .topP(topP(config, model))
                .maxTokens(maxTokens(config, model));
        // DashScope 原生 SDK 自带默认端点，不能把 OpenAI 兼容地址（compatible-mode）传给它
        applyDashScopeBaseUrl(model, builder::baseUrl);
        return builder.build();
    }

    private EmbeddingModel openAiCompatibleEmbeddingModel(AigcModel model) {
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .modelName(model.getModel())
                .apiKey(defaultApiKey(model))
                .dimensions(model.getDimension())
                .timeout(timeout(model));
        applyBaseUrl(model, builder::baseUrl);
        return builder.build();
    }

    private EmbeddingModel ollamaEmbeddingModel(AigcModel model) {
        OllamaEmbeddingModel.OllamaEmbeddingModelBuilder builder = OllamaEmbeddingModel.builder()
                .modelName(model.getModel())
                .timeout(timeout(model));
        applyBaseUrl(model, builder::baseUrl);
        return builder.build();
    }

    private EmbeddingModel dashScopeEmbeddingModel(AigcModel model) {
        QwenEmbeddingModel.QwenEmbeddingModelBuilder builder = QwenEmbeddingModel.builder()
                .modelName(model.getModel())
                .apiKey(defaultApiKey(model))
                .dimension(model.getDimension());
        // 同上：过滤 OpenAI 兼容地址，避免原生 SDK 404
        applyDashScopeBaseUrl(model, builder::baseUrl);
        return builder.build();
    }

    private StreamingChatModel zhipuStreamingModel(AigcModel model, AgentModelConfig config) {
        ZhipuAiStreamingChatModel.ZhipuAiStreamingChatModelBuilder builder =
                ZhipuAiStreamingChatModel.builder()
                        .model(model.getModel())
                        .apiKey(defaultApiKey(model))
                        .temperature(temperature(config, model))
                        .topP(topP(config, model))
                        .maxToken(maxTokens(config, model));
        applyBaseUrl(model, builder::baseUrl);
        return builder.build();
    }

    private EmbeddingModel zhipuEmbeddingModel(AigcModel model) {
        ZhipuAiEmbeddingModel.ZhipuAiEmbeddingModelBuilder builder = ZhipuAiEmbeddingModel.builder()
                .model(model.getModel())
                .apiKey(defaultApiKey(model));
        applyBaseUrl(model, builder::baseUrl);
        return builder.build();
    }

    private StreamingChatModel geminiStreamingModel(AigcModel model, AgentModelConfig config) {
        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(defaultApiKey(model))
                .modelName(model.getModel())
                .temperature(temperature(config, model))
                .topP(topP(config, model))
                .maxOutputTokens(maxTokens(config, model))
                .build();
    }

    private EmbeddingModel geminiEmbeddingModel(AigcModel model) {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(defaultApiKey(model))
                .modelName(model.getModel())
                .build();
    }

    private Duration timeout(AigcModel model) {
        int minutes = model.getTimeout() == null || model.getTimeout() <= 0 ? 3 : model.getTimeout();
        return Duration.ofMinutes(minutes);
    }

    private Integer maxTokens(AgentModelConfig config, AigcModel model) {
        return firstNonNull(config.maxOutputTokens(), model.getMaxToken());
    }

    private Double temperature(AgentModelConfig config, AigcModel model) {
        return firstNonNull(config.temperature(), model.getTemperature(), 0.7D);
    }

    private Double topP(AgentModelConfig config, AigcModel model) {
        return firstNonNull(config.topP(), model.getTopP(), 0.9D);
    }

    private AgentModelConfig parseAgentModelConfig(AigcAgent agent) {
        if (agent == null || StrUtil.isBlank(agent.getModelConfigJson())) {
            return AgentModelConfig.empty();
        }
        try {
            return OBJECT_MAPPER.readValue(agent.getModelConfigJson(), AgentModelConfig.class);
        } catch (Exception ex) {
            log.warn("解析 Agent 模型配置失败，agentId={}", agent.getId(), ex);
            return AgentModelConfig.empty();
        }
    }

    private String defaultApiKey(AigcModel model) {
        return StrUtil.isBlank(model.getApiKey()) ? EMPTY_API_KEY : model.getApiKey();
    }

    private void applyBaseUrl(AigcModel model, java.util.function.Consumer<String> consumer) {
        consumer.accept(model.getBaseUrl());
    }
    /**
     * DashScope 原生 SDK 端点处理。
     *
     * <p>实测结论：官方 {@code dashscope.aliyuncs.com} 的地址（无论是裸域名、
     * {@code /api/v1} 还是 OpenAI 兼容地址）一旦显式传给原生 SDK，都会因为
     * SDK 自身的路径拼接规则导致 404，因此官方域名一律忽略、使用 SDK 默认端点；
     * 只有私化部署（非 aliyuncs.com 域名）才按裸域名覆盖。
     */
    private void applyDashScopeBaseUrl(AigcModel model, java.util.function.Consumer<String> consumer) {
        String baseUrl = model.getBaseUrl();
        if (StrUtil.isBlank(baseUrl) || baseUrl.contains("aliyuncs.com")) {
            return;
        }
        try {
            java.net.URI uri = java.net.URI.create(baseUrl.trim());
            if (uri.getHost() == null) {
                return;
            }
            String host = uri.getScheme()
                    + "://"
                    + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            log.info("DashScope 使用私化部署端点：{}", host);
            consumer.accept(host);
        } catch (Exception ex) {
            log.warn("忽略无法解析的 DashScope baseUrl={}，使用官方默认端点", baseUrl, ex);
        }
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Float toFloat(Double value) {
        return value == null ? null : value.floatValue();
    }

    private record AgentModelConfig(
            Double temperature,
            Double topP,
            Integer maxOutputTokens
    ) {
        private static AgentModelConfig empty() {
            return new AgentModelConfig(null, null, null);
        }
    }
}
