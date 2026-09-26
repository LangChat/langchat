<script lang="ts" setup>
import type {
  AnalysisChartResult,
  AnalysisTableResult,
} from './analysis-event-parser';

import { watch } from 'vue';

import { BarChart3, Table2 } from '@vben/icons';

import { NCollapse, NCollapseItem, NTabPane, NTabs, NTag } from 'naive-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';

import DataAnalysisChart from './data-analysis-chart.vue';

const props = defineProps<{
  chart?: AnalysisChartResult;
  table?: AnalysisTableResult;
}>();

const [Grid, gridApi] = useVbenVxeGrid<Record<string, unknown>>({
  class: 'bg-transparent shadow-none',
  gridClass: 'px-0 pb-0',
  gridOptions: {
    border: false,
    columns: [],
    data: [],
    maxHeight: 360,
    minHeight: 180,
    showOverflow: true,
    stripe: true,
  },
});

watch(
  () => props.table,
  (table) => {
    gridApi.setGridOptions({
      columns: (table?.columns || []).map((column) => ({
        align: 'left',
        field: column,
        minWidth: 140,
        title: column,
      })),
      data: table?.rows || [],
    });
  },
  { immediate: true },
);
</script>

<template>
  <div v-if="table || chart" class="mt-3 overflow-hidden rounded-lg border border-border bg-background">
    <NTabs type="line" class="px-3">
      <NTabPane v-if="chart" name="chart">
        <template #tab><span class="inline-flex items-center gap-1.5"><BarChart3 class="size-3.5" />图表</span></template>
        <DataAnalysisChart :option="chart.option" />
      </NTabPane>
      <NTabPane v-if="table" name="table">
        <template #tab><span class="inline-flex items-center gap-1.5"><Table2 class="size-3.5" />数据表</span></template>
        <div class="pb-3">
          <div class="mb-2 flex items-center justify-between gap-2">
            <span class="text-xs font-semibold text-foreground">{{ table.title }}</span>
            <NTag :bordered="false" size="small">{{ table.rowCount }} 行</NTag>
          </div>
          <Grid />
          <NCollapse v-if="table.sql" class="mt-2">
            <NCollapseItem name="sql" title="查看 SQL">
              <pre class="overflow-x-auto rounded-md bg-muted/50 p-3 text-[11px] leading-5 text-muted-foreground">{{ table.sql }}</pre>
            </NCollapseItem>
          </NCollapse>
        </div>
      </NTabPane>
    </NTabs>
  </div>
</template>
