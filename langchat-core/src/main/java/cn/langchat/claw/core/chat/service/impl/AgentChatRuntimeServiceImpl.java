package cn.langchat.claw.core.chat.service.impl;

import cn.langchat.claw.aigc.biz.entity.AigcAgent;
import cn.langchat.claw.aigc.biz.entity.AigcConversation;
import cn.langchat.claw.aigc.biz.entity.AigcKnowledge;
import cn.langchat.claw.aigc.biz.entity.AigcMessage;
import cn.langchat.claw.aigc.biz.entity.AigcMessageEvent;
import cn.langchat.claw.aigc.biz.entity.AigcModel;
import cn.langchat.claw.aigc.biz.entity.AigcVectorStore;
import cn.langchat.claw.aigc.biz.service.AigcConversationService;
import cn.langchat.claw.aigc.biz.service.AigcMessageService;
import cn.langchat.claw.aigc.biz.service.AigcMessageEventService;
import cn.langchat.claw.aigc.biz.service.AigcModelService;
import cn.langchat.claw.aigc.biz.service.AigcVectorStoreService;
import cn.langchat.claw.common.auth.AuthUtil;
import cn.langchat.claw.common.exception.BizException;
import cn.langchat.claw.core.chat.enums.ChatMessageTypeEnum;
import cn.langchat.claw.core.chat.enums.ChatRoleEnum;
import cn.langchat.claw.core.chat.model.knowledge.KnowledgeSearchHit;
import cn.langchat.claw.core.chat.model.protocol.OpenAiChatCompletionChunk;
import cn.langchat.claw.core.chat.model.request.AgentChatStreamRequest;
import cn.langchat.claw.core.chat.service.AgentChatRuntimeService;
import cn.langchat.claw.core.chat.support.AgentChatEventAssembler;
import cn.langchat.claw.core.chat.support.AgentStreamFluxUtil;
import cn.langchat.claw.core.runtime.AgentChatAiService;
import cn.langchat.claw.core.runtime.AgentRuntimeLoader;
import cn.langchat.claw.core.runtime.factory.LangChain4jModelFactory;
import cn.langchat.claw.core.runtime.factory.VectorStoreFactory;
import cn.langchat.claw.core.runtime.model.AgentRuntimeDefinition;
import cn.langchat.claw.core.runtime.mcp.McpClientManager;
import cn.langchat.claw.core.runtime.rag.CompositeContentRetriever;
import cn.langchat.claw.core.runtime.rag.KeywordSegmentContentRetriever;
import cn.langchat.claw.core.runtime.rag.KnowledgeSearchService;
import cn.langchat.claw.core.runtime.skill.AgentToolContext;
import cn.langchat.claw.core.runtime.skill.AgentToolRegistry;
import cn.langchat.claw.core.support.CoreErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Agent 对话运行时服务实现。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentChatRuntimeServiceImpl implements AgentChatRuntimeService {

    /**
     * 智能体未配置系统提示词时的兜底提示词，避免空模板导致 langchain4j 抛出
     * "text cannot be null or blank"。
     */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是 LangChat 智能助手，请专业、准确、简洁地回答用户问题。";

    private final AgentRuntimeLoader agentRuntimeLoader;
    private final LangChain4jModelFactory langChain4jModelFactory;
    private final VectorStoreFactory vectorStoreFactory;
    private final KnowledgeSearchService knowledgeSearchService;
    private final AgentToolRegistry agentToolRegistry;
    private final McpClientManager mcpClientManager;
    private final AigcConversationService aigcConversationService;
    private final AigcMessageService aigcMessageService;
    private final AigcMessageEventService aigcMessageEventService;
    private final AigcVectorStoreService aigcVectorStoreService;
    private final AigcModelService aigcModelService;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<ServerSentEvent<String>> streamChat(String agentId, AgentChatStreamRequest request) {
        validateRequest(request);
        AgentRuntimeDefinition runtime = agentRuntimeLoader.load(agentId);
        String conversationId = ensureConversation(runtime.agent(), request.getConversationId(), request.getMessage());
        String completionId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "");
        long created = Instant.now().getEpochSecond();
        AgentChatEventAssembler assembler = new AgentChatEventAssembler(
                conversationId,
                completionId,
                created,
                runtime.chatModelConfig().getModel()
        );
        return AgentStreamFluxUtil.createEventStream(
                sink -> doStreamChat(agentId, request, runtime, assembler, sink),
                assembler,
                objectMapper
        );
    }

    private void doStreamChat(
            String agentId,
            AgentChatStreamRequest request,
            AgentRuntimeDefinition runtime,
            AgentChatEventAssembler assembler,
            FluxSink<OpenAiChatCompletionChunk> sink
    ) {
        long startAt = System.currentTimeMillis();
        String userId = AuthUtil.getUserId();
        String conversationId = assembler.conversationId();
        AtomicReference<String> responseText = new AtomicReference<>("");
        AtomicBoolean completed = new AtomicBoolean(false);
        List<OpenAiChatCompletionChunk> traceEvents = new ArrayList<>();

        try {
            List<AigcKnowledge> boundKnowledges = runtime.knowledges();
            StreamingChatModel chatModel = langChain4jModelFactory.getStreamingChatModel(runtime.chatModelConfig(), runtime.agent());
            ContentRetriever contentRetriever = buildContentRetriever(runtime, boundKnowledges);
            Map<ToolSpecification, ToolExecutor> tools = agentToolRegistry.buildTools(
                    runtime.skills(),
                    new AgentToolContext(runtime.agent(), conversationId, userId, null, boundKnowledges)
            );
            ToolProvider mcpToolProvider = mcpClientManager.buildToolProvider(runtime.mcps());
            AgentChatAiService aiService = buildAiService(
                    runtime.agent(), chatModel, contentRetriever, tools, mcpToolProvider);
            saveUserMessage(conversationId, agentId, runtime.chatModelConfig(), request.getMessage(), assembler.completionId());
            emitTraceEvent(assembler.logDelta(
                    "已接收用户消息",
                    "request.accepted",
                    "running",
                    Map.of(
                            "agentId", agentId,
                            "conversation_id", conversationId
                    )
            ), traceEvents, sink);

            emitTraceEvent(assembler.messageStart(), traceEvents, sink);
            emitTraceEvent(assembler.logDelta(
                    "模型开始生成",
                    "model.generating",
                    "running",
                    Map.of("model", defaultText(runtime.chatModelConfig().getModel()))
            ), traceEvents, sink);
            TokenStream tokenStream = aiService.chat(
                    conversationId,
                    applyVariables(resolveSystemPrompt(runtime.agent().getSystemPrompt()), request.getVariables()),
                    request.getMessage()
            );
            tokenStream
                    .onPartialResponse(partial -> onPartialResponse(partial, assembler, responseText, traceEvents, sink))
                    .onRetrieved(contents -> onRetrieved(contents, assembler, traceEvents, sink))
                    .beforeToolExecution(before -> {
                        emitTraceEvent(assembler.logDelta(
                                "开始执行工具调用",
                                "tool.before",
                                "running",
                                Map.of(
                                        "request_id", defaultText(before.request().id()),
                                        "tool_name", defaultText(before.request().name())
                                )
                        ), traceEvents, sink);
                        emitTraceEvent(assembler.beforeTool(before), traceEvents, sink);
                    })
                    .onToolExecuted(execution -> {
                        emitTraceEvent(assembler.toolExecuted(execution), traceEvents, sink);
                        emitTraceEvent(assembler.logDelta(
                                execution.hasFailed() ? "工具调用失败" : "工具调用完成",
                                "tool.executed",
                                execution.hasFailed() ? "failed" : "completed",
                                Map.of(
                                        "request_id", defaultText(execution.request().id()),
                                        "tool_name", defaultText(execution.request().name())
                                )
                        ), traceEvents, sink);
                    })
                    .onCompleteResponse(response -> onCompleteResponse(
                            response,
                            agentId,
                            runtime.chatModelConfig(),
                            assembler,
                            responseText.get(),
                            traceEvents,
                            startAt,
                            sink,
                            completed
                    ))
                    .onError(error -> {
                        if (completed.compareAndSet(false, true)) {
                            log.error("Agent 流式对话失败，agentId={}, conversationId={}", agentId, conversationId, error);
                            emitTraceEvent(assembler.error(friendlyModelError(error)), traceEvents, sink);
                            persistTraceEvents(conversationId, agentId, assembler.completionId(), null, traceEvents);
                            sink.complete();
                        }
                    })
                    .start();
        } catch (Exception ex) {
            log.error("构建 Agent 运行时失败，agentId={}", agentId, ex);
            emitTraceEvent(assembler.error(ex), traceEvents, sink);
            persistTraceEvents(conversationId, agentId, assembler.completionId(), null, traceEvents);
            sink.complete();
        }
    }

    private AgentChatAiService buildAiService(
            AigcAgent agent,
            StreamingChatModel chatModel,
            ContentRetriever contentRetriever,
            Map<ToolSpecification, ToolExecutor> tools,
            ToolProvider mcpToolProvider
    ) {
        AiServices<AgentChatAiService> builder = AiServices.builder(AgentChatAiService.class)
                .streamingChatModel(chatModel)
                .chatMemoryProvider(memoryId -> buildChatMemory(String.valueOf(memoryId)));
        if (contentRetriever != null) {
            builder.contentRetriever(contentRetriever);
        }
        if (!tools.isEmpty()) {
            builder.tools(tools);
        }
        if (mcpToolProvider != null) {
            builder.toolProvider(mcpToolProvider);
        }
        log.info("构建 AiService 完成，toolSize={}, mcpEnabled={}", tools.size(), mcpToolProvider != null);
        return builder.build();
    }

    private ChatMemory buildChatMemory(String conversationId) {
        int maxMessages = 20;
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(conversationId)
                .maxMessages(maxMessages)
                .build();
        List<AigcMessage> messages = aigcMessageService.lambdaQuery()
                .eq(AigcMessage::getConversationId, conversationId)
                .orderByAsc(AigcMessage::getCreateTime)
                .list();
        for (AigcMessage message : messages) {
            ChatRoleEnum role = ChatRoleEnum.fromCode(message.getRole());
            switch (role) {
                case ASSISTANT -> chatMemory.add(AiMessage.from(defaultText(message.getMessage())));
                case SYSTEM -> chatMemory.add(SystemMessage.from(defaultText(message.getMessage())));
                case USER -> chatMemory.add(UserMessage.from(defaultText(message.getMessage())));
            }
        }
        return chatMemory;
    }

    private ContentRetriever buildContentRetriever(AgentRuntimeDefinition runtime, List<AigcKnowledge> boundKnowledges) {
        List<ContentRetriever> retrievers = new ArrayList<>();
        if (!boundKnowledges.isEmpty()) {
            retrievers.add(new KeywordSegmentContentRetriever(knowledgeSearchService, boundKnowledges, 5));
        }
        for (AigcKnowledge knowledge : boundKnowledges) {
            if (knowledge.getVectorStoreId() == null || knowledge.getVectorStoreId().isBlank()) {
                continue;
            }
            AigcVectorStore vectorStore = aigcVectorStoreService.getById(knowledge.getVectorStoreId());
            if (vectorStore == null) {
                continue;
            }
            EmbeddingStore<dev.langchain4j.data.segment.TextSegment> embeddingStore = vectorStoreFactory.getStore(vectorStore);
            AigcModel embeddingModelConfig = knowledge.getVectorModelId() != null && !knowledge.getVectorModelId().isBlank()
                    ? aigcModelService.getById(knowledge.getVectorModelId())
                    : null;
            if (embeddingModelConfig == null) {
                continue;
            }
            EmbeddingModel embeddingModel = langChain4jModelFactory.getEmbeddingModel(embeddingModelConfig);
            retrievers.add(EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .maxResults(valueOrDefault(knowledge.getMaxResults(), 5))
                    .minScore(valueOrDefault(knowledge.getMinScore(), 0.6D))
                    .displayName(knowledge.getName())
                    .build());
        }
        if (retrievers.isEmpty()) {
            return null;
        }
        return new CompositeContentRetriever(retrievers, 12);
    }

    private void onPartialResponse(
            String partial,
            AgentChatEventAssembler assembler,
            AtomicReference<String> responseText,
            List<OpenAiChatCompletionChunk> traceEvents,
            FluxSink<OpenAiChatCompletionChunk> sink
    ) {
        if (partial == null || partial.isBlank()) {
            return;
        }
        responseText.updateAndGet(text -> text + partial);
        emitTraceEvent(assembler.messageDelta(partial), traceEvents, sink);
    }

    private void onRetrieved(
            List<Content> contents,
            AgentChatEventAssembler assembler,
            List<OpenAiChatCompletionChunk> traceEvents,
            FluxSink<OpenAiChatCompletionChunk> sink
    ) {
        emitTraceEvent(assembler.retrieved(toKnowledgeHits(contents)), traceEvents, sink);
    }

    private void onCompleteResponse(
            ChatResponse response,
            String agentId,
            AigcModel model,
            AgentChatEventAssembler assembler,
            String content,
            List<OpenAiChatCompletionChunk> traceEvents,
            long startAt,
            FluxSink<OpenAiChatCompletionChunk> sink,
            AtomicBoolean completed
    ) {
        if (!completed.compareAndSet(false, true)) {
            return;
        }
        AigcMessage assistantMessage = saveAssistantMessage(
                assembler.conversationId(),
                agentId,
                assembler.completionId(),
                model,
                content,
                response,
                traceEvents,
                startAt
        );
        emitTraceEvent(assembler.messageCompleted(assistantMessage.getId(), response), traceEvents, sink);
        emitTraceEvent(assembler.logDelta(
                "响应生成完成并已入库",
                "message.completed",
                "completed",
                Map.of(
                        "assistant_message_id", assistantMessage.getId(),
                        "finish_reason", resolveFinishReason(response)
                )
        ), traceEvents, sink);
        emitTraceEvent(assembler.messageStop(resolveFinishReason(response)), traceEvents, sink);
        persistTraceEvents(assembler.conversationId(), agentId, assembler.completionId(), assistantMessage.getId(), traceEvents);
        sink.complete();
    }

    private List<KnowledgeSearchHit> toKnowledgeHits(List<Content> contents) {
        return contents.stream().map(this::toKnowledgeHit).toList();
    }

    private KnowledgeSearchHit toKnowledgeHit(Content content) {
        Map<String, Object> metadataMap = new LinkedHashMap<>(content.textSegment().metadata().toMap());
        KnowledgeSearchHit hit = new KnowledgeSearchHit();
        hit.setKnowledgeId(stringValue(metadataMap.get("knowledgeId")));
        hit.setKnowledgeName(stringValue(metadataMap.get("knowledgeName")));
        hit.setDocsId(stringValue(metadataMap.get("docsId")));
        hit.setDocsName(stringValue(metadataMap.get("docsName")));
        hit.setSegmentId(stringValue(metadataMap.get("segmentId")));
        hit.setScore(doubleValue(metadataMap.get("score")));
        hit.setContent(defaultText(content.textSegment().text()));
        hit.setMetadata(metadataMap);
        return hit;
    }

    private void saveUserMessage(
            String conversationId,
            String agentId,
            AigcModel model,
            String content,
            String completionId
    ) {
        AigcMessage userMessage = new AigcMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setAgentId(agentId);
        userMessage.setChatId(completionId);
        userMessage.setRole(ChatRoleEnum.USER.code());
        userMessage.setModel(model.getModel());
        userMessage.setType(ChatMessageTypeEnum.TEXT.code());
        userMessage.setMessage(content);
        aigcMessageService.save(userMessage);
    }

    private AigcMessage saveAssistantMessage(
            String conversationId,
            String agentId,
            String completionId,
            AigcModel model,
            String content,
            ChatResponse response,
            List<OpenAiChatCompletionChunk> traceEvents,
            long startAt
    ) {
        AigcMessage assistantMessage = new AigcMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setAgentId(agentId);
        assistantMessage.setChatId(completionId);
        assistantMessage.setRole(ChatRoleEnum.ASSISTANT.code());
        assistantMessage.setModel(response.modelName() == null ? model.getModel() : response.modelName());
        assistantMessage.setType(ChatMessageTypeEnum.TEXT.code());
        assistantMessage.setMessage(defaultText(content));
        assistantMessage.setFinishReason(resolveFinishReason(response));
        assistantMessage.setInputToken(response.tokenUsage() == null ? 0 : response.tokenUsage().inputTokenCount());
        assistantMessage.setOutputToken(response.tokenUsage() == null ? 0 : response.tokenUsage().outputTokenCount());
        assistantMessage.setDuration((int) (System.currentTimeMillis() - startAt));
        assistantMessage.setTraceInfo(writeValue(traceEvents));
        aigcMessageService.save(assistantMessage);
        return assistantMessage;
    }

    private String ensureConversation(AigcAgent agent, String conversationId, String firstMessage) {
        if (conversationId != null && !conversationId.isBlank()) {
            AigcConversation existing = aigcConversationService.getById(conversationId);
            if (existing != null) {
                if (!agent.getId().equals(existing.getAgentId())) {
                    throw new BizException(CoreErrorCode.CONVERSATION_NOT_FOUND);
                }
                return conversationId;
            }
            AigcConversation fixedConversation = new AigcConversation();
            fixedConversation.setId(conversationId);
            fixedConversation.setAgentId(agent.getId());
            fixedConversation.setTitle("Agent 构建调试");
            aigcConversationService.save(fixedConversation);
            return conversationId;
        }
        AigcConversation conversation = new AigcConversation();
        conversation.setAgentId(agent.getId());
        conversation.setTitle(buildConversationTitle(firstMessage));
        aigcConversationService.save(conversation);
        return conversation.getId();
    }

    private void validateRequest(AgentChatStreamRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BizException(CoreErrorCode.INVALID_CHAT_REQUEST);
        }
    }

    /**
     * 将供应商返回的原始错误翻译为用户可读的提示。
     *
     * <p>例如 DashScope 对无效模型名返回含糊的 {@code url error}，
     * 直接透出会让用户误以为是网络/地址问题。
     */
    private Throwable friendlyModelError(Throwable error) {
        String message = error == null ? null : error.getMessage();
        if (message == null || message.isBlank()) {
            return error;
        }
        if (message.contains("url error") || message.contains("Model not exist")) {
            String hint = "模型调用失败：模型名称无效或当前账号未开通该模型，请在「模型管理」中确认模型名称与供应商平台一致。原始错误：";
            return new IllegalStateException(hint + message, error);
        }
        return error;
    }

    private String buildConversationTitle(String message) {
        String content = defaultText(message).trim();
        return content.length() <= 20 ? content : content.substring(0, 20);
    }

    private String applyVariables(String prompt, Map<String, Object> variables) {
        if (prompt == null || prompt.isBlank() || variables == null || variables.isEmpty()) {
            return defaultText(prompt);
        }
        String resolved = prompt;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            resolved = resolved.replace("{{" + entry.getKey() + "}}", Objects.toString(entry.getValue(), ""));
        }
        return resolved;
    }

    private String resolveSystemPrompt(String systemPrompt) {
        return systemPrompt == null || systemPrompt.isBlank() ? DEFAULT_SYSTEM_PROMPT : systemPrompt;
    }

    private void emitTraceEvent(
            OpenAiChatCompletionChunk chunk,
            List<OpenAiChatCompletionChunk> traceEvents,
            FluxSink<OpenAiChatCompletionChunk> sink
    ) {
        traceEvents.add(chunk);
        sink.next(chunk);
    }

    private void persistTraceEvents(
            String conversationId,
            String agentId,
            String chatId,
            String messageId,
            List<OpenAiChatCompletionChunk> traceEvents
    ) {
        if (traceEvents.isEmpty()) {
            return;
        }
        List<AigcMessageEvent> items = new ArrayList<>(traceEvents.size());
        for (int index = 0; index < traceEvents.size(); index++) {
            OpenAiChatCompletionChunk chunk = traceEvents.get(index);
            AigcMessageEvent item = new AigcMessageEvent();
            item.setConversationId(conversationId);
            item.setAgentId(agentId);
            item.setChatId(chatId);
            item.setMessageId(messageId);
            item.setEventIndex(index);
            item.setEventName(resolveEventName(chunk));
            item.setEventType(resolveEventType(chunk));
            item.setEventStatus(resolveEventStatus(chunk));
            item.setPayloadJson(writeValue(chunk));
            items.add(item);
        }
        aigcMessageEventService.saveBatch(items);
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BizException(CoreErrorCode.CHAT_RUNTIME_ERROR.code(), ex.getMessage());
        }
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    @SuppressWarnings("unchecked")
    private String resolveEventName(OpenAiChatCompletionChunk chunk) {
        Map<String, Object> event = resolveDeltaEvent(chunk);
        if (event != null && event.get("name") != null) {
            return String.valueOf(event.get("name"));
        }
        if (chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
            return "message.delta";
        }
        if (chunk.getChoices().get(0).getFinishReason() != null) {
            return "message.stop";
        }
        if (chunk.getChoices().get(0).getDelta() != null && chunk.getChoices().get(0).getDelta().getRole() != null) {
            return "message.start";
        }
        return "message.delta";
    }

    @SuppressWarnings("unchecked")
    private String resolveEventType(OpenAiChatCompletionChunk chunk) {
        Map<String, Object> event = resolveDeltaEvent(chunk);
        if (event != null && event.get("type") != null) {
            return String.valueOf(event.get("type"));
        }
        return "message";
    }

    @SuppressWarnings("unchecked")
    private String resolveEventStatus(OpenAiChatCompletionChunk chunk) {
        Map<String, Object> event = resolveDeltaEvent(chunk);
        if (event != null && event.get("status") != null) {
            return String.valueOf(event.get("status"));
        }
        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty() && chunk.getChoices().get(0).getFinishReason() != null) {
            return "completed";
        }
        return "running";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveDeltaEvent(OpenAiChatCompletionChunk chunk) {
        if (chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
            return null;
        }
        if (chunk.getChoices().get(0).getDelta() == null || chunk.getChoices().get(0).getDelta().getEvent() == null) {
            return null;
        }
        Object event = chunk.getChoices().get(0).getDelta().getEvent();
        if (event instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return objectMapper.convertValue(event, Map.class);
    }

    private String resolveFinishReason(ChatResponse response) {
        FinishReason finishReason = response.finishReason();
        return finishReason == null ? FinishReason.STOP.name() : finishReason.name();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private int valueOrDefault(Integer first, int fallback) {
        if (first != null) {
            return first;
        }
        return fallback;
    }

    private double valueOrDefault(Double first, double fallback) {
        if (first != null) {
            return first;
        }
        return fallback;
    }

    private double valueOrDefault(Double first, Double second, double fallback) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return fallback;
    }
}
