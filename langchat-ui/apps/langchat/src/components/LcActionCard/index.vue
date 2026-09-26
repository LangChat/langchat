<script setup lang="ts">
import type { Component } from 'vue';

import LcCard from '#/components/LcCard/index.vue';

interface ActionItem {
  disabled?: boolean;
  icon?: Component;
  key: string;
  label: string;
}

interface Props {
  actions?: ActionItem[];
  description?: string;
  title?: string;
}

withDefaults(defineProps<Props>(), {
  actions: () => [],
  description: '',
  title: '快捷操作',
});

const emit = defineEmits<{
  action: [item: ActionItem];
}>();
</script>

<template>
  <LcCard
    :show-icon="false"
    class="lc-action-card min-h-[180px] !px-2 !pb-1.5 !pt-2"
  >
    <template #header>
      <div class="text-[12px] font-semibold text-foreground">{{ title }}</div>
    </template>

    <div v-if="description" class="text-[10px] leading-4 text-muted-foreground">
      {{ description }}
    </div>

    <div class="flex flex-col gap-1">
      <button
        v-for="item in actions"
        :key="item.key"
        :disabled="item.disabled"
        class="inline-flex w-full cursor-pointer items-center gap-1.5 rounded-md border border-transparent bg-muted/45 px-2 py-1.5 text-left text-[10px] text-foreground transition-colors hover:border-primary/35 hover:bg-primary/10 hover:text-primary disabled:cursor-not-allowed disabled:opacity-50"
        type="button"
        @click="emit('action', item)"
      >
        <component :is="item.icon" v-if="item.icon" class="size-3 shrink-0" />
        <span class="truncate">{{ item.label }}</span>
      </button>
    </div>
  </LcCard>
</template>

<style scoped>
:deep(.lc-action-card > .flex) {
  gap: 0.25rem;
}
</style>
