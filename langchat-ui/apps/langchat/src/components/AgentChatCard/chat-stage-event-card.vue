<script setup lang="ts">
import type { AgentChatStageEvent } from './types';

import { computed } from 'vue';
import { CircleCheckBig, LoaderCircle, TriangleAlert } from '@vben/icons';

interface Props {
  item: AgentChatStageEvent;
}

const props = defineProps<Props>();

const stageMeta = computed(() => {
  if (props.item.status === 'error') {
    return {
      dotClass: 'bg-red-500',
      icon: TriangleAlert,
      iconClass: 'text-red-500',
      textClass: 'text-red-600',
    };
  }
  if (props.item.status === 'success') {
    return {
      dotClass: 'bg-emerald-500',
      icon: CircleCheckBig,
      iconClass: 'text-emerald-500',
      textClass: 'text-emerald-600',
    };
  }
  return {
    dotClass: 'bg-sky-500',
    icon: LoaderCircle,
    iconClass: 'text-sky-500 animate-spin',
    textClass: 'text-sky-600',
  };
});
</script>

<template>
  <div
    class="rounded border border-dashed border-border/70 bg-background px-1.5 py-1"
  >
    <div class="flex items-center gap-1.5 text-[11px]">
      <span :class="stageMeta.dotClass" class="size-1.5 rounded-full" />
      <component
        :is="stageMeta.icon"
        :class="['size-3', stageMeta.iconClass]"
      />
      <span :class="stageMeta.textClass">{{ item.text }}</span>
    </div>
  </div>
</template>
