<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  /** 状态文本 */
  label: string;
  /** 状态语义类型 */
  type?: 'default' | 'error' | 'info' | 'primary' | 'success' | 'warning';
}

const props = withDefaults(defineProps<Props>(), {
  type: 'default',
});

const typeClass = computed(() => {
  const styles = {
    default: 'border-border bg-muted/55 text-muted-foreground',
    error:
      'border-red-200 bg-red-50 text-red-700 dark:border-red-900/70 dark:bg-red-950/45 dark:text-red-300',
    info:
      'border-sky-200 bg-sky-50 text-sky-700 dark:border-sky-900/70 dark:bg-sky-950/45 dark:text-sky-300',
    primary: 'border-primary/25 bg-primary/8 text-primary',
    success:
      'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900/70 dark:bg-emerald-950/45 dark:text-emerald-300',
    warning:
      'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-900/70 dark:bg-amber-950/45 dark:text-amber-300',
  } as const;

  return styles[props.type];
});
</script>

<template>
  <span
    :class="typeClass"
    class="inline-flex h-[18px] shrink-0 items-center whitespace-nowrap rounded-[5px] border px-1.5 text-[10px] font-medium leading-none tracking-[0.02em]"
  >
    {{ label }}
  </span>
</template>
