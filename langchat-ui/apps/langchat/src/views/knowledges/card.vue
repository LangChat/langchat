<script setup lang="ts">
import type { AigcKnowledge } from '#/api/aigc/knowledge';
import type { LabelOption } from '#/views/shared/aigc/options';

import { computed } from 'vue';

import { Database, SquarePen, Trash2 } from '@vben/icons';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';

interface Props {
  item: AigcKnowledge;
  modelOptions: LabelOption[];
  vectorStoreOptions: LabelOption[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  delete: [item: AigcKnowledge];
  docs: [item: AigcKnowledge];
  edit: [item: AigcKnowledge];
  index: [item: AigcKnowledge];
}>();

const modelLabel = computed(() => {
  const found = props.modelOptions.find((o) => o.value === props.item.modelId);
  return found?.label || '未配置';
});

const vectorStoreLabel = computed(() => {
  const found = props.vectorStoreOptions.find(
    (o) => o.value === props.item.vectorStoreId,
  );
  return found?.label || '未配置';
});

const metaItems = computed(() => [
  { label: '模型', value: modelLabel.value },
  { label: '向量库', value: vectorStoreLabel.value },
  {
    label: '召回',
    value: props.item.maxResults ? `Top ${props.item.maxResults}` : '默认',
  },
  {
    label: '重排',
    value: props.item.rerank ? '已启用' : '未启用',
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
    :hoverable="true"
    :icon-component="Database"
    :meta-items="metaItems"
    class="h-full cursor-pointer"
    @click="emit('docs', item)"
  >
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground group-hover:text-primary"
        >
          {{ item.name || '未命名知识库' }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{ item.description || '用于承载文档切片、向量召回和重排配置。' }}
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag
        :label="item.rerank ? '已启用重排' : '未启用重排'"
        :type="item.rerank ? 'success' : 'warning'"
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
