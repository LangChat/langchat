<script setup lang="ts">
import type { AgentChatToolEvent } from './types';

import { computed, ref } from 'vue';
import {
  ChevronDown,
  CircleCheckBig,
  CircleX,
  LoaderCircle,
  Wrench,
} from '@vben/icons';

interface Props {
  item: AgentChatToolEvent;
}

const props = defineProps<Props>();
const expanded = ref(false);

const statusMeta = computed(() => {
  if (props.item.status === 'failed') {
    return {
      badgeClass: 'border-red-200 bg-red-50 text-red-600',
      cardClass: 'border-red-200/70 bg-red-50/50',
      icon: CircleX,
      label: '执行失败',
      loading: false,
    };
  }
  if (props.item.status === 'completed') {
    return {
      badgeClass: 'border-emerald-200 bg-emerald-50 text-emerald-600',
      cardClass: 'border-emerald-200/70 bg-emerald-50/40',
      icon: CircleCheckBig,
      label: '执行完成',
      loading: false,
    };
  }
  if (props.item.status === 'started') {
    return {
      badgeClass: 'border-sky-200 bg-sky-50 text-sky-600',
      cardClass: 'border-sky-200/70 bg-sky-50/40',
      icon: LoaderCircle,
      label: '开始执行',
      loading: true,
    };
  }
  return {
    badgeClass: 'border-sky-200 bg-sky-50 text-sky-600',
    cardClass: 'border-sky-200/70 bg-sky-50/40',
    icon: LoaderCircle,
    label: '执行中',
    loading: true,
  };
});
</script>

<template>
  <div :class="statusMeta.cardClass" class="rounded-md border p-1.5">
    <button
      class="flex w-full items-center gap-2 text-left"
      type="button"
      @click="expanded = !expanded"
    >
      <Wrench class="size-3 text-primary" />
      <span class="min-w-0 flex-1 truncate text-xs font-medium text-foreground">
        {{ item.toolName || '未命名工具' }}
      </span>
      <span
        :class="statusMeta.badgeClass"
        class="inline-flex items-center gap-1 rounded border px-1 py-0.5 text-[10px]"
      >
        <component
          :is="statusMeta.icon"
          :class="[statusMeta.loading && 'animate-spin', 'size-3']"
        />
        {{ statusMeta.label }}
      </span>
      <ChevronDown
        :class="[expanded && 'rotate-180']"
        class="size-3 text-muted-foreground transition-transform"
      />
    </button>

    <div v-if="expanded" class="mt-1.5 space-y-1">
      <div
        v-if="item.argumentsText"
        class="rounded border border-border/70 bg-background/80 p-1.5"
      >
        <div class="mb-1 text-[11px] text-muted-foreground">入参</div>
        <pre
          class="whitespace-pre-wrap break-all text-[10px] leading-4 text-foreground"
          >{{ item.argumentsText }}</pre
        >
      </div>

      <div
        v-if="item.resultText"
        class="rounded border border-border/70 bg-background/80 p-1.5"
      >
        <div class="mb-1 text-[11px] text-muted-foreground">出参</div>
        <pre
          class="whitespace-pre-wrap break-all text-[10px] leading-4 text-foreground"
          >{{ item.resultText }}</pre
        >
      </div>

      <div
        v-if="item.errorText"
        class="rounded border border-red-200 bg-red-50 p-1.5"
      >
        <div class="mb-1 text-[11px] text-red-500">错误信息</div>
        <pre
          class="whitespace-pre-wrap break-all text-[10px] leading-4 text-red-600"
          >{{ item.errorText }}</pre
        >
      </div>
    </div>
  </div>
</template>
