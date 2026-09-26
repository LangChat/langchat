package cn.langchat.core.chat.support;

import cn.langchat.datasource.model.DataQueryResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 将查询结果转换为受控的 ECharts option。
 */
@Component
public class DataAnalysisChartBuilder {

    private static final List<String> SUPPORTED_TYPES = List.of("auto", "bar", "line", "pie", "none");

    public Map<String, Object> build(String requestedType, String title, DataQueryResult result) {
        if (result == null || result.rows().isEmpty() || result.columns().isEmpty()) {
            return Map.of();
        }
        String chartType = normalizeType(requestedType);
        if ("none".equals(chartType)) {
            return Map.of();
        }
        List<String> numericColumns = result.columns().stream()
                .filter(column -> result.rows().stream().anyMatch(row -> row.get(column) instanceof Number))
                .toList();
        if (numericColumns.isEmpty()) {
            return Map.of();
        }
        String categoryColumn = result.columns().stream()
                .filter(column -> !numericColumns.contains(column))
                .findFirst()
                .orElse(result.columns().get(0));
        if ("auto".equals(chartType)) {
            String category = categoryColumn.toLowerCase(Locale.ROOT);
            chartType = category.contains("date") || category.contains("time") || category.contains("日期")
                    ? "line"
                    : "bar";
        }
        Map<String, Object> option = "pie".equals(chartType)
                ? pieOption(title, categoryColumn, numericColumns.get(0), result.rows())
                : axisOption(chartType, title, categoryColumn, numericColumns, result.rows());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chart_type", chartType);
        payload.put("option", option);
        payload.put("title", defaultTitle(title));
        return payload;
    }

    private Map<String, Object> axisOption(
            String type,
            String title,
            String categoryColumn,
            List<String> numericColumns,
            List<Map<String, Object>> rows
    ) {
        Map<String, Object> option = baseOption(title);
        option.put("grid", Map.of("top", 54, "right", 24, "bottom", 36, "left", 56));
        option.put("xAxis", Map.of(
                "type", "category",
                "data", rows.stream().map(row -> row.get(categoryColumn)).toList()
        ));
        option.put("yAxis", Map.of("type", "value"));
        List<Map<String, Object>> series = new ArrayList<>();
        for (String column : numericColumns) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", column);
            item.put("type", type);
            item.put("data", rows.stream().map(row -> row.get(column)).toList());
            if ("line".equals(type)) {
                item.put("smooth", true);
            }
            series.add(item);
        }
        option.put("series", series);
        return option;
    }

    private Map<String, Object> pieOption(
            String title,
            String categoryColumn,
            String valueColumn,
            List<Map<String, Object>> rows
    ) {
        Map<String, Object> option = baseOption(title);
        List<Map<String, Object>> data = rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", String.valueOf(row.get(categoryColumn)));
            item.put("value", row.get(valueColumn));
            return item;
        }).toList();
        option.put("series", List.of(Map.of(
                "name", valueColumn,
                "type", "pie",
                "radius", List.of("36%", "68%"),
                "data", data
        )));
        return option;
    }

    private Map<String, Object> baseOption(String title) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("title", Map.of("text", defaultTitle(title), "left", "center"));
        option.put("tooltip", Map.of("trigger", "axis"));
        option.put("legend", Map.of("bottom", 0));
        return option;
    }

    private String normalizeType(String requestedType) {
        String normalized = requestedType == null ? "auto" : requestedType.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_TYPES.contains(normalized) ? normalized : "auto";
    }

    private String defaultTitle(String title) {
        return title == null || title.isBlank() ? "智能问数结果" : title.trim();
    }
}
