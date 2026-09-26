package cn.langchat.claw.server.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.langchat.claw.aigc.biz.entity.*;
import cn.langchat.claw.aigc.biz.service.*;
import cn.langchat.claw.auth.service.AigcMenuService;
import cn.langchat.claw.auth.service.AigcRoleService;
import cn.langchat.claw.auth.service.AigcUserService;
import cn.langchat.claw.server.model.response.ExploreOverviewResponse;
import cn.langchat.claw.server.service.ExploreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Explore 概览服务实现。
 *
 * @author LangChat Team
 * @since 2026/3/27
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExploreServiceImpl implements ExploreService {

    private static final String AGENT_STATUS_PUBLISHED = "PUBLISHED";
    private static final String DOC_STATUS_PENDING = "pending";
    private static final String DOC_STATUS_RUNNING = "running";
    private static final String DOC_STATUS_COMPLETED = "completed";
    private static final String DOC_STATUS_FAILED = "failed";
    private static final String TONE_PRIMARY = "primary";
    private static final String TONE_SUCCESS = "success";
    private static final String TONE_WARNING = "warning";
    private static final String TONE_ERROR = "error";
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final String TREND_KEY_MESSAGE = "message";
    private static final String TREND_KEY_TOKEN = "token";
    private static final String TREND_KEY_USER = "user";

    private final AigcAgentService aigcAgentService;
    private final AigcLogService aigcLogService;
    private final AigcMcpService aigcMcpService;
    private final AigcMenuService aigcMenuService;
    private final AigcMessageService aigcMessageService;
    private final AigcRoleService aigcRoleService;
    private final AigcSkillService aigcSkillService;
    private final AigcUserService aigcUserService;

    @Override
    public ExploreOverviewResponse getOverview() {
        List<AigcAgent> agents = aigcAgentService.list();
        long now = System.currentTimeMillis();
        long currentWindowStart = dayStartOffset(6);
        long previousWindowStart = dayStartOffset(13);
        long previousWindowEnd = currentWindowStart - 1;
        List<AigcMessage> recentMessages = aigcMessageService.lambdaQuery()
                .ge(AigcMessage::getCreateTime, previousWindowStart)
                .le(AigcMessage::getCreateTime, now)
                .list();
        List<cn.langchat.claw.auth.entity.AigcUser> recentUsers = aigcUserService.lambdaQuery()
                .ge(cn.langchat.claw.auth.entity.AigcUser::getCreateTime, previousWindowStart)
                .le(cn.langchat.claw.auth.entity.AigcUser::getCreateTime, now)
                .list();

        ExploreOverviewResponse response = new ExploreOverviewResponse();
        response.setCoreMetrics(buildCoreMetrics(agents, recentMessages, recentUsers, currentWindowStart, previousWindowStart, previousWindowEnd, now));
        response.setTrendSeries(buildTrendSeries(recentMessages, recentUsers, currentWindowStart));
        response.setGovernanceMetrics(List.of());
        response.setDocumentMetrics(List.of());
        response.setProviderMetrics(List.of());
        response.setRecentActivities(List.of());
        return response;
    }

    /**
     * 构建核心指标。
     */
    private List<ExploreOverviewResponse.MetricCard> buildCoreMetrics(
            List<AigcAgent> agents,
            List<AigcMessage> messages,
            List<cn.langchat.claw.auth.entity.AigcUser> users,
            long currentWindowStart,
            long previousWindowStart,
            long previousWindowEnd,
            long now
    ) {
        long currentMessageCount = messages.stream()
                .filter(message -> inRange(message.getCreateTime(), currentWindowStart, now))
                .count();
        long previousMessageCount = messages.stream()
                .filter(message -> inRange(message.getCreateTime(), previousWindowStart, previousWindowEnd))
                .count();
        long currentTokenConsumption = messages.stream()
                .filter(message -> inRange(message.getCreateTime(), currentWindowStart, now))
                .mapToLong(this::messageToken)
                .sum();
        long previousTokenConsumption = messages.stream()
                .filter(message -> inRange(message.getCreateTime(), previousWindowStart, previousWindowEnd))
                .mapToLong(this::messageToken)
                .sum();
        long currentUserCount = users.stream()
                .filter(user -> inRange(user.getCreateTime(), currentWindowStart, now))
                .count();
        long previousUserCount = users.stream()
                .filter(user -> inRange(user.getCreateTime(), previousWindowStart, previousWindowEnd))
                .count();
        long publishedAgents = agents.stream()
                .filter(agent -> AGENT_STATUS_PUBLISHED.equals(agent.getStatus()))
                .count();
        String messageDelta = formatDelta(currentMessageCount - previousMessageCount);
        String tokenDelta = formatDelta(currentTokenConsumption - previousTokenConsumption);
        String userDelta = formatDelta(currentUserCount - previousUserCount);
        return List.of(
                metric("消息数量", String.valueOf(currentMessageCount), "近 7 天消息总量。", "较前 7 天 " + messageDelta, toneByDelta(currentMessageCount - previousMessageCount)),
                metric("Token 消耗量", String.valueOf(currentTokenConsumption), "近 7 天输入+输出 Token 总量。", "较前 7 天 " + tokenDelta, toneByDelta(currentTokenConsumption - previousTokenConsumption)),
                metric("用户数量", String.valueOf(currentUserCount), "近 7 天新增用户数量。", "较前 7 天 " + userDelta, toneByDelta(currentUserCount - previousUserCount)),
                metric("应用数量", String.valueOf(agents.size()), "当前已配置智能体应用总数（含草稿与发布）。", "已发布 " + publishedAgents, TONE_PRIMARY)
        );
    }

    /**
     * 构建趋势折线数据。
     */
    private List<ExploreOverviewResponse.TrendSeries> buildTrendSeries(
            List<AigcMessage> messages,
            List<cn.langchat.claw.auth.entity.AigcUser> users,
            long currentWindowStart
    ) {
        List<LocalDate> dates = buildRecentDates(7);
        Map<LocalDate, Long> messageMap = initSeriesMap(dates);
        Map<LocalDate, Long> tokenMap = initSeriesMap(dates);
        Map<LocalDate, Long> userMap = initSeriesMap(dates);

        messages.stream()
                .filter(message -> message.getCreateTime() != null && message.getCreateTime() >= currentWindowStart)
                .forEach(message -> {
                    LocalDate date = toLocalDate(message.getCreateTime());
                    if (messageMap.containsKey(date)) {
                        messageMap.put(date, messageMap.get(date) + 1);
                        tokenMap.put(date, tokenMap.get(date) + messageToken(message));
                    }
                });

        users.stream()
                .filter(user -> user.getCreateTime() != null && user.getCreateTime() >= currentWindowStart)
                .forEach(user -> {
                    LocalDate date = toLocalDate(user.getCreateTime());
                    if (userMap.containsKey(date)) {
                        userMap.put(date, userMap.get(date) + 1);
                    }
                });

        return List.of(
                createTrendSeries(TREND_KEY_MESSAGE, "消息量趋势", "条", messageMap),
                createTrendSeries(TREND_KEY_TOKEN, "Token 消耗趋势", "tokens", tokenMap),
                createTrendSeries(TREND_KEY_USER, "新增用户趋势", "人", userMap)
        );
    }

    /**
     * 构建治理指标。
     */
    private List<ExploreOverviewResponse.MetricCard> buildGovernanceMetrics(List<AigcDocs> docs, List<AigcAgent> agents) {
        long publishedAgents = agents.stream()
                .filter(agent -> AGENT_STATUS_PUBLISHED.equals(agent.getStatus()))
                .count();
        long readyDocs = docs.stream()
                .filter(doc -> DOC_STATUS_COMPLETED.equalsIgnoreCase(String.valueOf(doc.getEmbedStatus())))
                .count();
        long readyKnowledges = docs.stream()
                .filter(doc -> DOC_STATUS_COMPLETED.equalsIgnoreCase(String.valueOf(doc.getEmbedStatus())))
                .map(AigcDocs::getKnowledgeId)
                .filter(knowledgeId -> knowledgeId != null && !knowledgeId.isBlank())
                .distinct()
                .count();
        return List.of(
                metric("发布智能体", String.valueOf(publishedAgents), "重点关注可直接提供服务的智能体。", "已发布 / 总智能体", TONE_SUCCESS),
                metric("可检索知识库", String.valueOf(readyKnowledges), "至少存在一份向量化完成文档的知识库。", "具备检索能力的知识库", TONE_SUCCESS),
                metric("向量化完成", String.valueOf(readyDocs), "文档切分与向量化已完成，可用于检索。", "文档入索引成功数", TONE_PRIMARY),
                metric("技能中心", String.valueOf(aigcSkillService.count()), "支撑 HTTP、MCP 与知识检索等技能。", "可编排技能数量", TONE_WARNING),
                metric("MCP 服务", String.valueOf(aigcMcpService.count()), "外部能力接入与工具市场基础设施。", "已配置 MCP 服务", TONE_WARNING),
                metric("权限治理", String.valueOf(aigcMenuService.count()), "覆盖菜单、角色与账号管理。", "菜单资源总数", TONE_PRIMARY),
                metric("角色配置", String.valueOf(aigcRoleService.count()), "角色授权驱动动态菜单与按钮权限。", "角色总数", TONE_PRIMARY),
                metric("账号规模", String.valueOf(aigcUserService.count()), "当前系统内受管账号数量。", "用户总数", TONE_PRIMARY)
        );
    }

    /**
     * 构建文档状态指标。
     */
    private List<ExploreOverviewResponse.MetricCard> buildDocumentMetrics(List<AigcDocs> docs) {
        Map<String, Long> docStatusMap = docs.stream()
                .collect(Collectors.groupingBy(doc -> normalizeDocStatus(doc.getEmbedStatus()), Collectors.counting()));
        return List.of(
                metric("待处理", String.valueOf(docStatusMap.getOrDefault(DOC_STATUS_PENDING, 0L)), "已接入但尚未开始向量化。", "待执行向量任务", TONE_WARNING),
                metric("处理中", String.valueOf(docStatusMap.getOrDefault(DOC_STATUS_RUNNING, 0L)), "当前正在解析、切分或写入向量库。", "运行中的任务", TONE_PRIMARY),
                metric("已完成", String.valueOf(docStatusMap.getOrDefault(DOC_STATUS_COMPLETED, 0L)), "任务已完成，可直接参与召回。", "完成入索引文档", TONE_SUCCESS),
                metric("失败任务", String.valueOf(docStatusMap.getOrDefault(DOC_STATUS_FAILED, 0L)), "需要排查向量库连接、模型配置或文档内容。", "异常任务数量", TONE_ERROR)
        );
    }

    /**
     * 构建供应商覆盖统计。
     */
    private List<ExploreOverviewResponse.ProviderMetric> buildProviderMetrics(List<AigcModel> models, List<AigcVectorStore> vectorStores) {
        List<ExploreOverviewResponse.ProviderMetric> modelProviders = buildProviderItems("模型供应商", models, AigcModel::getProvider);
        List<ExploreOverviewResponse.ProviderMetric> vectorProviders = buildProviderItems("向量库供应商", vectorStores, AigcVectorStore::getProvider);
        return List.copyOf(
                java.util.stream.Stream.concat(modelProviders.stream(), vectorProviders.stream())
                        .sorted(Comparator.comparing(ExploreOverviewResponse.ProviderMetric::getCategory)
                                .thenComparing(ExploreOverviewResponse.ProviderMetric::getCount, Comparator.reverseOrder()))
                        .toList()
        );
    }

    /**
     * 构建最新活动。
     */
    private List<ExploreOverviewResponse.RecentActivity> buildRecentActivities() {
        return aigcLogService.lambdaQuery()
                .orderByDesc(AigcLog::getCreateTime)
                .last("limit 8")
                .list()
                .stream()
                .map(log -> {
                    ExploreOverviewResponse.RecentActivity activity = new ExploreOverviewResponse.RecentActivity();
                    activity.setAction(log.getOperation());
                    activity.setCreateTime(log.getCreateTime());
                    activity.setDuration(log.getTime());
                    activity.setUrl(log.getUrl());
                    activity.setUsername(log.getUsername());
                    return activity;
                })
                .toList();
    }

    /**
     * 构建供应商统计项。
     */
    private <T> List<ExploreOverviewResponse.ProviderMetric> buildProviderItems(
            String category,
            List<T> items,
            Function<T, String> providerExtractor
    ) {
        return items.stream()
                .map(providerExtractor)
                .filter(provider -> provider != null && !provider.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> {
                    ExploreOverviewResponse.ProviderMetric metric = new ExploreOverviewResponse.ProviderMetric();
                    metric.setCategory(category);
                    metric.setCount(entry.getValue());
                    metric.setProvider(entry.getKey());
                    return metric;
                })
                .toList();
    }

    /**
     * 构建指标卡片。
     */
    private ExploreOverviewResponse.MetricCard metric(
            String label,
            String value,
            String description,
            String hint,
            String tone
    ) {
        ExploreOverviewResponse.MetricCard card = new ExploreOverviewResponse.MetricCard();
        card.setDescription(description);
        card.setHint(hint);
        card.setLabel(label);
        card.setTone(tone);
        card.setValue(value);
        return card;
    }

    /**
     * 归一化文档状态。
     */
    private String normalizeDocStatus(String status) {
        if (status == null || status.isBlank()) {
            return DOC_STATUS_PENDING;
        }
        String normalized = status.trim().toLowerCase();
        return switch (normalized) {
            case DOC_STATUS_RUNNING -> DOC_STATUS_RUNNING;
            case DOC_STATUS_COMPLETED -> DOC_STATUS_COMPLETED;
            case DOC_STATUS_FAILED -> DOC_STATUS_FAILED;
            default -> DOC_STATUS_PENDING;
        };
    }

    private ExploreOverviewResponse.TrendSeries createTrendSeries(
            String key,
            String label,
            String unit,
            Map<LocalDate, Long> values
    ) {
        ExploreOverviewResponse.TrendSeries series = new ExploreOverviewResponse.TrendSeries();
        series.setKey(key);
        series.setLabel(label);
        series.setUnit(unit);
        List<ExploreOverviewResponse.TrendPoint> points = new ArrayList<>();
        values.forEach((date, value) -> {
            ExploreOverviewResponse.TrendPoint point = new ExploreOverviewResponse.TrendPoint();
            point.setDate(date.format(DATE_LABEL_FORMATTER));
            point.setValue(value);
            points.add(point);
        });
        series.setPoints(points);
        return series;
    }

    private Map<LocalDate, Long> initSeriesMap(List<LocalDate> dates) {
        Map<LocalDate, Long> values = new LinkedHashMap<>();
        dates.forEach(date -> values.put(date, 0L));
        return values;
    }

    private List<LocalDate> buildRecentDates(int days) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<LocalDate> dates = new ArrayList<>();
        for (int offset = days - 1; offset >= 0; offset--) {
            dates.add(today.minusDays(offset));
        }
        return dates;
    }

    private long dayStartOffset(int daysBeforeToday) {
        LocalDate date = LocalDate.now(ZoneId.systemDefault()).minusDays(daysBeforeToday);
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private LocalDate toLocalDate(Long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private boolean inRange(Long timestamp, long start, long end) {
        return timestamp != null && timestamp >= start && timestamp <= end;
    }

    private long messageToken(AigcMessage message) {
        return (message.getInputToken() == null ? 0 : message.getInputToken())
                + (message.getOutputToken() == null ? 0 : message.getOutputToken());
    }

    private String formatDelta(long delta) {
        if (delta > 0) {
            return "+" + NumberUtil.decimalFormat(",###", delta);
        }
        if (delta < 0) {
            return NumberUtil.decimalFormat(",###", delta);
        }
        return "持平";
    }

    private String toneByDelta(long delta) {
        if (delta > 0) {
            return TONE_SUCCESS;
        }
        if (delta < 0) {
            return TONE_WARNING;
        }
        return TONE_PRIMARY;
    }
}
