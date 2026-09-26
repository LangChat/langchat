package cn.langchat.monitor.service;

import cn.langchat.monitor.entity.AigcModelCallLog;
import cn.langchat.monitor.model.response.ModelCallOverviewResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 模型调用统计服务。
 *
 * <p>在应用侧完成聚合而非数据库分组：模型调用日志量级可控，且需要与前端图表结构
 * 一一对应，应用侧聚合更易于扩展与调试。</p>
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelCallStatisticsService {

    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final String STATUS_ERROR = "ERROR";
    private static final int DEFAULT_WINDOW_DAYS = 7;
    private static final int MAX_WINDOW_DAYS = 90;
    private static final int TOP_MODEL_LIMIT = 8;
    private static final int RECENT_CALL_LIMIT = 10;
    private static final String UNKNOWN_LABEL = "未标注";

    private final AigcModelCallLogService aigcModelCallLogService;

    /**
     * 构建监控总览。
     *
     * @param windowDays 统计窗口天数，缺省或非法时取 7 天
     */
    public ModelCallOverviewResponse getOverview(Integer windowDays) {
        int days = resolveWindowDays(windowDays);
        long windowStart = LocalDate.now()
                .minusDays(days - 1L)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        List<AigcModelCallLog> logs = aigcModelCallLogService.lambdaQuery()
                .ge(AigcModelCallLog::getCreateTime, windowStart)
                .orderByDesc(AigcModelCallLog::getCreateTime)
                .list();

        ModelCallOverviewResponse response = new ModelCallOverviewResponse();
        response.setWindowDays(days);
        response.setMetrics(buildMetrics(logs));
        response.setTrend(buildTrend(logs, days));
        response.setProviderDistribution(buildDistribution(logs, AigcModelCallLog::getProvider));
        response.setCallTypeDistribution(buildDistribution(logs, AigcModelCallLog::getCallType));
        response.setSceneDistribution(buildDistribution(logs, AigcModelCallLog::getScene));
        response.setTopModels(buildTopModels(logs));
        response.setRecentCalls(buildRecentCalls(logs));
        return response;
    }

    private int resolveWindowDays(Integer windowDays) {
        if (windowDays == null || windowDays <= 0) {
            return DEFAULT_WINDOW_DAYS;
        }
        return Math.min(windowDays, MAX_WINDOW_DAYS);
    }

    /**
     * 指标卡片：调用总量、Token 消耗、平均耗时、失败次数。
     */
    private List<ModelCallOverviewResponse.MetricCard> buildMetrics(List<AigcModelCallLog> logs) {
        long callCount = logs.size();
        long tokenCount = logs.stream().mapToLong(this::tokenOf).sum();
        long errorCount = logs.stream().filter(this::isError).count();
        long avgDuration = logs.stream()
                .filter(item -> item.getDuration() != null)
                .mapToLong(AigcModelCallLog::getDuration)
                .average()
                .stream()
                .mapToLong(value -> (long) value)
                .findFirst()
                .orElse(0L);
        double successRate = callCount == 0 ? 100D : (callCount - errorCount) * 100D / callCount;

        List<ModelCallOverviewResponse.MetricCard> metrics = new ArrayList<>();
        metrics.add(metric("调用总量", formatCount(callCount), "统计窗口内的模型调用次数", "primary"));
        metrics.add(metric("Token 消耗", formatCount(tokenCount), "输入与输出 Token 合计", "success"));
        metrics.add(metric("平均耗时", avgDuration + " ms", "单次模型调用的平均响应时间", "warning"));
        metrics.add(metric(
                "调用成功率",
                String.format("%.1f%%", successRate),
                errorCount == 0 ? "当前窗口无失败调用" : "失败 " + errorCount + " 次",
                errorCount == 0 ? "success" : "error"
        ));
        return metrics;
    }

    private ModelCallOverviewResponse.MetricCard metric(String label, String value, String hint, String tone) {
        ModelCallOverviewResponse.MetricCard card = new ModelCallOverviewResponse.MetricCard();
        card.setLabel(label);
        card.setValue(value);
        card.setHint(hint);
        card.setTone(tone);
        return card;
    }

    /**
     * 按天聚合调用次数与 Token 消耗，缺失日期补零保证图表连续。
     */
    private List<ModelCallOverviewResponse.TrendPoint> buildTrend(List<AigcModelCallLog> logs, int days) {
        Map<String, ModelCallOverviewResponse.TrendPoint> points = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int offset = days - 1; offset >= 0; offset--) {
            String label = today.minusDays(offset).format(DATE_LABEL_FORMATTER);
            ModelCallOverviewResponse.TrendPoint point = new ModelCallOverviewResponse.TrendPoint();
            point.setDate(label);
            point.setCallCount(0L);
            point.setTokenCount(0L);
            points.put(label, point);
        }

        for (AigcModelCallLog item : logs) {
            String label = formatDate(item.getCreateTime());
            ModelCallOverviewResponse.TrendPoint point = points.get(label);
            if (point == null) {
                continue;
            }
            point.setCallCount(point.getCallCount() + 1);
            point.setTokenCount(point.getTokenCount() + tokenOf(item));
        }
        return new ArrayList<>(points.values());
    }

    /**
     * 按指定维度聚合分布，按调用次数倒序。
     */
    private List<ModelCallOverviewResponse.DistributionItem> buildDistribution(
            List<AigcModelCallLog> logs,
            Function<AigcModelCallLog, String> dimension
    ) {
        Map<String, long[]> grouped = new LinkedHashMap<>();
        for (AigcModelCallLog item : logs) {
            String key = normalizeLabel(dimension.apply(item));
            long[] counter = grouped.computeIfAbsent(key, ignored -> new long[2]);
            counter[0] += 1;
            counter[1] += tokenOf(item);
        }
        return grouped.entrySet().stream()
                .map(entry -> {
                    ModelCallOverviewResponse.DistributionItem item = new ModelCallOverviewResponse.DistributionItem();
                    item.setName(entry.getKey());
                    item.setCallCount(entry.getValue()[0]);
                    item.setTokenCount(entry.getValue()[1]);
                    return item;
                })
                .sorted(Comparator.comparingLong(ModelCallOverviewResponse.DistributionItem::getCallCount).reversed())
                .toList();
    }

    /**
     * 模型消耗排行，按 Token 消耗倒序。
     */
    private List<ModelCallOverviewResponse.ModelUsageItem> buildTopModels(List<AigcModelCallLog> logs) {
        Map<String, List<AigcModelCallLog>> grouped = logs.stream()
                .collect(Collectors.groupingBy(
                        item -> normalizeLabel(item.getModelId()) + "|" + normalizeLabel(item.getModelName()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.values().stream()
                .map(this::toModelUsageItem)
                .sorted(Comparator.comparingLong(ModelCallOverviewResponse.ModelUsageItem::getTokenCount).reversed())
                .limit(TOP_MODEL_LIMIT)
                .toList();
    }

    private ModelCallOverviewResponse.ModelUsageItem toModelUsageItem(List<AigcModelCallLog> items) {
        AigcModelCallLog first = items.get(0);
        ModelCallOverviewResponse.ModelUsageItem usage = new ModelCallOverviewResponse.ModelUsageItem();
        usage.setModelId(first.getModelId());
        usage.setModelName(first.getModelName());
        usage.setProvider(first.getProvider());
        usage.setCallCount((long) items.size());
        usage.setTokenCount(items.stream().mapToLong(this::tokenOf).sum());
        usage.setErrorCount(items.stream().filter(this::isError).count());
        usage.setAvgDuration(items.stream()
                .filter(item -> item.getDuration() != null)
                .mapToLong(AigcModelCallLog::getDuration)
                .average()
                .stream()
                .mapToLong(value -> (long) value)
                .findFirst()
                .orElse(0L));
        return usage;
    }

    /**
     * 最近调用记录。
     */
    private List<ModelCallOverviewResponse.RecentCallItem> buildRecentCalls(List<AigcModelCallLog> logs) {
        return logs.stream()
                .limit(RECENT_CALL_LIMIT)
                .map(item -> {
                    ModelCallOverviewResponse.RecentCallItem call = new ModelCallOverviewResponse.RecentCallItem();
                    call.setModelName(item.getModelName());
                    call.setProvider(item.getProvider());
                    call.setCallType(item.getCallType());
                    call.setScene(item.getScene());
                    call.setStatus(item.getStatus());
                    call.setTotalToken(item.getTotalToken());
                    call.setDuration(item.getDuration());
                    call.setCreateTime(item.getCreateTime());
                    return call;
                })
                .toList();
    }

    private long tokenOf(AigcModelCallLog item) {
        return item.getTotalToken() == null ? 0L : item.getTotalToken();
    }

    private boolean isError(AigcModelCallLog item) {
        return STATUS_ERROR.equalsIgnoreCase(item.getStatus());
    }

    private String formatDate(Long timestamp) {
        if (timestamp == null) {
            return "";
        }
        return java.time.Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DATE_LABEL_FORMATTER);
    }

    private String normalizeLabel(String value) {
        return value == null || value.isBlank() ? UNKNOWN_LABEL : value;
    }

    /**
     * 大数值可读化：超过万级折算为万。
     */
    private String formatCount(long value) {
        if (value >= 100_000_000L) {
            return String.format("%.2f 亿", value / 100_000_000D);
        }
        if (value >= 10_000L) {
            return String.format("%.2f 万", value / 10_000D);
        }
        return String.valueOf(value);
    }
}
