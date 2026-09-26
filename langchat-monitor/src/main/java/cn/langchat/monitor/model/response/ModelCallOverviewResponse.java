package cn.langchat.monitor.model.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 模型调用监控总览响应。
 *
 * @author LangChat Team
 * @since 2026/9/26
 */
@Data
public class ModelCallOverviewResponse {

    /** 统计窗口天数。 */
    private Integer windowDays;

    /** 指标卡片。 */
    private List<MetricCard> metrics = new ArrayList<>();

    /** 调用趋势（按天）。 */
    private List<TrendPoint> trend = new ArrayList<>();

    /** 供应商分布。 */
    private List<DistributionItem> providerDistribution = new ArrayList<>();

    /** 调用类型分布。 */
    private List<DistributionItem> callTypeDistribution = new ArrayList<>();

    /** 调用场景分布。 */
    private List<DistributionItem> sceneDistribution = new ArrayList<>();

    /** 模型消耗排行。 */
    private List<ModelUsageItem> topModels = new ArrayList<>();

    /** 最近调用记录。 */
    private List<RecentCallItem> recentCalls = new ArrayList<>();

    /**
     * 指标卡片。
     */
    @Data
    public static class MetricCard {

        /** 指标名称。 */
        private String label;

        /** 指标值。 */
        private String value;

        /** 补充说明。 */
        private String hint;

        /** 状态色：primary / success / warning / error。 */
        private String tone;
    }

    /**
     * 趋势点。
     */
    @Data
    public static class TrendPoint {

        /** 日期，格式 MM-dd。 */
        private String date;

        /** 调用次数。 */
        private Long callCount;

        /** Token 消耗。 */
        private Long tokenCount;
    }

    /**
     * 分布项。
     */
    @Data
    public static class DistributionItem {

        /** 维度名称。 */
        private String name;

        /** 调用次数。 */
        private Long callCount;

        /** Token 消耗。 */
        private Long tokenCount;
    }

    /**
     * 模型消耗项。
     */
    @Data
    public static class ModelUsageItem {

        /** 模型 ID。 */
        private String modelId;

        /** 模型名称。 */
        private String modelName;

        /** 供应商。 */
        private String provider;

        /** 调用次数。 */
        private Long callCount;

        /** Token 消耗。 */
        private Long tokenCount;

        /** 平均耗时，单位毫秒。 */
        private Long avgDuration;

        /** 失败次数。 */
        private Long errorCount;
    }

    /**
     * 最近调用记录。
     */
    @Data
    public static class RecentCallItem {

        /** 模型名称。 */
        private String modelName;

        /** 供应商。 */
        private String provider;

        /** 调用类型。 */
        private String callType;

        /** 调用场景。 */
        private String scene;

        /** 状态。 */
        private String status;

        /** Token 消耗。 */
        private Integer totalToken;

        /** 耗时，单位毫秒。 */
        private Long duration;

        /** 调用时间。 */
        private Long createTime;
    }
}
