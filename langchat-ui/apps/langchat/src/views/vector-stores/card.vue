<script setup lang="ts">
import type { AigcVectorStore } from '#/api/aigc/vector-store';

import { computed } from 'vue';

import { DatabaseZap, SquarePen, Trash2 } from '@vben/icons';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';
import {
  findOptionLabel,
  VECTOR_PROVIDER_OPTIONS,
} from '#/views/shared/aigc/options';

interface Props {
  item: AigcVectorStore;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  delete: [item: AigcVectorStore];
  edit: [item: AigcVectorStore];
}>();
const metaItems = computed(() => [
  {
    label: '提供商',
    value: findOptionLabel(VECTOR_PROVIDER_OPTIONS, props.item.provider),
  },
  { label: '数据库', value: props.item.databaseName || '--' },
  { label: '维度', value: props.item.dimension ?? '--' },
  {
    label: '主机',
    value: props.item.host
      ? `${props.item.host}:${props.item.port ?? ''}`
      : '--',
  },
]);
function formatTime(timestamp?: number) {
  if (!timestamp) {
    return '刚刚';
  }
  return new Date(timestamp).toLocaleString('zh-CN', { hour12: false });
}
</script>

<template>
  <LcCard
    :icon-component="DatabaseZap"
    :meta-items="metaItems"
    class="cursor-pointer"
    hoverable
    @click="emit('edit', item)"
  >
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.name || '未命名向量库' }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        当前节点 {{ item.host || '--' }}:{{ item.port || '--' }}，用于承载
        {{ item.tableName || '默认表' }} 的向量化数据。
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag
        :label="findOptionLabel(VECTOR_PROVIDER_OPTIONS, item.provider)"
        type="primary"
      />
    </template>

    <template #footer-extra>
      <div class="truncate whitespace-nowrap text-[11px] text-muted-foreground">
        {{ formatTime(item.updateTime) }}
      </div>
    </template>

    <template #footer>
      <NSpace :size="4">
        <NButton
          v-tippy="'编辑'"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click.stop="emit('edit', item)"
        >
          <SquarePen class="size-3.5" />
        </NButton>
        <NButton
          v-tippy="'删除'"
          circle
          class="text-muted-foreground hover:text-destructive"
          quaternary
          size="small"
          type="error"
          @click.stop="emit('delete', item)"
        >
          <Trash2 class="size-3.5" />
        </NButton>
      </NSpace>
    </template>
  </LcCard>
</template>
