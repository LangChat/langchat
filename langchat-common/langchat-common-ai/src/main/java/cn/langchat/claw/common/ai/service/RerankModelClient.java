package cn.langchat.claw.common.ai.service;

import cn.langchat.claw.common.ai.model.AiRerankRequest;
import cn.langchat.claw.common.ai.model.AiRerankResponse;

/**
 * 重排模型客户端接口。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface RerankModelClient {

    /**
     * 执行文档重排。
     */
    AiRerankResponse rerank(AiRerankRequest request);
}
