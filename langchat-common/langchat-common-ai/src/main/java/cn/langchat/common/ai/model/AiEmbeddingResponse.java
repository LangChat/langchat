package cn.langchat.common.ai.model;

import java.util.List;

/**
 * 向量化响应对象。
 *
 * @param dimension 向量维度
 * @param vectors 向量结果
 * @param providerType 厂商类型
 * @author LangChat Team
 * @since 2026/3/24
 */
public record AiEmbeddingResponse(
        Integer dimension,
        List<List<Double>> vectors,
        String providerType
) {
}
