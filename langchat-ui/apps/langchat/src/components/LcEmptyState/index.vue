<script setup lang="ts">
import type { Component } from 'vue';

import { Inbox } from '@vben/icons';

interface Props {
  description?: string;
  icon?: Component;
  title?: string;
}

withDefaults(defineProps<Props>(), {
  description: '',
  icon: () => Inbox,
  title: undefined,
});
</script>

<template>
  <div
    class="flex min-h-[220px] flex-col items-center justify-center px-6 py-10 text-center"
  >
    <div
      class="mb-3 flex size-11 items-center justify-center rounded-lg border border-border/70 text-muted-foreground/60"
    >
      <component :is="icon" class="size-5" />
    </div>
    <div class="text-[13px] font-semibold text-foreground/80">
      {{ title ?? $t('common.empty.noData') }}
    </div>
    <div
      v-if="description"
      class="mt-1 max-w-sm text-xs leading-5 text-muted-foreground"
    >
      {{ description }}
    </div>
    <div v-if="$slots.default" class="mt-4">
      <slot></slot>
    </div>
  </div>
</template>
