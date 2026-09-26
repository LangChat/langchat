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

const windowOptions = [
  { label: '近 7 天', value: 7 },
  { label: '近 14 天', value: 14 },
  { label: '近 30 天', value: 30 },
];

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

const CALL_TYPE_LABELS: Record<string, string> = {
  CHAT: '对话模型',
  EMBEDDING: '向量模型',
  IMAGE: '文生图',
  OCR: '图像识别',
};

const SCENE_LABELS: Record<string, string> = {
  AGENT_CHAT: 'Agent 对话',
  DATA_ANALYSIS: '智能问数',
  KNOWLEDGE_INDEX: '知识库向量化',
  KNOWLEDGE_RETRIEVE: '知识库检索',
  IMAGE_GENERATE: '文生图',
  IMAGE_OCR: '图像识别',
  unknown: '未标注',
};

const recentColumns = [
  { key: 'modelName', title: '模型', width: 160, ellipsis: { tooltip: true } },
  { key: 'provider', title: '供应商', width: 110 },
  { key: 'callType', title: '类型', width: 100 },
  { key: 'scene', title: '场景', width: 130 },
  { key: 'totalToken', title: 'Token', width: 90 },
  { key: 'duration', title: '耗时', width: 90 },
  { key: 'status', title: '状态', width: 90 },
  { key: 'createTime', title: '调用时间', width: 160 },
];

function resolveMetricIcon(label?: string) {
  switch (label) {
    case 'Token 消耗': {
      return Zap;
    }
    case '平均耗时': {
      return Activity;
    }
    case '调用成功率': {
      return TriangleAlert;
    }
    default: {
      return Cpu;
    }
  }
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
  return CALL_TYPE_LABELS[String(value ?? '')] ?? String(value ?? '--');
}

function sceneLabel(value?: string) {
  return SCENE_LABELS[String(value ?? '')] ?? String(value ?? '--');
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
  return {
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['调用次数', 'Token 消耗'],
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
        name: '次数',
        nameTextStyle: { color: '#94a3b8', fontSize: 10 },
        axisLabel: { color: '#64748b', fontSize: 11 },
        splitLine: { lineStyle: { color: '#e2e8f0' } },
      },
      {
        type: 'value',
        name: 'Token',
        nameTextStyle: { color: '#94a3b8', fontSize: 10 },
        axisLabel: { color: '#64748b', fontSize: 11 },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '调用次数',
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
        name: 'Token 消耗',
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
        text: '暂无调用数据',
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
  const raw = String(item.name ?? '未知');
  return CALL_TYPE_LABELS[raw] ?? raw;
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
          <div class="text-base font-semibold text-foreground">模型调用监控</div>
          <div class="mt-0.5 text-xs text-muted-foreground">
            统计所有模型调用的次数、Token 消耗与耗时，数据来自模型调用埋点日志。
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
            刷新
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
                  {{ item.label }}
                </div>
                <div class="mt-0.5 text-[9px] leading-4 text-muted-foreground">
                  {{ item.hint }}
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
                近 {{ windowDays }} 天
              </NTag>
            </div>
          </LcCard>
        </div>

        <!-- 趋势图 -->
        <div class="mt-3 rounded-lg border border-border bg-card p-4">
          <div class="mb-2 flex items-center justify-between gap-3">
            <div>
              <div class="text-sm font-semibold text-foreground">调用趋势</div>
              <div class="mt-0.5 text-xs text-muted-foreground">
                按天统计调用次数与 Token 消耗
              </div>
            </div>
          </div>
          <EchartsUI ref="trendChartRef" height="300px" />
        </div>

        <!-- 分布图 -->
        <div class="mt-3 grid gap-3 lg:grid-cols-2">
          <div class="rounded-lg border border-border bg-card p-4">
            <div class="mb-2">
              <div class="text-sm font-semibold text-foreground">调用类型分布</div>
              <div class="mt-0.5 text-xs text-muted-foreground">
                对话 / 向量 / 文生图 / 图像识别的调用占比
              </div>
            </div>
            <EchartsUI ref="callTypeChartRef" height="260px" />
          </div>

          <div class="rounded-lg border border-border bg-card p-4">
            <div class="mb-2">
              <div class="text-sm font-semibold text-foreground">供应商分布</div>
              <div class="mt-0.5 text-xs text-muted-foreground">
                各模型供应商的调用占比
              </div>
            </div>
            <EchartsUI ref="providerChartRef" height="260px" />
          </div>
        </div>

        <!-- 模型消耗排行 -->
        <div class="mt-3 rounded-lg border border-border bg-card p-4">
          <div class="mb-3 flex items-center justify-between gap-3">
            <div>
              <div class="text-sm font-semibold text-foreground">模型消耗排行</div>
              <div class="mt-0.5 text-xs text-muted-foreground">
                按 Token 消耗倒序展示调用量最高的模型
              </div>
            </div>
            <Server class="size-4 text-muted-foreground" />
          </div>

          <div v-if="topModels.length > 0" class="space-y-3">
            <div v-for="item in topModels" :key="item.modelId" class="space-y-1.5">
              <div class="flex items-center justify-between gap-3 text-xs">
                <div class="flex min-w-0 items-center gap-2">
                  <span class="truncate font-medium text-foreground">
                    {{ item.modelName || '未命名模型' }}
                  </span>
                  <NTag :bordered="false" size="tiny">{{ item.provider }}</NTag>
                </div>
                <div class="flex shrink-0 items-center gap-3 text-muted-foreground">
                  <span>调用 {{ formatNumber(item.callCount) }}</span>
                  <span>Token {{ formatNumber(item.tokenCount) }}</span>
                  <span>{{ formatDuration(item.avgDuration) }}</span>
                  <span v-if="Number(item.errorCount ?? 0) > 0" class="text-destructive">
                    失败 {{ item.errorCount }}
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
          <NEmpty v-else class="py-10" description="暂无调用数据" />
        </div>

        <!-- 最近调用 -->
        <div class="mt-3 rounded-lg border border-border bg-card p-4">
          <div class="mb-3">
            <div class="text-sm font-semibold text-foreground">最近调用记录</div>
            <div class="mt-0.5 text-xs text-muted-foreground">
              最近 10 条模型调用明细
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
              <NEmpty description="暂无调用数据" />
            </template>
          </NDataTable>
          <NEmpty v-else class="py-10" description="暂无调用数据" />
        </div>
      </NSpin>
    </div>
  </Page>
</template>
