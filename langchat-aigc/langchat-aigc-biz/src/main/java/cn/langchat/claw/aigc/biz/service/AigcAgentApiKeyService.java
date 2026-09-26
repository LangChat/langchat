package cn.langchat.claw.aigc.biz.service;

import cn.langchat.claw.aigc.biz.entity.AigcAgentApiKey;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * Agent API Key Service。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
public interface AigcAgentApiKeyService extends IService<AigcAgentApiKey> {

    /**
     * 根据 API Key 值查询有效密钥。
     */
    AigcAgentApiKey getByApiKey(String apiKey);

    /**
     * 生成新的 API Key 值。
     */
    String generateApiKey();

    /**
     * 累加密钥调用统计。
     */
    void recordCall(String apiKeyId, Long inputTokens, Long outputTokens);
}
