<script lang="ts" setup>
import type {
  ModelCallDistributionItem,
  ModelCallMetricCard,
  ModelCallOverview,
  ModelCallRecentItem,
  ModelCallTrendPoint,
  ModelCallUsageItem,
} from '#/api/core/monitor';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import {
  Activity,
  Cpu,
  RefreshCcw,
  Server,
  TriangleAlert,
  Zap,
} from '@vben/icons';
import { $t } from '@vben/locales';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

import {
  NButton,
  NDataTable,
  NEmpty,
  NProgress,
  NRadioButton,
  NRadioGroup,
  NSpin,
  NTag,
} from 'naive-ui';

import { getModelCallOverviewApi } from '#/api/core/monitor';
import LcCard from '#/components/LcCard/index.vue';

const loading = ref(false);
const windowDays = ref(7);
const overview = ref<ModelCallOverview>({});

const trendChartRef = ref();
const callTypeChartRef = ref();
const providerChartRef = ref();
const { renderEcharts: renderTrendChart } = useEcharts(trendChartRef);
const { renderEcharts: renderCallTypeChart } = useEcharts(callTypeChartRef);
const { renderEcharts: renderProviderChart } = useEcharts(providerChartRef);

/** 统计卡片文案来自后端埋点，这里按已知枚举值映射到本地化键，未命中时原样展示。 */
const METRIC_LABEL_KEYS: Record<string, string> = {
  调用总量: 'monitor.metrics.totalCalls',
  'Token 消耗': 'monitor.metrics.tokens',
  平均耗时: 'monitor.metrics.avgDuration',
  调用成功率: 'monitor.metrics.successRate',
};

/** 统计卡片 hint 来自后端埋点，按已知枚举值映射到本地化键，未命中时原样展示。 */
const METRIC_HINT_KEYS: Record<string, string> = {
  统计窗口内的模型调用次数: 'monitor.hints.totalCalls',
  '输入与输出 Token 合计': 'monitor.hints.tokens',
  单次模型调用的平均响应时间: 'monitor.hints.avgDuration',
  当前窗口无失败调用: 'monitor.hints.successRate',
};

function metricHint(hint?: string) {
  const key = METRIC_HINT_KEYS[String(hint ?? '')];
  return key ? $t(key) : String(hint ?? '');
}

const CALL_TYPE_LABEL_KEYS: Record<string, string> = {
  CHAT: 'monitor.callType.CHAT',
  EMBEDDING: 'monitor.callType.EMBEDDING',
  IMAGE: 'monitor.callType.IMAGE',
  OCR: 'monitor.callType.OCR',
};

const SCENE_LABEL_KEYS: Record<string, string> = {
  AGENT_CHAT: 'monitor.scene.AGENT_CHAT',
  DATA_ANALYSIS: 'monitor.scene.DATA_ANALYSIS',
  KNOWLEDGE_INDEX: 'monitor.scene.KNOWLEDGE_INDEX',
  KNOWLEDGE_RETRIEVE: 'monitor.scene.KNOWLEDGE_RETRIEVE',
  IMAGE_GENERATE: 'monitor.scene.IMAGE_GENERATE',
  IMAGE_OCR: 'monitor.scene.IMAGE_OCR',
  unknown: 'monitor.scene.unknown',
};

const windowOptions = computed(() =>
  [7, 14, 30].map((days) => ({
    label: $t('monitor.window.days', { days }),
    value: days,
  })),
);

const metrics = computed<ModelCallMetricCard[]>(() => overview.value.metrics ?? []);
const trend = computed<ModelCallTrendPoint[]>(() => overview.value.trend ?? []);
const callTypeDistribution = computed<ModelCallDistributionItem[]>(
  () => overview.value.callTypeDistribution ?? [],
);
const providerDistribution = computed<ModelCallDistributionItem[]>(
  () => overview.value.providerDistribution ?? [],
);
const topModels = computed<ModelCallUsageItem[]>(() => overview.value.topModels ?? []);
const recentCalls = computed<ModelCallRecentItem[]>(() => overview.value.recentCalls ?? []);

/** 模型消耗排行的最大值，用于计算进度条比例。 */
const maxModelToken = computed(() =>
  Math.max(1, ...topModels.value.map((item) => Number(item.tokenCount ?? 0))),
);

