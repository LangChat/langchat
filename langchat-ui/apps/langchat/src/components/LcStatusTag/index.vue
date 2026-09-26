<script setup lang="ts">
import { NTag } from 'naive-ui';

interface Props {
  /** 状态文本 */
  label: string;
  /** NaiveUI 标签类型 */
  type?: 'default' | 'error' | 'info' | 'primary' | 'success' | 'warning';
}

withDefaults(defineProps<Props>(), {
  type: 'default',
});
</script>

<template>
  <NTag
    :bordered="false"
    :type="type"
    round
    size="tiny"
    class="rounded-full border border-slate-200 px-1.5 py-0 text-[9px] font-normal tracking-[0.06em]"
  >
    {{ label }}
  </NTag>
</template>
