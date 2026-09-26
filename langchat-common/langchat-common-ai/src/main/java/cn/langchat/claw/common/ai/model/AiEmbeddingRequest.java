package cn.langchat.claw.common.ai.model;

import java.util.List;

/**
 * 向量化请求对象。
 *
 * @param modelId 模型ID
 * @param texts 待向量化文本列表
 * @author LangChat Team
 * @since 2026/3/24
 */
public record AiEmbeddingRequest(
        String modelId,
        List<String> texts
) {
}
