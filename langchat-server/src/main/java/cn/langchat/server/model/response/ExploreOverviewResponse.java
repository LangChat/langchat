package cn.langchat.server.model.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Explore 概览响应对象。
 *
 * @author LangChat Team
 * @since 2026/3/27
 */
@Data
public class ExploreOverviewResponse {

    /** 核心指标卡片。 */
    private List<MetricCard> coreMetrics = new ArrayList<>();

    /** 治理指标卡片。 */
    private List<MetricCard> governanceMetrics = new ArrayList<>();

    /** 文档状态指标。 */
    private List<MetricCard> documentMetrics = new ArrayList<>();

    /** 供应商覆盖统计。 */
    private List<ProviderMetric> providerMetrics = new ArrayList<>();

    /** 最新活动。 */
    private List<RecentActivity> recentActivities = new ArrayList<>();

    /** 趋势图数据。 */
    private List<TrendSeries> trendSeries = new ArrayList<>();

    /**
     * 指标卡片。
     */
    @Data
    public static class MetricCard {

        /** 辅助描述。 */
        private String description;

        /** 指标说明。 */
        private String hint;

        /** 标签。 */
        private String label;

        /** 状态色。 */
        private String tone;

        /** 指标值。 */
        private String value;
    }

    /**
     * 供应商指标。
     */
    @Data
    public static class ProviderMetric {

        /** 分类。 */
        private String category;

        /** 数量。 */
        private Long count;

        /** 供应商名称。 */
        private String provider;
    }

    /**
     * 最近活动。
     */
    @Data
    public static class RecentActivity {

        /** 活动描述。 */
        private String action;

        /** 活动时间。 */
        private Long createTime;

        /** 操作耗时。 */
        private Long duration;

        /** 操作地址。 */
        private String url;

        /** 操作用户。 */
        private String username;
    }

    /**
     * 趋势序列。
     */
    @Data
    public static class TrendSeries {

        /** 序列键。 */
        private String key;

        /** 序列名称。 */
        private String label;

        /** 数值单位。 */
        private String unit;

        /** 折线点位。 */
        private List<TrendPoint> points = new ArrayList<>();
    }

    /**
     * 趋势点位。
     */
    @Data
    public static class TrendPoint {

        /** 日期标签（示例：03-27）。 */
        private String date;

        /** 指标值。 */
        private Long value;
    }
}
