package cn.langchat.core.chat.support;

import cn.langchat.core.chat.enums.AgentStreamEventTypeEnum;
import cn.langchat.core.chat.enums.ChatRoleEnum;
import cn.langchat.core.chat.enums.OpenAiObjectTypeEnum;
import cn.langchat.core.chat.model.event.KnowledgeReferenceItem;
import cn.langchat.core.chat.model.knowledge.KnowledgeSearchHit;
import cn.langchat.core.chat.model.protocol.OpenAiChatCompletionChoice;
import cn.langchat.core.chat.model.protocol.OpenAiChatCompletionChunk;
import cn.langchat.core.chat.model.protocol.OpenAiChatCompletionDelta;
import cn.langchat.core.chat.model.protocol.OpenAiChatCompletionUsage;
import cn.langchat.core.support.CoreErrorCode;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 流式事件组装器（OpenAI chunk 结构）。
 *
 * @author LangChat Team
 * @since 2026/3/25
 */
public class AgentChatEventAssembler {

    public static final String DONE_MARKER = "[DONE]";

    private final String conversationId;
    private final String completionId;
    private final Long created;
    private final String model;

    public AgentChatEventAssembler(String conversationId, String completionId, Long created, String model) {
        this.conversationId = conversationId;
        this.completionId = completionId;
        this.created = created;
        this.model = model;
    }

    /**
     * 构建消息开始事件。
     */
    public OpenAiChatCompletionChunk messageStart() {
        OpenAiChatCompletionDelta delta = new OpenAiChatCompletionDelta();
        delta.setRole(ChatRoleEnum.ASSISTANT.code());
        return chunk(delta, null);
    }

    /**
     * 构建消息增量事件。
     */
    public OpenAiChatCompletionChunk messageDelta(String content) {
        OpenAiChatCompletionDelta delta = new OpenAiChatCompletionDelta();
        delta.setContent(content);
        return chunk(delta, null);
    }

    /**
     * 构建知识检索事件。
     */
    public OpenAiChatCompletionChunk retrieved(List<KnowledgeSearchHit> hits) {
        List<KnowledgeReferenceItem> items = hits.stream().map(this::toReferenceItem).toList();
        return eventDelta(
                AgentStreamEventTypeEnum.RAG_RETRIEVED,
                Map.of(
                        "items", items,
                        "status", "completed",
                        "type", "rag"
                )
        );
    }

    /**
     * 构建 Tool 调用前事件。
     */
    public OpenAiChatCompletionChunk beforeTool(BeforeToolExecution before) {
        return eventDelta(
                AgentStreamEventTypeEnum.TOOL_BEFORE,
                Map.of(
                        "arguments", defaultText(before.request().arguments()),
                        "request_id", defaultText(before.request().id()),
                        "status", "running",
                        "tool_name", defaultText(before.request().name()),
                        "type", "tool"
                )
        );
    }

    /**
     * 构建 Tool 调用完成事件。
     */
    public OpenAiChatCompletionChunk toolExecuted(ToolExecution execution) {
        return eventDelta(
                AgentStreamEventTypeEnum.TOOL_EXECUTED,
                Map.of(
                        "arguments", defaultText(execution.request().arguments()),
                        "failed", execution.hasFailed(),
                        "request_id", defaultText(execution.request().id()),
                        "result", defaultText(execution.result()),
                        "status", execution.hasFailed() ? "failed" : "completed",
                        "tool_name", defaultText(execution.request().name()),
                        "type", "tool"
                )
        );
    }

    /**
     * 构建执行日志事件。
     */
    public OpenAiChatCompletionChunk logDelta(String message, String phase, String status, Map<String, Object> detail) {
        return eventDelta(
                AgentStreamEventTypeEnum.LOG_DELTA,
                Map.of(
                        "detail", detail == null ? Map.of() : detail,
                        "message", message,
                        "phase", phase,
                        "status", status,
                        "type", "log"
                )
        );
    }

    /**
     * 构建智能问数表格事件。
     */
    public OpenAiChatCompletionChunk analysisTable(Map<String, Object> payload) {
        return analysisEvent(AgentStreamEventTypeEnum.ANALYSIS_TABLE, "analysis.table", payload);
    }

    /**
     * 构建智能问数图表事件。
     */
    public OpenAiChatCompletionChunk analysisEchart(Map<String, Object> payload) {
        return analysisEvent(AgentStreamEventTypeEnum.ANALYSIS_ECHART, "analysis.echart", payload);
    }

