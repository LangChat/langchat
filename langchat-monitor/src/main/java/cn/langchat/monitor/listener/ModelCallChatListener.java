package cn.langchat.monitor.listener;

import cn.langchat.monitor.model.ModelCallMetadata;
import cn.langchat.monitor.model.ModelCallRecord;
import cn.langchat.monitor.model.ModelCallType;
import cn.langchat.monitor.service.ModelCallRecorder;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import java.util.Map;

/**
 * 对话模型调用监听器。
 *
 * <p>基于 langchain4j 官方的 {@link ChatModelListener} 机制采集调用次数、Token 消耗、
 * 耗时与错误信息。实例在模型工厂构建模型客户端时创建，与具体模型配置一对一绑定，
 * 因此无需额外上下文即可知道"是谁被调用了"。</p>
 *
 * <p>耗时统计依赖 langchain4j 在同一次调用的 request/response 回调间共享的
 * attributes Map（由 {@code StreamingChatModel} 每次调用新建），因此本监听器可安全地
 * 被同一模型的并发调用共用。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
public class ModelCallChatListener implements ChatModelListener {

    /** attributes 中记录请求起始时间的键。 */
    private static final String START_TIME_KEY = "langchat.monitor.startTime";

    private final ModelCallRecorder modelCallRecorder;
    private final ModelCallMetadata metadata;

    public ModelCallChatListener(ModelCallRecorder modelCallRecorder, ModelCallMetadata metadata) {
        this.modelCallRecorder = modelCallRecorder;
        this.metadata = metadata;
    }

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        requestContext.attributes().put(START_TIME_KEY, System.currentTimeMillis());
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        ChatResponse response = responseContext.chatResponse();
        TokenUsage usage = response == null ? null : response.tokenUsage();
        modelCallRecorder.record(ModelCallRecord.builder()
                .metadata(metadata)
                .callType(ModelCallType.CHAT)
                .success(true)
                .inputToken(usage == null ? null : usage.inputTokenCount())
                .outputToken(usage == null ? null : usage.outputTokenCount())
                .totalToken(usage == null ? null : usage.totalTokenCount())
                .duration(resolveDuration(responseContext.attributes()))
                .build());
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        Throwable error = errorContext.error();
        modelCallRecorder.record(ModelCallRecord.builder()
                .metadata(metadata)
                .callType(ModelCallType.CHAT)
                .success(false)
                .duration(resolveDuration(errorContext.attributes()))
                .errorMessage(error == null ? null : error.getMessage())
                .build());
    }

    private Long resolveDuration(Map<Object, Object> attributes) {
        Object start = attributes.remove(START_TIME_KEY);
        if (start instanceof Long startTime) {
            return Math.max(0L, System.currentTimeMillis() - startTime);
        }
        return null;
    }
}
