<script lang="ts" setup>
import type { Component } from 'vue';

import type {
  AnalysisChartResult,
  AnalysisTableResult,
} from './analysis-event-parser';

import type { DataAnalysisCatalogItem } from '#/api/aigc/data-analysis';
import type { ChatSendPayload, ChatSuggestion } from '#/components/chat';

import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import {
  BarChart3,
  Check,
  Database,
  ListOrdered,
  Sparkles,
  TrendingUp,
} from '@vben/icons';

import {
  NCheckbox,
  NCheckboxGroup,
  NEmpty,
  NSelect,
  NSpin,
  NTag,
} from 'naive-ui';

import { message } from '#/adapter/naive';
import {
  listDataAnalysisCatalog,
  streamDataAnalysis,
} from '#/api/aigc/data-analysis';
import { ChatLayout } from '#/components/chat';

import { parseDataAnalysisEvent } from './analysis-event-parser';
import DataAnalysisResult from './data-analysis-result.vue';

const loadingCatalog = ref(false);
const asking = ref(false);
const catalog = ref<DataAnalysisCatalogItem[]>([]);
const datasourceId = ref('');
const selectedTables = ref<string[]>([]);
let abortController: AbortController | null = null;

const chatRef = ref<InstanceType<typeof ChatLayout> | null>(null);

const datasourceOptions = computed(() =>
  catalog.value.map((item) => ({
    label: item.datasourceName,
    value: item.datasourceId,
  })),
);
const currentDatasource = computed(() =>
  catalog.value.find((item) => item.datasourceId === datasourceId.value),
);
const availableTables = computed(() => currentDatasource.value?.tables || []);
const allSelected = computed(
  () =>
    availableTables.value.length > 0 &&
    selectedTables.value.length === availableTables.value.length,
);
const partiallySelected = computed(
  () => selectedTables.value.length > 0 && !allSelected.value,
);

const suggestionItems: ChatSuggestion[] = [
  { icon: BarChart3 as Component, label: '统计各状态的数据量并展示占比' },
  { icon: TrendingUp as Component, label: '按时间分析最近的数据变化趋势' },
  { icon: ListOrdered as Component, label: '找出数量最多的前十项并生成图表' },
];

function asChart(value: unknown): AnalysisChartResult | undefined {
  return value as AnalysisChartResult | undefined;
}

function asTable(value: unknown): AnalysisTableResult | undefined {
  return value as AnalysisTableResult | undefined;
}

watch(datasourceId, () => {
  selectedTables.value = [];
});

async function loadCatalog() {
  loadingCatalog.value = true;
  try {
    catalog.value = await listDataAnalysisCatalog();
    if (!datasourceId.value && catalog.value.length > 0) {
      datasourceId.value = catalog.value[0]?.datasourceId || '';
    }
  } finally {
    loadingCatalog.value = false;
  }
}

function toggleAll(checked: boolean) {
  selectedTables.value = checked
    ? availableTables.value.map((table) => table.sourceName)
    : [];
}

async function handleSend(payload: ChatSendPayload) {
  if (asking.value) return;
  if (!datasourceId.value || selectedTables.value.length === 0) {
    message.warning('请先选择数据源和要检索的表');
    return;
  }
  const layout = chatRef.value;
  if (!layout) return;

  const turnId = layout.beginTurn(payload.text);
  asking.value = true;
  abortController = new AbortController();
  try {
    await streamDataAnalysis(
      {
        datasourceId: datasourceId.value,
        question: payload.text,
        tableNames: [...selectedTables.value],
      },
      (streamEvent) => {
        const event = parseDataAnalysisEvent(streamEvent);
        if (event.type === 'message.delta') {
          layout.appendDelta(turnId, event.content);
        }
        if (event.type === 'table') {
          layout.setTurnExtra(turnId, { table: event.result });
        }
        if (event.type === 'chart') {
          layout.setTurnExtra(turnId, { chart: event.result });
        }
        if (event.type === 'tool') {
          layout.setTurnMeta(
            turnId,
            event.status === 'completed' ? '数据查询完成' : '正在查询数据',
          );
        }
        if (event.type === 'error') {
          layout.failTurn(turnId, event.message);
          message.error('智能问数执行失败');
        }
        if (event.type === 'done') {
          layout.completeTurn(turnId);
        }
      },
      abortController.signal,
    );
  } catch (error) {
    layout.failTurn(
      turnId,
      (error as Error)?.message || '智能问数执行失败',
    );
    message.error('智能问数执行失败');
  } finally {
    asking.value = false;
    abortController = null;
  }
}

