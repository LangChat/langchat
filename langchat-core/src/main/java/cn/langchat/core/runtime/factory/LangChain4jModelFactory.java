package cn.langchat.core.runtime.factory;

import cn.hutool.core.util.StrUtil;
import cn.langchat.aigc.biz.entity.AigcAgent;
import cn.langchat.aigc.biz.entity.AigcModel;
import cn.langchat.common.ai.enums.AiProviderType;
import cn.langchat.common.exception.BizException;
import cn.langchat.core.support.CoreErrorCode;
import cn.langchat.monitor.listener.ModelCallListenerFactory;
import cn.langchat.monitor.model.ModelCallMetadata;
import cn.langchat.monitor.model.ModelCallScene;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LangChain4j 模型工厂。
 *
 * <p>构建模型客户端时统一注入模型调用监听器（{@code langchat-monitor} 模块提供），
 * 使每一次模型调用都能被监控模块采集。向量模型部分供应商的构建器不支持监听器，
 * 统一通过 {@code EmbeddingModel.addListener(...)} 包装注入。</p>
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LangChain4jModelFactory {

    private static final String EMPTY_API_KEY = "EMPTY_API_KEY";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ModelCallListenerFactory modelCallListenerFactory;

    private final ConcurrentMap<String, StreamingChatModel> chatModelCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, EmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();

    /**
     * 构建流式聊天模型。
     *
     * @param model 模型配置
     * @param agent Agent 配置，可为空
     * @param scene 调用场景，用于监控报表按业务链路区分消耗
     */
    public StreamingChatModel getStreamingChatModel(AigcModel model, AigcAgent agent, String scene) {
        AgentModelConfig config = parseAgentModelConfig(agent);
        String cacheKey = String.join(
                ":",
                model.getId(),
                String.valueOf(model.getUpdateTime()),
                String.valueOf(temperature(config, model)),
                String.valueOf(topP(config, model)),
                String.valueOf(maxTokens(config, model)),
                String.valueOf(scene)
        );
        return chatModelCache.computeIfAbsent(cacheKey, key -> createStreamingChatModel(model, config, scene));
    }

    /**
     * 构建流式聊天模型（默认 Agent 对话场景）。
     */
    public StreamingChatModel getStreamingChatModel(AigcModel model, AigcAgent agent) {
        return getStreamingChatModel(model, agent, ModelCallScene.AGENT_CHAT);
    }

    /**
     * 构建向量模型。
     *
     * @param model 模型配置
     * @param scene 调用场景，用于监控报表按业务链路区分消耗
     */
    public EmbeddingModel getEmbeddingModel(AigcModel model, String scene) {
        String cacheKey = model.getId() + ":" + model.getUpdateTime() + ":" + scene;
        return embeddingModelCache.computeIfAbsent(cacheKey, key -> createEmbeddingModel(model, scene));
    }

    /**
     * 构建向量模型（默认知识库向量化场景）。
     */
    public EmbeddingModel getEmbeddingModel(AigcModel model) {
        return getEmbeddingModel(model, ModelCallScene.KNOWLEDGE_INDEX);
    }

    private StreamingChatModel createStreamingChatModel(AigcModel model, AgentModelConfig config, String scene) {
        AiProviderType provider = AiProviderType.fromCode(model.getProvider());
        log.info("构建流式聊天模型，modelId={}, provider={}, model={}, scene={}",
                model.getId(), provider, model.getModel(), scene);
        List<ChatModelListener> listeners = List.of(modelCallListenerFactory.createChatListener(metadataOf(model, scene)));
        return switch (provider) {
            case OPENAI, DEEPSEEK, OPENAI_COMPATIBLE -> openAiCompatibleStreamingModel(model, config, listeners);
            case OLLAMA -> ollamaStreamingModel(model, config, listeners);
            case DASHSCOPE -> dashScopeStreamingModel(model, config, listeners);
            case ZHIPU -> zhipuStreamingModel(model, config, listeners);
            case GEMINI -> geminiStreamingModel(model, config, listeners);
            default -> throw new BizException(CoreErrorCode.UNSUPPORTED_PROVIDER);
        };
    }

    private EmbeddingModel createEmbeddingModel(AigcModel model, String scene) {
        AiProviderType provider = AiProviderType.fromCode(model.getProvider());
        log.info("构建向量模型，modelId={}, provider={}, model={}, scene={}",
                model.getId(), provider, model.getModel(), scene);
        EmbeddingModelListener listener = modelCallListenerFactory.createEmbeddingListener(metadataOf(model, scene));
        EmbeddingModel embeddingModel = switch (provider) {
            case OPENAI, DEEPSEEK, OPENAI_COMPATIBLE -> openAiCompatibleEmbeddingModel(model, List.of(listener));
            case OLLAMA -> ollamaEmbeddingModel(model, List.of(listener));
            case DASHSCOPE -> dashScopeEmbeddingModel(model);
            case ZHIPU -> zhipuEmbeddingModel(model);
            case GEMINI -> geminiEmbeddingModel(model, List.of(listener));
            default -> throw new BizException(CoreErrorCode.UNSUPPORTED_PROVIDER);
        };
        // DashScope / 智谱 的构建器不提供 listeners(...) 方法，用官方包装器补齐；
        // addListener 对已注册该监听器的模型会识别并叠加，不会重复触发。
        return embeddingModel.addListener(listener);
    }

    private ModelCallMetadata metadataOf(AigcModel model, String scene) {
        return ModelCallMetadata.of(model.getId(), model.getModel(), model.getProvider(), scene);
    }

    private StreamingChatModel openAiCompatibleStreamingModel(
            AigcModel model, AgentModelConfig config, List<ChatModelListener> listeners) {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .modelName(model.getModel())
                .apiKey(defaultApiKey(model))
                .temperature(temperature(config, model))
                .topP(topP(config, model))
                .maxTokens(maxTokens(config, model))
                .timeout(timeout(model))
                .listeners(listeners);
        applyBaseUrl(model, builder::baseUrl);
        return builder.build();
    }

    private StreamingChatModel ollamaStreamingModel(
            AigcModel model, AgentModelConfig config, List<ChatModelListener> listeners) {
        OllamaStreamingChatModel.OllamaStreamingChatModelBuilder builder = OllamaStreamingChatModel.builder()
                .modelName(model.getModel())
                .temperature(temperature(config, model))
                .topP(topP(config, model))
                .numPredict(maxTokens(config, model))
                .timeout(timeout(model))
                .listeners(listeners);
        applyBaseUrl(model, builder::baseUrl);
        return builder.build();
    }

    private StreamingChatModel dashScopeStreamingModel(
            AigcModel model, AgentModelConfig config, List<ChatModelListener> listeners) {
        QwenStreamingChatModel.QwenStreamingChatModelBuilder builder = QwenStreamingChatModel.builder()
                .modelName(model.getModel())
                .apiKey(defaultApiKey(model))
                .temperature(toFloat(temperature(config, model)))
                .topP(topP(config, model))
                .maxTokens(maxTokens(config, model))
                .listeners(listeners);
        // DashScope 原生 SDK 自带默认端点，不能把 OpenAI 兼容地址（compatible-mode）传给它
        applyDashScopeBaseUrl(model, builder::baseUrl);
        return builder.build();
    }

    private EmbeddingModel openAiCompatibleEmbeddingModel(AigcModel model, List<EmbeddingModelListener> listeners) {
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
                .modelName(model.getModel())
                .apiKey(defaultApiKey(model))
                .dimensions(model.getDimension())
                .timeout(timeout(model))
                .listeners(listeners);
        applyBaseUrl(model, builder::baseUrl);
        return builder.build();
    }

    private EmbeddingModel ollamaEmbeddingModel(AigcModel model, List<EmbeddingModelListener> listeners) {
        OllamaEmbeddingModel.OllamaEmbeddingModelBuilder builder = OllamaEmbeddingModel.builder()
                .modelName(model.getModel())
                .timeout(timeout(model))
                .listeners(listeners);
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

    private StreamingChatModel zhipuStreamingModel(
            AigcModel model, AgentModelConfig config, List<ChatModelListener> listeners) {
        ZhipuAiStreamingChatModel.ZhipuAiStreamingChatModelBuilder builder =
                ZhipuAiStreamingChatModel.builder()
                        .model(model.getModel())
                        .apiKey(defaultApiKey(model))
                        .temperature(temperature(config, model))
                        .topP(topP(config, model))
                        .maxToken(maxTokens(config, model))
                        .listeners(listeners);
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

    private StreamingChatModel geminiStreamingModel(
            AigcModel model, AgentModelConfig config, List<ChatModelListener> listeners) {
        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(defaultApiKey(model))
                .modelName(model.getModel())
                .temperature(temperature(config, model))
                .topP(topP(config, model))
                .maxOutputTokens(maxTokens(config, model))
                .listeners(listeners)
                .build();
    }

    private EmbeddingModel geminiEmbeddingModel(AigcModel model, List<EmbeddingModelListener> listeners) {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(defaultApiKey(model))
                .modelName(model.getModel())
                .listeners(listeners)
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
