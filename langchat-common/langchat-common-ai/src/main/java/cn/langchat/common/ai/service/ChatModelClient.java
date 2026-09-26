package cn.langchat.common.ai.service;

import cn.langchat.common.ai.model.AiChatRequest;
import cn.langchat.common.ai.model.AiChatResponse;

/**
 * 聊天模型客户端接口。
 *
 * @author LangChat Team
 * @since 2026/3/24
 */
public interface ChatModelClient {

    /**
     * 发起聊天请求。
     */
    AiChatResponse chat(AiChatRequest request);
}