    /**
     * 构建消息完成事件。
     */
    public OpenAiChatCompletionChunk messageCompleted(String assistantMessageId, ChatResponse response) {
        OpenAiChatCompletionUsage usage = usage(response.tokenUsage());
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("conversation_id", conversationId);
        event.put("finish_reason", response.finishReason() == null ? null : response.finishReason().name());
        event.put("message_id", assistantMessageId);
        event.put("status", "completed");
        event.put("type", "meta");
        event.put("usage", usage);
        return eventDelta(AgentStreamEventTypeEnum.MESSAGE_COMPLETED, event);
    }

    /**
     * 构建消息停止事件。
     */
    public OpenAiChatCompletionChunk messageStop(String finishReason) {
        return chunk(new OpenAiChatCompletionDelta(), finishReason);
    }

    /**
     * 构建超时事件。
     */
    public OpenAiChatCompletionChunk timeout() {
        return eventDelta(
                AgentStreamEventTypeEnum.TIMEOUT,
                Map.of(
                        "code", CoreErrorCode.CHAT_STREAM_TIMEOUT.code(),
                        "message", CoreErrorCode.CHAT_STREAM_TIMEOUT.message(),
                        "status", "failed",
                        "type", "error"
                )
        );
    }

    /**
     * 构建异常事件。
     */
    public OpenAiChatCompletionChunk error(Throwable throwable) {
        String errorMessage = throwable == null || throwable.getMessage() == null
                ? CoreErrorCode.CHAT_RUNTIME_ERROR.message()
                : throwable.getMessage();
        return eventDelta(
                AgentStreamEventTypeEnum.ERROR,
                Map.of(
                        "code", CoreErrorCode.CHAT_RUNTIME_ERROR.code(),
                        "message", errorMessage,
                        "status", "failed",
                        "type", "error"
                )
        );
    }

    /**
     * 获取会话 ID。
     */
    public String conversationId() {
        return conversationId;
    }

    /**
     * 获取 Completion ID。
     */
    public String completionId() {
        return completionId;
    }

    private OpenAiChatCompletionChunk chunk(OpenAiChatCompletionDelta delta, String finishReason) {
        OpenAiChatCompletionChoice choice = new OpenAiChatCompletionChoice();
        choice.setIndex(0);
        choice.setDelta(delta);
        choice.setFinishReason(finishReason);

        OpenAiChatCompletionChunk chunk = new OpenAiChatCompletionChunk();
        chunk.setId(completionId);
        chunk.setObject(OpenAiObjectTypeEnum.CHAT_COMPLETION_CHUNK.code());
        chunk.setCreated(created);
        chunk.setModel(model);
        chunk.setChoices(List.of(choice));
        return chunk;
    }

    private OpenAiChatCompletionChunk eventDelta(AgentStreamEventTypeEnum eventType, Map<String, Object> eventPayload) {
        OpenAiChatCompletionDelta delta = new OpenAiChatCompletionDelta();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("name", eventType.code());
        event.putAll(eventPayload);
        delta.setEvent(event);
        return chunk(delta, null);
    }

    private OpenAiChatCompletionChunk analysisEvent(
            AgentStreamEventTypeEnum eventType,
            String type,
            Map<String, Object> payload
    ) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("status", "completed");
        event.put("type", type);
        if (payload != null) {
            event.putAll(payload);
        }
        return eventDelta(eventType, event);
    }

    private OpenAiChatCompletionUsage usage(TokenUsage tokenUsage) {
        OpenAiChatCompletionUsage usage = new OpenAiChatCompletionUsage();
        usage.setPromptTokens(tokenUsage == null || tokenUsage.inputTokenCount() == null ? 0 : tokenUsage.inputTokenCount());
        usage.setCompletionTokens(tokenUsage == null || tokenUsage.outputTokenCount() == null ? 0 : tokenUsage.outputTokenCount());
        usage.setTotalTokens(tokenUsage == null || tokenUsage.totalTokenCount() == null ? 0 : tokenUsage.totalTokenCount());
        return usage;
    }

    private KnowledgeReferenceItem toReferenceItem(KnowledgeSearchHit hit) {
        KnowledgeReferenceItem item = new KnowledgeReferenceItem();
        item.setKnowledgeId(hit.getKnowledgeId());
        item.setKnowledgeName(hit.getKnowledgeName());
        item.setDocsId(hit.getDocsId());
        item.setDocsName(hit.getDocsName());
        item.setSegmentId(hit.getSegmentId());
        item.setScore(hit.getScore());
        item.setContent(hit.getContent());
        item.setMetadata(hit.getMetadata());
        return item;
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }
}
