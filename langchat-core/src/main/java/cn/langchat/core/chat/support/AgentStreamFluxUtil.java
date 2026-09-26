package cn.langchat.core.chat.support;

import cn.langchat.core.chat.model.protocol.OpenAiChatCompletionChunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

/**
 * Agent 流式事件工具类。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public final class AgentStreamFluxUtil {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(3);

    private AgentStreamFluxUtil() {
    }

    /**
     * 创建标准化的 SSE 事件流。
     */
    public static Flux<ServerSentEvent<String>> createEventStream(
            Consumer<FluxSink<OpenAiChatCompletionChunk>> producer,
            AgentChatEventAssembler assembler,
            ObjectMapper objectMapper
    ) {
        return Flux.<OpenAiChatCompletionChunk>create(producer, FluxSink.OverflowStrategy.BUFFER)
                .timeout(DEFAULT_TIMEOUT)
                .onErrorResume(TimeoutException.class, ex -> Flux.just(assembler.timeout()))
                .onErrorResume(ex -> Flux.just(assembler.error(ex)))
                .map(chunk -> toServerSentEvent(chunk, objectMapper))
                .concatWith(Mono.just(ServerSentEvent.<String>builder()
                        .data(AgentChatEventAssembler.DONE_MARKER)
                        .build()));
    }

    private static ServerSentEvent<String> toServerSentEvent(OpenAiChatCompletionChunk chunk, ObjectMapper objectMapper) {
        return ServerSentEvent.<String>builder()
                .data(writeJson(chunk, objectMapper))
                .build();
    }

    private static String writeJson(OpenAiChatCompletionChunk chunk, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(chunk);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("serialize stream chunk failed", ex);
        }
    }
}
