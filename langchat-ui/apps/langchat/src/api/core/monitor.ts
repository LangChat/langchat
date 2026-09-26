import { requestClient } from '#/api/request';

/**
 * 监控指标卡片。
 */
export interface ModelCallMetricCard {
  hint?: string;
  label?: string;
  tone?: string;
  value?: string;
}

/**
 * 调用趋势点。
 */
export interface ModelCallTrendPoint {
  callCount?: number;
  date?: string;
  tokenCount?: number;
}

/**
 * 分布项。
 */
export interface ModelCallDistributionItem {
  callCount?: number;
  name?: string;
  tokenCount?: number;
}

/**
 * 模型消耗项。
 */
export interface ModelCallUsageItem {
  avgDuration?: number;
  callCount?: number;
  errorCount?: number;
  modelId?: string;
  modelName?: string;
  provider?: string;
  tokenCount?: number;
}

/**
 * 最近调用记录。
 */
export interface ModelCallRecentItem {
  callType?: string;
  createTime?: number;
  duration?: number;
  modelName?: string;
  provider?: string;
  scene?: string;
  status?: string;
  totalToken?: number;
}

/**
 * 模型调用监控总览。
 */
export interface ModelCallOverview {
  callTypeDistribution?: ModelCallDistributionItem[];
  metrics?: ModelCallMetricCard[];
  providerDistribution?: ModelCallDistributionItem[];
  recentCalls?: ModelCallRecentItem[];
  sceneDistribution?: ModelCallDistributionItem[];
  topModels?: ModelCallUsageItem[];
  trend?: ModelCallTrendPoint[];
  windowDays?: number;
}

/**
 * 查询模型调用监控总览。
 */
export function getModelCallOverviewApi(windowDays?: number) {
  return requestClient.get<ModelCallOverview>('/v1/monitor/model-calls/overview', {
    params: { windowDays },
  });
}
