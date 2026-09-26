<script lang="ts" setup>
import { nextTick, ref, watch } from 'vue';

import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

import { sanitizeEchartOption } from './analysis-event-parser';

const props = defineProps<{
  option: Record<string, unknown>;
}>();

const chartRef = ref();
const { renderEcharts } = useEcharts(chartRef);

watch(
  () => props.option,
  async (option) => {
    const safeOption = sanitizeEchartOption(option);
    if (!safeOption) return;
    await nextTick();
    await renderEcharts(safeOption as never, true);
  },
  { deep: true, immediate: true },
);
</script>

<template>
  <EchartsUI ref="chartRef" class="h-[340px] w-full" />
</template>