onMounted(loadCatalog);
onBeforeUnmount(() => abortController?.abort());
</script>

<template>
  <Page>
    <div
      class="flex h-full min-h-0 flex-col gap-3 rounded-xl border border-border bg-background p-3"
    >
      <div class="flex items-center justify-between gap-3 border-b border-border pb-3">
        <div class="flex items-center gap-3">
          <div class="flex size-9 items-center justify-center rounded-lg border border-border bg-background text-primary"><BarChart3 class="size-4" /></div>
          <div><div class="text-base font-semibold text-foreground">智能问数</div><div class="text-[11px] text-muted-foreground">{{ selectedTables.length }} 张表已选择</div></div>
        </div>
        <NTag v-if="currentDatasource" :bordered="false" type="success"><span class="inline-flex items-center gap-1"><Check class="size-3" />{{ currentDatasource.datasourceName }}</span></NTag>
      </div>

      <NSpin :show="loadingCatalog" class="min-h-0 flex-1" content-class="h-full">
        <div class="grid h-full min-h-0 gap-3 lg:grid-cols-[280px_minmax(0,1fr)]">
          <aside class="flex min-h-0 flex-col border-r border-border pr-3">
            <div class="mb-3">
              <div class="mb-1.5 text-xs font-semibold text-foreground">数据源</div>
              <NSelect v-model:value="datasourceId" :options="datasourceOptions" placeholder="选择数据源" />
            </div>
            <div class="mb-2 flex items-center justify-between border-b border-border pb-2">
              <NCheckbox :checked="allSelected" :indeterminate="partiallySelected" @update:checked="toggleAll">可检索表</NCheckbox>
              <span class="text-[10px] text-muted-foreground">{{ selectedTables.length }}/{{ availableTables.length }}</span>
            </div>
            <div class="min-h-0 flex-1 space-y-1 overflow-y-auto pr-1">
              <NCheckboxGroup v-model:value="selectedTables" class="flex flex-col gap-1">
                <label v-for="table in availableTables" :key="table.sourceName" class="flex cursor-pointer items-center gap-2 rounded-md px-2 py-2 hover:bg-muted/50">
                  <NCheckbox :value="table.sourceName" />
                  <Database class="size-3.5 shrink-0 text-muted-foreground" />
                  <span class="min-w-0 flex-1 truncate text-xs font-medium text-foreground">{{ table.name }}</span>
                  <span class="text-[10px] text-muted-foreground">{{ table.columnCount }}</span>
                </label>
              </NCheckboxGroup>
              <NEmpty v-if="availableTables.length === 0" description="暂无可检索表" size="small" />
            </div>
          </aside>

          <main class="flex min-h-0 min-w-0 flex-col">
            <ChatLayout
              ref="chatRef"
              :disabled="asking"
              empty-title="从已授权的数据中提问"
              placeholder="输入你的数据问题"
              :show-attachment="false"
              :suggestions="suggestionItems"
              @send="handleSend"
            >
              <template #empty-head>
                <div class="flex flex-col items-center gap-3">
                  <div class="flex size-12 items-center justify-center rounded-xl border border-border text-primary"><Sparkles class="size-5" /></div>
                  <div class="text-xl font-semibold text-foreground">从已授权的数据中提问</div>
                </div>
              </template>
              <template #message-append="{ item }">
                <DataAnalysisResult
                  v-if="item.extra?.chart || item.extra?.table"
                  class="mt-3"
                  :chart="asChart(item.extra?.chart)"
                  :table="asTable(item.extra?.table)"
                />
              </template>
            </ChatLayout>
          </main>
        </div>
      </NSpin>
    </div>
  </Page>
</template>
