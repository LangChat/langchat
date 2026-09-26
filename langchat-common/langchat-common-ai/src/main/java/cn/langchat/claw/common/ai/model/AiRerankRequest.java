package cn.langchat.claw.common.ai.model;

import java.util.List;

/**
 * 重排请求对象。
 *
 * @param modelId 模型ID
 * @param query 查询文本
 * @param documents 候选文档
 * @author LangChat Team
 * @since 2026/3/24
 */
public record AiRerankRequest(
        String modelId,
        String query,
        List<String> documents
) {
}
