package cn.langchat.monitor.listener;

import cn.langchat.monitor.model.ModelCallMetadata;
import cn.langchat.monitor.model.ModelCallRecord;
import cn.langchat.monitor.model.ModelCallType;
import cn.langchat.monitor.service.ModelCallRecorder;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.listener.EmbeddingModelErrorContext;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import dev.langchain4j.model.embedding.listener.EmbeddingModelRequestContext;
import dev.langchain4j.model.embedding.listener.EmbeddingModelResponseContext;
import dev.langchain4j.model.output.Response;
import java.util.List;
import java.util.Map;

/**
 * 向量模型调用监听器。
 *
 * <p>采集向量化调用次数、Token 消耗与耗时。实例由模型工厂在构建时创建并与模型配置绑定；
 * 由于部分供应商（DashScope、智谱）的构建器不直接支持监听器，工厂统一通过
 * {@code EmbeddingModel.addListener(...)} 包装注入，从而对所有供应商生效。</p>
 *
 * <p>条目数取本次请求的文本分段数量，便于观察批量向量化的规模。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
public class ModelCallEmbeddingListener implements EmbeddingModelListener {

    /** attributes 中记录请求起始时间的键。 */
    private static final String START_TIME_KEY = "langchat.monitor.startTime";

    private final ModelCallRecorder modelCallRecorder;
    private final ModelCallMetadata metadata;

    public ModelCallEmbeddingListener(ModelCallRecorder modelCallRecorder, ModelCallMetadata metadata) {
        this.modelCallRecorder = modelCallRecorder;
        this.metadata = metadata;
    }

    @Override
    public void onRequest(EmbeddingModelRequestContext requestContext) {
        requestContext.attributes().put(START_TIME_KEY, System.currentTimeMillis());
    }

    @Override
    public void onResponse(EmbeddingModelResponseContext responseContext) {
        Response<List<Embedding>> response = responseContext.response();
        modelCallRecorder.record(ModelCallRecord.builder()
                .metadata(metadata)
                .callType(ModelCallType.EMBEDDING)
                .success(true)
                .inputToken(resolveInputToken(response))
                .totalToken(resolveInputToken(response))
                .duration(resolveDuration(responseContext.attributes()))
                .itemCount(resolveItemCount(responseContext))
                .build());
    }

    @Override
    public void onError(EmbeddingModelErrorContext errorContext) {
        Throwable error = errorContext.error();
        modelCallRecorder.record(ModelCallRecord.builder()
                .metadata(metadata)
                .callType(ModelCallType.EMBEDDING)
                .success(false)
                .duration(resolveDuration(errorContext.attributes()))
                .errorMessage(error == null ? null : error.getMessage())
                .build());
    }

    private Integer resolveInputToken(Response<List<Embedding>> response) {
        if (response == null || response.tokenUsage() == null) {
            return null;
        }
        return response.tokenUsage().inputTokenCount();
    }

    private Integer resolveItemCount(EmbeddingModelResponseContext responseContext) {
        List<?> segments = responseContext.textSegments();
        if (segments != null && !segments.isEmpty()) {
            return segments.size();
        }
        Response<List<Embedding>> response = responseContext.response();
        if (response != null && response.content() != null) {
            return response.content().size();
        }
        return null;
    }

    private Long resolveDuration(Map<Object, Object> attributes) {
        Object start = attributes.remove(START_TIME_KEY);
        if (start instanceof Long startTime) {
            return Math.max(0L, System.currentTimeMillis() - startTime);
        }
        return null;
    }
}
