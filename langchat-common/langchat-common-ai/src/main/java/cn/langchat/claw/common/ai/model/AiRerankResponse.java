package cn.langchat.claw.common.ai.model;

import java.util.List;

/**
 * 重排响应对象。
 *
 * @param indices 排序后的索引列表
 * @param scores 排序后的分数列表
 * @param providerType 厂商类型
 * @author LangChat Team
 * @since 2026/3/24
 */
public record AiRerankResponse(
        List<Integer> indices,
        List<Double> scores,
        String providerType
) {
}
