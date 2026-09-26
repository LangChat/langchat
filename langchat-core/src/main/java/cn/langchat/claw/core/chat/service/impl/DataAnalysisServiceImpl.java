package cn.langchat.claw.core.chat.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.langchat.claw.aigc.biz.entity.AigcModel;
import cn.langchat.claw.aigc.biz.service.AigcModelService;
import cn.langchat.claw.common.exception.BizException;
import cn.langchat.claw.core.chat.model.protocol.OpenAiChatCompletionChunk;
import cn.langchat.claw.core.chat.model.request.DataAnalysisRequest;
import cn.langchat.claw.core.chat.model.response.DataAnalysisCatalogItem;
import cn.langchat.claw.core.chat.service.DataAnalysisService;
import cn.langchat.claw.core.chat.support.AgentChatEventAssembler;
import cn.langchat.claw.core.chat.support.AgentStreamFluxUtil;
import cn.langchat.claw.core.chat.support.DataAnalysisChartBuilder;
import cn.langchat.claw.core.runtime.DataAnalysisAiService;
import cn.langchat.claw.core.runtime.factory.LangChain4jModelFactory;
import cn.langchat.claw.core.support.CoreErrorCode;
import cn.langchat.claw.datasource.entity.AigcDatasource;
import cn.langchat.claw.datasource.model.DataQueryResult;
import cn.langchat.claw.datasource.service.AigcDatasourceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecutor;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * 智能问数运行时实现。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataAnalysisServiceImpl implements DataAnalysisService {

    private static final String QUERY_TOOL = "query_data";
    private static final String SCHEMA_TOOL = "inspect_selected_tables";
    private static final String TIME_TOOL = "get_current_time";

    private final AigcDatasourceService datasourceService;
    private final AigcModelService modelService;
    private final LangChain4jModelFactory modelFactory;
    private final DataAnalysisChartBuilder chartBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public List<DataAnalysisCatalogItem> listCatalog() {
        List<DataAnalysisCatalogItem> catalog = new ArrayList<>();
        List<AigcDatasource> datasources = datasourceService.lambdaQuery()
                .eq(AigcDatasource::getEnabled, true)
                .orderByDesc(AigcDatasource::getUpdateTime)
                .list();
        for (AigcDatasource datasource : datasources) {
            List<ConfiguredTable> tables = parseConfiguredTables(datasource.getStructureJson());
            if (tables.isEmpty()) {
                continue;
            }
            catalog.add(new DataAnalysisCatalogItem(
                    datasource.getId(),
                    datasource.getName(),
                    datasource.getDbType(),
                    tables.stream().map(table -> new DataAnalysisCatalogItem.TableItem(
                            table.name(),
                            table.sourceName(),
                            table.comment(),
                            table.columns().size()
                    )).toList()
            ));
        }
        return catalog;
    }

    @Override
    public Flux<ServerSentEvent<String>> stream(DataAnalysisRequest request) {
        AnalysisContext context = resolveContext(request);
        AigcModel model = resolveModel(request.getModelId());
        String conversationId = StrUtil.blankToDefault(
                request.getConversationId(),
                "analysis-" + UUID.randomUUID().toString().replace("-", "")
        );
        AgentChatEventAssembler assembler = new AgentChatEventAssembler(
                conversationId,
                "chatcmpl-" + UUID.randomUUID().toString().replace("-", ""),
                Instant.now().getEpochSecond(),
                model.getModel()
        );
        return AgentStreamFluxUtil.createEventStream(
                sink -> runAnalysis(request, context, model, assembler, sink),
                assembler,
                objectMapper
        );
    }

    private void runAnalysis(
            DataAnalysisRequest request,
            AnalysisContext context,
            AigcModel model,
            AgentChatEventAssembler assembler,
            FluxSink<OpenAiChatCompletionChunk> sink
    ) {
        AtomicBoolean completed = new AtomicBoolean(false);
        ConcurrentMap<String, QueryExecution> queryExecutions = new ConcurrentHashMap<>();
        try {
            StreamingChatModel chatModel = modelFactory.getStreamingChatModel(model, null);
            Map<ToolSpecification, ToolExecutor> tools = buildTools(context, queryExecutions);
            DataAnalysisAiService aiService = AiServices.builder(DataAnalysisAiService.class)
                    .streamingChatModel(chatModel)
                    .tools(tools)
                    .build();
            sink.next(assembler.messageStart());
            sink.next(assembler.logDelta(
                    "已加载授权表结构",
                    "analysis.schema.loaded",
                    "completed",
                    Map.of("table_count", context.tables().size())
            ));
            TokenStream stream = aiService.analyze(buildSystemPrompt(context), request.getQuestion());
            stream.onPartialResponse(partial -> sink.next(assembler.messageDelta(partial)))
                    .beforeToolExecution(before -> sink.next(assembler.beforeTool(before)))
                    .onToolExecuted(execution -> {
                        sink.next(assembler.toolExecuted(execution));
                        QueryExecution query = queryExecutions.remove(execution.request().id());
                        if (!execution.hasFailed() && query != null) {
                            emitAnalysisResult(query, assembler, sink);
                        }
                    })
                    .onCompleteResponse(response -> {
                        if (completed.compareAndSet(false, true)) {
                            completeStream(response, assembler, sink);
                        }
                    })
                    .onError(error -> {
                        if (completed.compareAndSet(false, true)) {
                            log.error("智能问数执行失败，datasourceId={}", request.getDatasourceId(), error);
                            sink.next(assembler.error(error));
                            sink.complete();
                        }
                    })
                    .start();
        } catch (Exception ex) {
            log.error("构建智能问数运行时失败，datasourceId={}", request.getDatasourceId(), ex);
            sink.next(assembler.error(ex));
            sink.complete();
        }
    }

    private Map<ToolSpecification, ToolExecutor> buildTools(
            AnalysisContext context,
            ConcurrentMap<String, QueryExecution> executions
    ) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        ToolSpecification querySpec = ToolSpecification.builder()
                .name(QUERY_TOOL)
                .description("对用户已授权的表执行只读 SQL，并生成表格与可选图表")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("sql", "必须是只读 SELECT/WITH 查询，使用真实表名和字段名")
                        .addStringProperty("title", "结果标题，使用简短中文")
                        .addStringProperty("chartType", "图表类型：auto、bar、line、pie、none")
                        .required(List.of("sql"))
                        .additionalProperties(false)
                        .build())
                .build();
        tools.put(querySpec, (toolRequest, memoryId) -> {
            Map<String, Object> arguments = parseToolArguments(toolRequest.arguments());
            String sql = stringValue(arguments.get("sql"));
            String title = stringValue(arguments.get("title"));
            String chartType = stringValue(arguments.get("chartType"));
            DataQueryResult result = datasourceService.executeReadOnlyQuery(
                    context.datasource().getId(),
                    sql,
                    context.allowedTables()
            );
            executions.put(toolRequest.id(), new QueryExecution(sql, title, chartType, result));
            return writeJson(Map.of(
                    "columns", result.columns(),
                    "rowCount", result.rows().size(),
                    "rows", result.rows()
            ));
        });

        ToolSpecification schemaSpec = ToolSpecification.builder()
                .name(SCHEMA_TOOL)
                .description("重新查看当前已选择且允许 AI 检索的表结构")
                .parameters(JsonObjectSchema.builder().additionalProperties(false).build())
                .build();
        tools.put(schemaSpec, (toolRequest, memoryId) -> context.schemaPrompt());

        ToolSpecification timeSpec = ToolSpecification.builder()
                .name(TIME_TOOL)
                .description("获取当前系统时间，用于处理今天、本月、最近等相对时间问题")
                .parameters(JsonObjectSchema.builder().additionalProperties(false).build())
                .build();
        tools.put(timeSpec, (toolRequest, memoryId) -> OffsetDateTime.now().toString());
        return tools;
    }

    private void emitAnalysisResult(
            QueryExecution query,
            AgentChatEventAssembler assembler,
            FluxSink<OpenAiChatCompletionChunk> sink
    ) {
        Map<String, Object> tablePayload = new LinkedHashMap<>();
        tablePayload.put("columns", query.result().columns());
        tablePayload.put("row_count", query.result().rows().size());
        tablePayload.put("rows", query.result().rows());
        tablePayload.put("sql", query.sql());
        tablePayload.put("title", defaultTitle(query.title()));
        sink.next(assembler.analysisTable(tablePayload));

        Map<String, Object> chartPayload = chartBuilder.build(
                query.chartType(),
                query.title(),
                query.result()
        );
        if (!chartPayload.isEmpty()) {
            sink.next(assembler.analysisEchart(chartPayload));
        }
    }

    private void completeStream(
            ChatResponse response,
            AgentChatEventAssembler assembler,
            FluxSink<OpenAiChatCompletionChunk> sink
    ) {
        sink.next(assembler.messageCompleted(
                "analysis-message-" + UUID.randomUUID().toString().replace("-", ""),
                response
        ));
        sink.next(assembler.messageStop(
                response.finishReason() == null ? "stop" : response.finishReason().name().toLowerCase(Locale.ROOT)
        ));
        sink.complete();
    }

    private AnalysisContext resolveContext(DataAnalysisRequest request) {
        if (request == null || StrUtil.isBlank(request.getDatasourceId())
                || StrUtil.isBlank(request.getQuestion())
                || request.getTableNames() == null || request.getTableNames().isEmpty()) {
            throw new BizException(CoreErrorCode.INVALID_CHAT_REQUEST);
        }
        AigcDatasource datasource = datasourceService.getById(request.getDatasourceId());
        if (datasource == null || Boolean.FALSE.equals(datasource.getEnabled())) {
            throw new BizException(CoreErrorCode.INVALID_CHAT_REQUEST.code(), "数据源不存在或已停用");
        }
        Set<String> requested = request.getTableNames().stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<ConfiguredTable> selected = parseConfiguredTables(datasource.getStructureJson()).stream()
                .filter(table -> requested.contains(table.sourceName()))
                .toList();
        if (selected.isEmpty() || selected.size() != requested.size()) {
            throw new BizException(CoreErrorCode.INVALID_CHAT_REQUEST.code(), "选择中包含未导入或未启用的表");
        }
        Set<String> allowedTables = selected.stream()
                .map(ConfiguredTable::sourceName)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new AnalysisContext(datasource, selected, allowedTables, buildSchemaPrompt(selected));
    }

    private AigcModel resolveModel(String modelId) {
        AigcModel model = StrUtil.isNotBlank(modelId) ? modelService.getById(modelId) : null;
        if (model == null) {
            model = modelService.lambdaQuery()
                    .in(AigcModel::getType, List.of("CHAT", "REASONING"))
                    .orderByAsc(AigcModel::getCreateTime)
                    .last("LIMIT 1")
                    .one();
        }
        if (model == null) {
            throw new BizException(CoreErrorCode.MODEL_NOT_FOUND);
        }
        return model;
    }

    private List<ConfiguredTable> parseConfiguredTables(String structureJson) {
        if (StrUtil.isBlank(structureJson)) {
            return List.of();
        }
        try {
            JsonNode tablesNode = objectMapper.readTree(structureJson).path("tables");
            if (!tablesNode.isArray()) {
                return List.of();
            }
            List<ConfiguredTable> tables = new ArrayList<>();
            for (JsonNode tableNode : tablesNode) {
                if (!tableNode.path("imported").asBoolean(false)
                        || !tableNode.path("enabled").asBoolean(true)) {
                    continue;
                }
                String name = tableNode.path("tableName").asText("");
                String sourceName = tableNode.path("sourceTableName").asText(name);
                if (sourceName.isBlank()) {
                    continue;
                }
                List<ConfiguredColumn> columns = new ArrayList<>();
                for (JsonNode columnNode : tableNode.path("columns")) {
                    String aiName = columnNode.path("name").asText("");
                    String sourceColumn = columnNode.path("originalName").asText(aiName);
                    columns.add(new ConfiguredColumn(
                            sourceColumn,
                            aiName,
                            columnNode.path("type").asText(""),
                            columnNode.path("comment").asText("")
                    ));
                }
                tables.add(new ConfiguredTable(
                        name,
                        sourceName,
                        tableNode.path("tableComment").asText(""),
                        columns
                ));
            }
            return tables;
        } catch (Exception ex) {
            log.warn("解析智能问数表配置失败", ex);
            return List.of();
        }
    }

    private String buildSystemPrompt(AnalysisContext context) {
        return """
                你是企业数据分析助手。你只能基于当前授权表回答问题。
                必须遵守：
                1. 需要事实数据时必须调用 query_data，不得编造数字。
                2. SQL 只能是 SELECT 或 WITH，只能使用下方真实表名和真实字段名。
                3. 默认控制结果规模，明细查询不超过 200 行；优先聚合后再查询。
                4. query_data 的 chartType 只能使用 auto、bar、line、pie、none。
                5. 查询完成后用简洁中文总结关键结论，不要在正文重复完整表格或 ECharts JSON。
                6. 遇到“今天、本月、最近”等相对时间，先调用 get_current_time。

                授权表结构：
                """ + context.schemaPrompt();
    }

    private String buildSchemaPrompt(List<ConfiguredTable> tables) {
        StringBuilder prompt = new StringBuilder();
        for (ConfiguredTable table : tables) {
            prompt.append("\n表 ").append(table.sourceName());
            if (!table.name().equals(table.sourceName())) {
                prompt.append("（业务名称：").append(table.name()).append("）");
            }
            if (StrUtil.isNotBlank(table.comment())) {
                prompt.append("：").append(table.comment());
            }
            prompt.append('\n');
            for (ConfiguredColumn column : table.columns()) {
                prompt.append("- ").append(column.sourceName())
                        .append(" ").append(column.type());
                if (!column.aiName().equals(column.sourceName())) {
                    prompt.append("，业务字段名：").append(column.aiName());
                }
                if (StrUtil.isNotBlank(column.comment())) {
                    prompt.append("，含义：").append(column.comment());
                }
                prompt.append('\n');
            }
        }
        return prompt.toString();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化查询结果失败", ex);
        }
    }

    private Map<String, Object> parseToolArguments(String arguments) {
        try {
            JsonNode node = objectMapper.readTree(arguments == null ? "{}" : arguments);
            Map<String, Object> result = new LinkedHashMap<>();
            node.fields().forEachRemaining(entry ->
                    result.put(entry.getKey(), objectMapper.convertValue(entry.getValue(), Object.class)));
            return result;
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultTitle(String value) {
        return StrUtil.blankToDefault(value, "智能问数结果");
    }

    private record AnalysisContext(
            AigcDatasource datasource,
            List<ConfiguredTable> tables,
            Set<String> allowedTables,
            String schemaPrompt
    ) {
    }

    private record ConfiguredTable(
            String name,
            String sourceName,
            String comment,
            List<ConfiguredColumn> columns
    ) {
    }

    private record ConfiguredColumn(
            String sourceName,
            String aiName,
            String type,
            String comment
    ) {
    }

    private record QueryExecution(
            String sql,
            String title,
            String chartType,
            DataQueryResult result
    ) {
    }
}