const recentColumns = computed(() => [
  {
    key: 'modelName',
    title: $t('monitor.columns.model'),
    width: 160,
    ellipsis: { tooltip: true },
  },
  { key: 'provider', title: $t('monitor.columns.provider'), width: 110 },
  { key: 'callType', title: $t('monitor.columns.type'), width: 100 },
  { key: 'scene', title: $t('monitor.columns.scene'), width: 130 },
  { key: 'totalToken', title: $t('monitor.chart.tokensLegend'), width: 90 },
  { key: 'duration', title: $t('monitor.columns.duration'), width: 90 },
  { key: 'status', title: $t('monitor.columns.status'), width: 90 },
  { key: 'createTime', title: $t('monitor.columns.createTime'), width: 160 },
]);

function resolveMetricIcon(label?: string) {
  const key = METRIC_LABEL_KEYS[String(label ?? '')];
  switch (key) {
    case 'monitor.metrics.avgDuration': {
      return Activity;
    }
    case 'monitor.metrics.successRate': {
      return TriangleAlert;
    }
    case 'monitor.metrics.tokens': {
      return Zap;
    }
    default: {
      return Cpu;
    }
  }
}

function metricLabel(label?: string) {
  const key = METRIC_LABEL_KEYS[String(label ?? '')];
  return key ? $t(key) : String(label ?? '');
}

function resolveToneType(tone?: string) {
  switch (tone) {
    case 'error': {
      return 'error';
    }
    case 'success': {
      return 'success';
    }
    case 'warning': {
      return 'warning';
    }
    default: {
      return 'primary';
    }
  }
}

function callTypeLabel(value?: string) {
  const key = CALL_TYPE_LABEL_KEYS[String(value ?? '')];
  return key ? $t(key) : String(value ?? '--');
}

function sceneLabel(value?: string) {
  const key = SCENE_LABEL_KEYS[String(value ?? '')];
  return key ? $t(key) : String(value ?? '--');
}

function formatNumber(value?: null | number) {
  if (value === null || value === undefined) {
    return '0';
  }
  return Number(value).toLocaleString('zh-CN');
}

function formatDuration(value?: null | number) {
  return value === null || value === undefined ? '--' : `${value} ms`;
}

function formatTime(timestamp?: number) {
  if (!timestamp) {
    return '--';
  }
  return new Date(timestamp).toLocaleString('zh-CN', { hour12: false });
}

/** 表格数据在渲染前做一次展示态转换，避免在模板里写判断逻辑。 */
const recentRows = computed(() =>
  recentCalls.value.map((item) => ({
    ...item,
    callType: callTypeLabel(item.callType),
    scene: sceneLabel(item.scene),
    totalToken: formatNumber(item.totalToken),
    duration: formatDuration(item.duration),
    createTime: formatTime(item.createTime),
  })),
);

function buildTrendOption(points: ModelCallTrendPoint[]) {
  const dates = points.map((item) => item.date || '--');
  const callsLegend = $t('monitor.chart.callsLegend');
  const tokensLegend = $t('monitor.chart.tokensLegend');
  return {
    tooltip: { trigger: 'axis' },
    legend: {
      data: [callsLegend, tokensLegend],
      top: 0,
      textStyle: { color: '#64748b', fontSize: 11 },
    },
    grid: { top: 34, right: 20, bottom: 24, left: 52 },
    xAxis: {
      type: 'category',
      data: dates,
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 11 },
      axisLine: { lineStyle: { color: '#cbd5e1' } },
    },
    yAxis: [
      {
        type: 'value',
        name: $t('monitor.chart.countSeries'),
        nameTextStyle: { color: '#94a3b8', fontSize: 10 },
        axisLabel: { color: '#64748b', fontSize: 11 },
        splitLine: { lineStyle: { color: '#e2e8f0' } },
      },
      {
        type: 'value',
        name: tokensLegend,
        nameTextStyle: { color: '#94a3b8', fontSize: 10 },
        axisLabel: { color: '#64748b', fontSize: 11 },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: callsLegend,
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: points.map((item) => item.callCount ?? 0),
        lineStyle: { color: '#2563eb', width: 2.5 },
        itemStyle: { color: '#2563eb' },
        areaStyle: { opacity: 0.14 },
      },
      {
        name: tokensLegend,
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        yAxisIndex: 1,
        data: points.map((item) => item.tokenCount ?? 0),
        lineStyle: { color: '#0ea5e9', width: 2.5 },
        itemStyle: { color: '#0ea5e9' },
      },
    ],
  };
}

