<script lang="ts" setup>
import type {AigcMessage} from '#/api/aigc/chat';

import {computed, onMounted, ref, watch} from 'vue';

import {$t, i18n} from '@vben/locales';
import {EchartsUI, useEcharts} from '@vben/plugins/echarts';

import {NButton, NSpin, NTag} from 'naive-ui';

import {listConversationApi, listConversationMessagesApi,} from '#/api/aigc/chat';

interface Props {
  agentId?: string;
}

const props = withDefaults(defineProps<Props>(), {
  agentId: '',
});

const loading = ref(false);
const messageRows = ref<AigcMessage[]>([]);
const trendChartRef = ref();
const roleChartRef = ref();
const tokenChartRef = ref();
const { renderEcharts: renderTrendChart } = useEcharts(trendChartRef);
const { renderEcharts: renderRoleChart } = useEcharts(roleChartRef);
const { renderEcharts: renderTokenChart } = useEcharts(tokenChartRef);

const currentLocale = computed(
  () => i18n.global.locale.value || 'zh-CN',
);

const totalMessages = computed(() => messageRows.value.length);
const totalInputTokens = computed(() =>
  messageRows.value.reduce(
    (sum, item) => sum + Number(item.inputToken || 0),
    0,
  ),
);
const totalOutputTokens = computed(() =>
  messageRows.value.reduce(
    (sum, item) => sum + Number(item.outputToken || 0),
    0,
  ),
);

const dayPoints = computed(() => {
  const map = new Map<string, number>();
  messageRows.value.forEach((item) => {
    const ts = item.createTime ? new Date(item.createTime) : null;
    const key = ts
      ? ts.toLocaleDateString(currentLocale.value)
      : $t('common.status.unknown');
    map.set(key, (map.get(key) || 0) + 1);
  });
  return [...map.entries()]
    .map(([date, value]) => ({ date, value }))
    .toSorted((a, b) => a.date.localeCompare(b.date));
});

const rolePoints = computed(() => {
  const map = new Map<string, number>();
  messageRows.value.forEach((item) => {
    const key = String(item.role || 'unknown');
    map.set(key, (map.get(key) || 0) + 1);
  });
  return [...map.entries()].map(([name, value]) => ({ name, value }));
});

const tokenPoints = computed(() => {
  const map = new Map<string, { in: number; out: number }>();
  messageRows.value.forEach((item) => {
    const ts = item.createTime ? new Date(item.createTime) : null;
    const key = ts
      ? ts.toLocaleDateString(currentLocale.value)
      : $t('common.status.unknown');
    const current = map.get(key) || { in: 0, out: 0 };
    current.in += Number(item.inputToken || 0);
    current.out += Number(item.outputToken || 0);
    map.set(key, current);
  });
  return [...map.entries()]
    .map(([date, value]) => ({
      date,
      input: value.in,
      output: value.out,
    }))
    .toSorted((a, b) => a.date.localeCompare(b.date));
});

const chartDayPoints = computed(() =>
  dayPoints.value.length > 0
    ? dayPoints.value
    : [{ date: $t('common.empty.noData'), value: 0 }],
);
const chartRolePoints = computed(() =>
  rolePoints.value.length > 0
    ? rolePoints.value
    : [
        { name: 'user', value: 0 },
        { name: 'assistant', value: 0 },
      ],
);
const chartTokenPoints = computed(() =>
  tokenPoints.value.length > 0
    ? tokenPoints.value
    : [
        {
          date: $t('common.empty.noData'),
          input: 0,
          output: 0,
        },
      ],
);

async function loadRows() {
  if (!props.agentId) {
    messageRows.value = [];
    await renderCharts();
    return;
  }
  loading.value = true;
  try {
    const conversations = await listConversationApi({
      agentId: props.agentId,
      pageNo: 1,
      pageSize: 30,
    });
    const groups = await Promise.all(
      conversations.map((item) =>
        item.id ? listConversationMessagesApi(item.id) : Promise.resolve([]),
      ),
    );
    messageRows.value = groups.flat();
    await renderCharts();
  } finally {
    loading.value = false;
  }
}

