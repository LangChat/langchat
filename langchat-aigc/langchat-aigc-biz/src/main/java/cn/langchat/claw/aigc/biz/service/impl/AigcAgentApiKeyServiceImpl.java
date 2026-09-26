package cn.langchat.claw.aigc.biz.service.impl;

import cn.langchat.claw.aigc.biz.entity.AigcAgentApiKey;
import cn.langchat.claw.aigc.biz.mapper.AigcAgentApiKeyMapper;
import cn.langchat.claw.aigc.biz.service.AigcAgentApiKeyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

/**
 * Agent API Key Service 实现。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Service
public class AigcAgentApiKeyServiceImpl extends ServiceImpl<AigcAgentApiKeyMapper, AigcAgentApiKey>
        implements AigcAgentApiKeyService {

    private static final char[] KEY_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public AigcAgentApiKey getByApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        return lambdaQuery().eq(AigcAgentApiKey::getApiKey, apiKey).one();
    }

    @Override
    public String generateApiKey() {
        StringBuilder builder = new StringBuilder("sk-langchat-");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 40; i++) {
            builder.append(KEY_CHARS[random.nextInt(KEY_CHARS.length)]);
        }
        return builder.toString();
    }

    @Override
    public void recordCall(String apiKeyId, Long inputTokens, Long outputTokens) {
        AigcAgentApiKey key = getById(apiKeyId);
        if (key == null) {
            return;
        }
        AigcAgentApiKey update = new AigcAgentApiKey();
        update.setId(apiKeyId);
        update.setCallCount((key.getCallCount() == null ? 0 : key.getCallCount()) + 1);
        update.setInputTokens(
                (key.getInputTokens() == null ? 0L : key.getInputTokens()) + (inputTokens == null ? 0L : inputTokens));
        update.setOutputTokens(
                (key.getOutputTokens() == null ? 0L : key.getOutputTokens())
                        + (outputTokens == null ? 0L : outputTokens));
        update.setLastCallTime(System.currentTimeMillis());
        updateById(update);
    }
}
