<script lang="ts" setup>
import type {ExploreOverview, ExploreTrendPoint} from '#/api/core/explore';

import {computed, onMounted, ref, watch} from 'vue';
import {useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {Box, Coins, Globe, LayoutGrid, MessageSquare, UserRound} from '@vben/icons';
import {EchartsUI, useEcharts} from '@vben/plugins/echarts';
import {preferences, usePreferences} from '@vben/preferences';

import {NSpin, NTag} from 'naive-ui';

import {getExploreOverviewApi} from '#/api/core/explore';
import LcCard from '#/components/LcCard/index.vue';

const router = useRouter();
const { isDark } = usePreferences();
const appName = computed(() => preferences.app.name);
const logoSrc = computed(() =>
  isDark.value && preferences.logo.sourceDark
    ? preferences.logo.sourceDark
    : preferences.logo.source,
);
const loading = ref(false);
const overview = ref<ExploreOverview>({});
const coreChartRef = ref();
const governanceChartRef = ref();
const activityChartRef = ref();
const { renderEcharts: renderCoreChart } = useEcharts(coreChartRef);
const { renderEcharts: renderGovernanceChart } = useEcharts(governanceChartRef);
const { renderEcharts: renderActivityChart } = useEcharts(activityChartRef);

const coreMetrics = computed(() => overview.value.coreMetrics ?? []);
const trendSeries = computed(() => overview.value.trendSeries ?? []);
const displayedCoreMetrics = computed(() => coreMetrics.value.slice(0, 4));

const messageTrendData = computed(() => getSeriesPoints('message'));
const tokenTrendData = computed(() => getSeriesPoints('token'));
const userTrendData = computed(() => getSeriesPoints('user'));

async function loadOverview() {
  loading.value = true;
  try {
    overview.value = await getExploreOverviewApi();
    await renderCharts();
  } finally {
    loading.value = false;
  }
}

function openRoute(path: string) {
  void router.push(path);
}

function resolveMetricRoute(label?: string) {
  switch (label) {
    case 'Token 消耗量':
    case '消息数量': {
      return '/chat';
    }
    case '应用数量': {
      return '/agents';
    }
    case '用户数量': {
      return '/permissions/users';
    }
    default: {
      return '/explore';
    }
  }
}

function resolveMetricIcon(label?: string) {
  switch (label) {
    case 'Token 消耗量': {
      return Coins;
    }
    case '应用数量': {
      return LayoutGrid;
    }
    case '消息数量': {
      return MessageSquare;
    }
    case '用户数量': {
      return UserRound;
    }
    default: {
      return Box;
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

function getSeriesPoints(key: string): ExploreTrendPoint[] {
  const found = trendSeries.value.find((item) => item.key === key);
  return found?.points ?? [];
}

function buildLineChartOption(data: ExploreTrendPoint[], color: string) {
  return {
    tooltip: { trigger: 'axis' },
    grid: { top: 18, right: 16, bottom: 24, left: 42 },
    xAxis: {
      type: 'category',
      data: data.map((item) => item.date || '--'),
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 11 },
      axisLine: { lineStyle: { color: '#cbd5e1' } },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#64748b', fontSize: 11 },
      splitLine: { lineStyle: { color: '#e2e8f0' } },
    },
    series: [
      {
        type: 'line',
        data: data.map((item) => item.value ?? 0),
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { color, width: 2.5 },
        itemStyle: { color },
        areaStyle: { opacity: 0.16 },
      },
    ],
  };
}

function getCoreChartOption() {
  return buildLineChartOption(messageTrendData.value, '#2563eb');
}

function getGovernanceChartOption() {
  return buildLineChartOption(tokenTrendData.value, '#0ea5e9');
}

function getActivityChartOption() {
  return buildLineChartOption(userTrendData.value, '#16a34a');
}

async function renderCharts() {
  const coreOption = getCoreChartOption();
  const governanceOption = getGovernanceChartOption();
  const activityOption = getActivityChartOption();
  await Promise.all([
    renderCoreChart(coreOption as never),
    renderGovernanceChart(governanceOption as never),
    renderActivityChart(activityOption as never),
  ]);
}

watch([displayedCoreMetrics, trendSeries], () => {
  void renderCharts();
});

onMounted(async () => {
  await loadOverview();
});
</script>

<template>
  <Page>
    <div class="flex flex-col gap-2">
      <!-- 产品品牌横幅 -->
      <div
        class="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border bg-card px-4 py-3"
      >
        <div class="flex min-w-0 items-center gap-3">
          <img
            v-if="logoSrc"
            :alt="appName"
            :src="logoSrc"
            class="size-9 shrink-0 rounded-md"
          />
          <div class="min-w-0">
            <div class="text-base font-semibold text-foreground">
              {{ appName }}
            </div>
            <div class="mt-0.5 truncate text-xs text-muted-foreground">
              开箱即用的企业级 AIGC 应用平台
            </div>
          </div>
        </div>

        <div
          class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground"
        >
          <span>
            由
            <span class="font-medium text-foreground/80">LangChat Team</span>
            打造
          </span>
          <a
            class="inline-flex items-center gap-1 transition-colors hover:text-primary"
            href="https://langchat.cn"
            rel="noopener"
            target="_blank"
          >
            <Globe class="size-3.5" />
            langchat.cn
          </a>
        </div>
      </div>

      <NSpin :show="loading">
        <div
          class="grid gap-x-3 gap-y-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4"
        >
          <LcCard
            v-for="item in displayedCoreMetrics"
            :key="item.label"
            :hoverable="false"
            :icon-component="resolveMetricIcon(item.label)"
            class="cursor-pointer"
            @click="openRoute(resolveMetricRoute(item.label))"
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
                指标项
              </NTag>
            </div>
          </LcCard>
        </div>

        <div class="mt-2 space-y-4">
          <div class="grid gap-3 xl:grid-cols-2">
            <LcCard
              :hoverable="false"
              :icon-component="MessageSquare"
            >
              <template #header>
                <div class="text-[13px] font-semibold text-foreground">
                  消息趋势（近7天）
                </div>
              </template>
              <EchartsUI ref="coreChartRef" height="300px" />
            </LcCard>

            <LcCard :hoverable="false" :icon-component="Coins">
              <template #header>
                <div class="text-[13px] font-semibold text-foreground">
                  Token 消耗趋势（近7天）
                </div>
              </template>
              <EchartsUI ref="governanceChartRef" height="300px" />
            </LcCard>
          </div>

          <LcCard :hoverable="false" :icon-component="UserRound">
            <template #header>
              <div class="text-[13px] font-semibold text-foreground">
                用户趋势（近7天）
              </div>
            </template>
            <EchartsUI ref="activityChartRef" height="300px" />
          </LcCard>
        </div>
      </NSpin>
    </div>
  </Page>
</template>
