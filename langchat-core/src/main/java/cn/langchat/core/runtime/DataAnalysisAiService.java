package cn.langchat.core.runtime;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 智能问数 AI Service。
 */
public interface DataAnalysisAiService {

    @SystemMessage("{{systemPrompt}}")
    TokenStream analyze(
            @V("systemPrompt") String systemPrompt,
            @UserMessage String question
    );
}
