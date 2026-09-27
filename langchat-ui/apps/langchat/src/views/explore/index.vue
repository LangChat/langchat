<script lang="ts" setup>
import type {ExploreOverview, ExploreTrendPoint} from '#/api/core/explore';

import {computed, onMounted, ref, watch} from 'vue';
import {useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {
  Box,
  Coins,
  ExternalLink,
  Globe,
  LayoutGrid,
  MessageSquare,
  SvgGithubIcon,
  UserRound,
} from '@vben/icons';
import {$t} from '@vben/locales';
import {EchartsUI, useEcharts} from '@vben/plugins/echarts';
import {preferences, usePreferences} from '@vben/preferences';

import {NSpin, NTag} from 'naive-ui';

import {getExploreOverviewApi} from '#/api/core/explore';
import LcCard from '#/components/LcCard/index.vue';
import {
  LANGCHAT_PRODUCT_LINKS,
  LANGCHAT_PRODUCT_TEAM,
} from '#/constants/product';

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

/** 指标文案来自后端统计，这里按已知枚举值映射到本地化键，未命中时原样展示。 */
const METRIC_LABEL_KEYS: Record<string, string> = {
  'Token 消耗量': 'explore.metrics.tokens',
  '消息数量': 'explore.metrics.messages',
  '应用数量': 'explore.metrics.apps',
  '用户数量': 'explore.metrics.users',
};

/**
 * 指标 hint 文案来自后端统计（中文），这里识别已知模式并映射为当前语言，未命中时原样展示。
 */
function formatMetricHint(hint?: string): string {
  const text = String(hint ?? '').trim();
  if (!text) {
    return '';
  }
  const deltaPrefix = '较前 7 天';
  if (text.startsWith(deltaPrefix)) {
    const delta = text.slice(deltaPrefix.length).trim();
    const value = delta === '持平' ? $t('explore.hints.flat') : delta;
    return `${$t('explore.hints.prefix')} ${value}`;
  }
  const published = /^已发布 (\d+)$/.exec(text);
  if (published) {
    return $t('explore.hints.published', { count: published[1] });
  }
  return text;
}

function metricLabel(label?: string) {
  const key = METRIC_LABEL_KEYS[String(label ?? '')];
  return key ? $t(key) : String(label ?? '');
}

function resolveMetricRoute(label?: string) {
  const key = METRIC_LABEL_KEYS[String(label ?? '')];
  switch (key) {
    case 'explore.metrics.apps': {
      return '/agents';
    }
    case 'explore.metrics.messages':
    case 'explore.metrics.tokens': {
      return '/chat';
    }
    case 'explore.metrics.users': {
      return '/permissions/users';
    }
    default: {
      return '/explore';
    }
  }
}

function resolveMetricIcon(label?: string) {
  const key = METRIC_LABEL_KEYS[String(label ?? '')];
  switch (key) {
    case 'explore.metrics.apps': {
      return LayoutGrid;
    }
    case 'explore.metrics.messages': {
      return MessageSquare;
    }
    case 'explore.metrics.tokens': {
      return Coins;
    }
    case 'explore.metrics.users': {
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
      <section
        class="relative overflow-hidden rounded-xl border border-border bg-card px-4 py-4 sm:px-5"
      >
        <div
          class="pointer-events-none absolute -right-12 -top-24 size-64 rounded-full bg-primary/10 blur-3xl"
        ></div>
        <div
          class="relative flex flex-col gap-5 xl:flex-row xl:items-center xl:justify-between"
        >
          <div class="flex min-w-0 items-start gap-3.5">
            <div
              class="flex size-11 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-primary/20 bg-primary/10"
            >
              <img
                v-if="logoSrc"
                :alt="appName"
                :src="logoSrc"
                class="size-8 object-contain"
              />
              <span v-else class="text-sm font-bold text-primary">LC</span>
            </div>
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <h1 class="text-lg font-semibold tracking-tight text-foreground">
                  {{ appName }}
                </h1>
                <span
                  class="rounded-md border border-primary/20 bg-primary/5 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.12em] text-primary"
                >
                  {{ $t('market.openSourceLabel') }}
                </span>
              </div>
              <p class="mt-1 max-w-2xl text-xs leading-5 text-muted-foreground">
                {{ $t('explore.banner.description') }}
              </p>
              <div
                class="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] text-muted-foreground"
              >
                <span class="inline-flex items-center gap-1.5">
                  <span class="size-1.5 rounded-full bg-primary"></span>
                  {{ $t('explore.banner.maintainedByPre') }}
                  <strong class="font-medium text-foreground">
                    {{ LANGCHAT_PRODUCT_TEAM }}
                  </strong>
                  {{ $t('explore.banner.maintainedByPost') }}
                </span>
                <button
                  class="inline-flex cursor-pointer items-center gap-1 transition-colors hover:text-primary"
                  type="button"
                  @click="openRoute('/about')"
                >
                  {{ $t('market.aboutProject') }}
                  <ExternalLink class="size-3" />
                </button>
              </div>
            </div>
          </div>

          <div class="shrink-0 xl:min-w-[420px]">
            <div
              class="mb-2 text-[10px] font-medium uppercase tracking-[0.14em] text-muted-foreground"
            >
              {{ $t('about.links.title') }}
            </div>
            <div class="grid grid-cols-2 gap-2 sm:grid-cols-4 xl:grid-cols-2">
              <a
                class="group flex h-9 items-center gap-2 rounded-lg border border-border bg-background/70 px-3 text-xs font-medium text-foreground transition-colors hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                :href="LANGCHAT_PRODUCT_LINKS.github"
                rel="noopener"
                target="_blank"
              >
                <SvgGithubIcon class="size-3.5 shrink-0" />
                GitHub
                <ExternalLink
                  class="ml-auto size-3 text-muted-foreground transition-colors group-hover:text-primary"
                />
              </a>
              <a
                class="group flex h-9 items-center gap-2 rounded-lg border border-border bg-background/70 px-3 text-xs font-medium text-foreground transition-colors hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                :href="LANGCHAT_PRODUCT_LINKS.gitee"
                rel="noopener"
                target="_blank"
              >
                <span class="font-semibold text-[#c71d23]">G</span>
                Gitee
                <ExternalLink
                  class="ml-auto size-3 text-muted-foreground transition-colors group-hover:text-primary"
                />
              </a>
              <a
                class="group flex h-9 items-center gap-2 rounded-lg border border-border bg-background/70 px-3 text-xs font-medium text-foreground transition-colors hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                :href="LANGCHAT_PRODUCT_LINKS.gitcode"
                rel="noopener"
                target="_blank"
              >
                <span class="font-semibold text-primary">GC</span>
                GitCode
                <ExternalLink
                  class="ml-auto size-3 text-muted-foreground transition-colors group-hover:text-primary"
                />
              </a>
              <a
                class="group flex h-9 items-center gap-2 rounded-lg border border-border bg-background/70 px-3 text-xs font-medium text-foreground transition-colors hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                :href="LANGCHAT_PRODUCT_LINKS.website"
                rel="noopener"
                target="_blank"
              >
                <Globe class="size-3.5 shrink-0" />
                {{ $t('about.links.website') }}
                <ExternalLink
                  class="ml-auto size-3 text-muted-foreground transition-colors group-hover:text-primary"
                />
              </a>
            </div>
          </div>
        </div>
      </section>

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
                  {{ metricLabel(item.label) }}
                </div>
                <div class="mt-0.5 text-[9px] leading-4 text-muted-foreground">
                  {{ formatMetricHint(item.hint) }}
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
                {{ $t('explore.metrics.label') }}
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
                  {{ $t('explore.charts.messageTrend') }}
                </div>
              </template>
              <EchartsUI ref="coreChartRef" height="300px" />
            </LcCard>

            <LcCard :hoverable="false" :icon-component="Coins">
              <template #header>
                <div class="text-[13px] font-semibold text-foreground">
                  {{ $t('explore.charts.tokenTrend') }}
                </div>
              </template>
              <EchartsUI ref="governanceChartRef" height="300px" />
            </LcCard>
          </div>

          <LcCard :hoverable="false" :icon-component="UserRound">
            <template #header>
              <div class="text-[13px] font-semibold text-foreground">
                {{ $t('explore.charts.userTrend') }}
              </div>
            </template>
            <EchartsUI ref="activityChartRef" height="300px" />
          </LcCard>
        </div>
      </NSpin>
    </div>
  </Page>
</template>