function buildDistributionOption(items: ModelCallDistributionItem[]) {
  if (items.length === 0) {
    return {
      title: {
        text: $t('monitor.chart.empty'),
        left: 'center',
        top: 'center',
        textStyle: { color: '#94a3b8', fontSize: 12, fontWeight: 'normal' },
      },
      series: [],
    };
  }
  return {
    tooltip: { trigger: 'item' },
    legend: {
      bottom: 0,
      textStyle: { color: '#64748b', fontSize: 11 },
    },
    series: [
      {
        type: 'pie',
        radius: ['42%', '66%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { color: '#475569', fontSize: 11 },
        data: items.map((item) => ({
          // 调用类型走中文映射，供应商名称直接展示
          name: resolveDistributionName(item),
          value: item.callCount ?? 0,
        })),
      },
    ],
  };
}

function resolveDistributionName(item: ModelCallDistributionItem) {
  const raw = String(item.name ?? '');
  const key = CALL_TYPE_LABEL_KEYS[raw];
  return key ? $t(key) : raw || $t('monitor.chart.unknown');
}

async function renderCharts() {
  await Promise.all([
    renderTrendChart(buildTrendOption(trend.value) as never),
    renderCallTypeChart(buildDistributionOption(callTypeDistribution.value) as never),
    renderProviderChart(buildDistributionOption(providerDistribution.value) as never),
  ]);
}

async function loadOverview() {
  loading.value = true;
  try {
    overview.value = await getModelCallOverviewApi(windowDays.value);
    await renderCharts();
  } finally {
    loading.value = false;
  }
}

async function handleWindowChange(value: number) {
  windowDays.value = value;
  await loadOverview();
}

onMounted(loadOverview);
</script>

<template>
  <Page>
    <div class="flex flex-col gap-3">
      <!-- 顶部操作栏 -->
      <div
        class="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border bg-card px-4 py-3"
      >
        <div class="min-w-0">
          <div class="text-base font-semibold text-foreground">
            {{ $t('monitor.title') }}
          </div>
          <div class="mt-0.5 text-xs text-muted-foreground">
            {{ $t('monitor.description') }}
          </div>
        </div>
        <div class="flex items-center gap-2">
          <NRadioGroup
            :value="windowDays"
            size="small"
            @update:value="handleWindowChange"
          >
            <NRadioButton
              v-for="item in windowOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </NRadioButton>
          </NRadioGroup>
          <NButton :loading="loading" secondary size="small" @click="loadOverview">
            <RefreshCcw class="size-3.5" />
            {{ $t('common.actions.refresh') }}
          </NButton>
        </div>
      </div>

      <NSpin :show="loading">
        <!-- 指标卡片 -->
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <LcCard
            v-for="item in metrics"
            :key="item.label"
            :hoverable="false"
            :icon-component="resolveMetricIcon(item.label)"
          >
            <template #header>
              <div class="min-w-0">
                <div class="truncate text-[13px] font-semibold text-foreground">
                  {{ metricLabel(item.label) }}
                </div>
                <div class="mt-0.5 text-[9px] leading-4 text-muted-foreground">
                  {{ metricHint(item.hint) }}
                </div>
              </div>
            </template>

            <div class="flex items-end justify-between gap-3">
              <div class="text-2xl font-semibold text-foreground">
                {{ item.value }}
              </div>
              <NTag
                :bordered="false"
                :type="resolveToneType(item.tone)"
                class="shrink-0"
                round
                size="small"
              >
                {{
                  $t('monitor.window.days', { days: windowDays })
                }}
              </NTag>
            </div>
          </LcCard>
        </div>

        <!-- 趋势图 -->
        <div class="mt-3 rounded-lg border border-border bg-card p-4">
          <div class="mb-2 flex items-center justify-between gap-3">
            <div>
              <div class="text-sm font-semibold text-foreground">
                {{ $t('monitor.trend.title') }}
              </div>
              <div class="mt-0.5 text-xs text-muted-foreground">
                {{ $t('monitor.trend.description') }}
              </div>
            </div>
          </div>
          <EchartsUI ref="trendChartRef" height="300px" />
        </div>

        <!-- 分布图 -->
        <div class="mt-3 grid gap-3 lg:grid-cols-2">
          <div class="rounded-lg border border-border bg-card p-4">
            <div class="mb-2">
              <div class="text-sm font-semibold text-foreground">
                {{ $t('monitor.typeDistribution.title') }}
              </div>
              <div class="mt-0.5 text-xs text-muted-foreground">
                {{ $t('monitor.typeDistribution.description') }}
              </div>
            </div>
            <EchartsUI ref="callTypeChartRef" height="260px" />
          </div>

          <div class="rounded-lg border border-border bg-card p-4">
            <div class="mb-2">
              <div class="text-sm font-semibold text-foreground">
                {{ $t('monitor.providerDistribution.title') }}
              </div>
              <div class="mt-0.5 text-xs text-muted-foreground">
                {{ $t('monitor.providerDistribution.description') }}
              </div>
            </div>
            <EchartsUI ref="providerChartRef" height="260px" />
          </div>
        </div>

        <!-- 模型消耗排行 -->
        <div class="mt-3 rounded-lg border border-border bg-card p-4">
          <div class="mb-3 flex items-center justify-between gap-3">
            <div>
              <div class="text-sm font-semibold text-foreground">
                {{ $t('monitor.ranking.title') }}
              </div>
              <div class="mt-0.5 text-xs text-muted-foreground">
                {{ $t('monitor.ranking.description') }}
              </div>
            </div>
            <Server class="size-4 text-muted-foreground" />
          </div>

          <div v-if="topModels.length > 0" class="space-y-3">
            <div v-for="item in topModels" :key="item.modelId" class="space-y-1.5">
              <div class="flex items-center justify-between gap-3 text-xs">
                <div class="flex min-w-0 items-center gap-2">
                  <span class="truncate font-medium text-foreground">
                    {{ item.modelName || $t('models.card.unnamed') }}
                  </span>
                  <NTag :bordered="false" size="tiny">{{ item.provider }}</NTag>
                </div>
                <div class="flex shrink-0 items-center gap-3 text-muted-foreground">
                  <span>{{
                    $t('monitor.ranking.calls', { count: formatNumber(item.callCount) })
                  }}</span>
                  <span>{{
                    $t('monitor.chart.tokensLegend')
                  }} {{ formatNumber(item.tokenCount) }}</span>
                  <span>{{ formatDuration(item.avgDuration) }}</span>
                  <span v-if="Number(item.errorCount ?? 0) > 0" class="text-destructive">
                    {{
                      $t('monitor.ranking.failed', { count: item.errorCount })
                    }}
                  </span>
                </div>
              </div>
              <NProgress
                :percentage="
                  Math.max(
                    2,
                    Math.round(
                      (Number(item.tokenCount ?? 0) / maxModelToken) * 100,
                    ),
                  )
                "
                :show-indicator="false"
                :height="6"
                color="#2563eb"
              />
            </div>
          </div>
          <NEmpty v-else class="py-10" :description="$t('monitor.chart.empty')" />
        </div>

        <!-- 最近调用 -->
        <div class="mt-3 rounded-lg border border-border bg-card p-4">
          <div class="mb-3">
            <div class="text-sm font-semibold text-foreground">
              {{ $t('monitor.recent.title') }}
            </div>
            <div class="mt-0.5 text-xs text-muted-foreground">
              {{ $t('monitor.recent.description') }}
            </div>
          </div>
          <NDataTable
            v-if="recentRows.length > 0"
            :columns="recentColumns"
            :data="recentRows"
            :pagination="false"
            :scroll-x="920"
            :single-line="false"
            size="small"
          >
            <template #empty>
              <NEmpty :description="$t('monitor.chart.empty')" />
            </template>
          </NDataTable>
          <NEmpty v-else class="py-10" :description="$t('monitor.chart.empty')" />
        </div>
      </NSpin>
    </div>
  </Page>
</template>
