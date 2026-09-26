<script setup lang="ts">
import type { AigcDocs } from '#/api/aigc/docs';
import type { LabelOption } from '#/views/shared/aigc/options';

import { computed } from 'vue';

import {
  Activity,
  FileSearch2,
  FileText,
  PlayCircle,
  RotateCcw,
  SquarePen,
  Trash2,
} from '@vben/icons';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';
import {
  resolveDocsStatusLabel,
  resolveDocsStatusType,
} from '#/views/shared/aigc/docs-status';

interface Props {
  item: AigcDocs;
  knowledgeOptions: LabelOption[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  delete: [item: AigcDocs];
  edit: [item: AigcDocs];
  index: [item: AigcDocs];
  preview: [item: AigcDocs];
  retry: [item: AigcDocs];
  status: [item: AigcDocs];
}>();

const knowledgeName = computed(
  () =>
    props.knowledgeOptions.find((item) => item.value === props.item.knowledgeId)
      ?.label || '未配置',
);

const metaItems = computed(() => [
  { label: '知识库', value: knowledgeName.value },
  { label: '类型', value: props.item.ext || props.item.type || '--' },
  {
    label: '大小',
    value:
      props.item.size > 0 ? `${(props.item.size / 1024).toFixed(1)} KB` : '--',
  },
  {
    label: '状态',
    value: resolveDocsStatusLabel(props.item.embedStatus),
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
  <LcCard :icon-component="FileText" :meta-items="metaItems" :show-icon="true">
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.name || '未命名文档' }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{
          item.content ||
          `归属知识库 ${knowledgeName}，状态 ${resolveDocsStatusLabel(item.embedStatus)}。`
        }}
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag
        :label="resolveDocsStatusLabel(item.embedStatus)"
        :type="resolveDocsStatusType(item.embedStatus)"
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
          v-tippy="'解析预览'"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click="emit('preview', item)"
        >
          <FileSearch2 class="size-3.5" />
        </NButton>
        <NButton
          v-tippy="'开始向量化'"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click="emit('index', item)"
        >
          <PlayCircle class="size-3.5" />
        </NButton>
        <NButton
          v-if="item.embedStatus === 'failed'"
          v-tippy="'失败重试'"
          circle
          class="text-muted-foreground hover:text-destructive"
          quaternary
          size="small"
          type="error"
          @click="emit('retry', item)"
        >
          <RotateCcw class="size-3.5" />
        </NButton>
        <NButton
          v-tippy="'状态详情'"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click="emit('status', item)"
        >
          <Activity class="size-3.5" />
        </NButton>
        <NButton
          v-tippy="'编辑'"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click="emit('edit', item)"
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
          @click="emit('delete', item)"
        >
          <Trash2 class="size-3.5" />
        </NButton>
      </NSpace>
    </template>
  </LcCard>
</template>
