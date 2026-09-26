package cn.langchat.monitor.listener;

import cn.langchat.monitor.model.ModelCallMetadata;
import cn.langchat.monitor.service.ModelCallRecorder;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.embedding.listener.EmbeddingModelListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 模型调用监听器工厂。
 *
 * <p>监听器需要与具体模型配置绑定才能记录准确的模型维度数据，因此这里按需创建
 * 绑定好元信息的监听器实例，由模型工厂在构建模型客户端时注入。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Component
@RequiredArgsConstructor
public class ModelCallListenerFactory {

    private final ModelCallRecorder modelCallRecorder;

    /**
     * 创建对话模型监听器。
     */
    public ChatModelListener createChatListener(ModelCallMetadata metadata) {
        return new ModelCallChatListener(modelCallRecorder, metadata);
    }

    /**
     * 创建向量模型监听器。
     */
    public EmbeddingModelListener createEmbeddingListener(ModelCallMetadata metadata) {
        return new ModelCallEmbeddingListener(modelCallRecorder, metadata);
    }
}