async function renderCharts() {
  await Promise.all([
    renderTrendChart({
      backgroundColor: 'transparent',
      grid: { top: 20, right: 18, bottom: 26, left: 42 },
      series: [
        {
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          data: chartDayPoints.value.map((item) => item.value),
          lineStyle: { color: '#2563eb', width: 2.5 },
          itemStyle: { color: '#2563eb' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(37,99,235,0.35)' },
                { offset: 1, color: 'rgba(37,99,235,0.04)' },
              ],
            },
          },
        },
      ],
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#ffffff',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: '#0f172a' },
      },
      xAxis: {
        type: 'category',
        data: chartDayPoints.value.map((item) => item.date),
        axisTick: { show: false },
        axisLabel: { color: '#64748b', fontSize: 11 },
        axisLine: { lineStyle: { color: '#cbd5e1' } },
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#64748b', fontSize: 11 },
        splitLine: { lineStyle: { color: '#e2e8f0' } },
      },
    } as never),
    renderRoleChart({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        backgroundColor: '#ffffff',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: '#0f172a' },
      },
      legend: {
        bottom: 0,
        textStyle: { color: '#64748b', fontSize: 11 },
      },
      series: [
        {
          type: 'pie',
          radius: ['42%', '72%'],
          data: chartRolePoints.value,
          label: { formatter: '{b}: {d}%' },
          itemStyle: {
            borderColor: 'rgba(255,255,255,0.5)',
            borderWidth: 1,
          },
        },
      ],
    } as never),
    renderTokenChart({
      backgroundColor: 'transparent',
      grid: { top: 20, right: 18, bottom: 26, left: 42 },
      legend: {
        top: 0,
        textStyle: { color: '#64748b', fontSize: 11 },
      },
      series: [
        {
          name: $t('agents.messageStats.inputTokens'),
          type: 'bar',
          data: chartTokenPoints.value.map((item) => item.input),
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: '#38bdf8' },
                { offset: 1, color: '#0284c7' },
              ],
            },
          },
          barMaxWidth: 26,
        },
        {
          name: $t('agents.messageStats.outputTokens'),
          type: 'bar',
          data: chartTokenPoints.value.map((item) => item.output),
          itemStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [
                { offset: 0, color: '#4ade80' },
                { offset: 1, color: '#15803d' },
              ],
            },
          },
          barMaxWidth: 26,
        },
      ],
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#ffffff',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: '#0f172a' },
      },
      xAxis: {
        type: 'category',
        data: chartTokenPoints.value.map((item) => item.date),
        axisTick: { show: false },
        axisLabel: { color: '#64748b', fontSize: 11 },
        axisLine: { lineStyle: { color: '#cbd5e1' } },
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#64748b', fontSize: 11 },
        splitLine: { lineStyle: { color: '#e2e8f0' } },
      },
    } as never),
  ]);
}

watch(
  () => props.agentId,
  async () => {
    await loadRows();
  },
  { immediate: true },
);

watch([dayPoints, rolePoints, tokenPoints], () => {
  void renderCharts();
});

onMounted(async () => {
  await renderCharts();
});
</script>

<template>
  <div
    class="overflow-y-auto rounded-xl border border-border bg-card p-3"
  >
    <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
      <div class="text-sm font-semibold text-foreground">
        {{ $t('agents.messageStats.title') }}
      </div>
      <div class="flex items-center gap-2">
        <NTag :bordered="false" round type="info">
          {{ $t('agents.messageStats.totalMessages', { count: totalMessages }) }}
        </NTag>
        <NTag :bordered="false" round type="success">
          {{
            $t('agents.messageStats.totalInputTokens', {
              count: totalInputTokens,
            })
          }}
        </NTag>
        <NTag :bordered="false" round>
          {{
            $t('agents.messageStats.totalOutputTokens', {
              count: totalOutputTokens,
            })
          }}
        </NTag>
        <NButton :loading="loading" secondary size="small" @click="loadRows">
          {{ $t('agents.messageStats.refresh') }}
        </NButton>
      </div>
    </div>

    <NSpin :show="loading">
      <div class="grid gap-3 xl:grid-cols-2">
        <div class="rounded-lg border border-border/80 bg-background p-3">
          <div class="mb-2 text-xs font-medium text-muted-foreground">
            {{ $t('agents.messageStats.messageTrend') }}
          </div>
          <EchartsUI ref="trendChartRef" height="280px" />
        </div>
        <div class="rounded-lg border border-border/80 bg-background p-3">
          <div class="mb-2 text-xs font-medium text-muted-foreground">
            {{ $t('agents.messageStats.roleDistribution') }}
          </div>
          <EchartsUI ref="roleChartRef" height="280px" />
        </div>
      </div>

      <div class="mt-3 rounded-lg border border-border/80 bg-background p-3">
        <div class="mb-2 text-xs font-medium text-muted-foreground">
          {{ $t('agents.messageStats.tokenTrend') }}
        </div>
        <EchartsUI ref="tokenChartRef" height="300px" />
      </div>
    </NSpin>
  </div>
</template>
