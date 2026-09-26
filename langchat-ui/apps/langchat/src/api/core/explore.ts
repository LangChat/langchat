import { requestClient } from '#/api/request';

/**
 * Explore 指标卡片。
 */
export interface ExploreMetricCard {
  description?: string;
  hint?: string;
  label?: string;
  tone?: string;
  value?: string;
}

/**
 * Explore 供应商统计。
 */
export interface ExploreProviderMetric {
  category?: string;
  count?: number;
  provider?: string;
}

/**
 * Explore 最近活动。
 */
export interface ExploreRecentActivity {
  action?: string;
  createTime?: number;
  duration?: number;
  url?: string;
  username?: string;
}

/**
 * Explore 概览响应。
 */
export interface ExploreOverview {
  coreMetrics?: ExploreMetricCard[];
  documentMetrics?: ExploreMetricCard[];
  governanceMetrics?: ExploreMetricCard[];
  providerMetrics?: ExploreProviderMetric[];
  recentActivities?: ExploreRecentActivity[];
  trendSeries?: ExploreTrendSeries[];
}

/**
 * Explore 趋势点位。
 */
export interface ExploreTrendPoint {
  date?: string;
  value?: number;
}

/**
 * Explore 趋势序列。
 */
export interface ExploreTrendSeries {
  key?: string;
  label?: string;
  points?: ExploreTrendPoint[];
  unit?: string;
}

/**
 * 查询 Explore 概览。
 */
export async function getExploreOverviewApi() {
  return requestClient.get<ExploreOverview>('/v1/explore/overview');
}
