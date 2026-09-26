package cn.langchat.claw.common.ai.service;

import cn.langchat.claw.common.ai.model.AiEmbeddingRequest;
import cn.langchat.claw.common.ai.model.AiEmbeddingResponse;

/**
 * 向量模型客户端接口。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface EmbeddingModelClient {

    /**
     * 执行向量化。
     */
    AiEmbeddingResponse embed(AiEmbeddingRequest request);
}
