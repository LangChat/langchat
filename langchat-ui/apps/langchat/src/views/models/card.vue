<script lang="ts" setup>
import type { AigcModel } from '#/api/aigc/model';

import { computed } from 'vue';

import { SquarePen, Trash2 } from '@vben/icons';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';

import { getModelTypeIcon, getModelTypeLabel, getProviderLabel } from './model-meta';
import { getProviderIconUrl } from './provider-icons';

interface Props {
  item: AigcModel;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  delete: [item: AigcModel];
  edit: [item: AigcModel];
}>();

const providerLabel = computed(() => getProviderLabel(props.item.provider));
const typeLabel = computed(() => getModelTypeLabel(props.item.type));
const providerIconUrl = computed(() => getProviderIconUrl(props.item.provider));
const typeIcon = computed(() => getModelTypeIcon(props.item.type));

const metaItems = computed(() => [
  { label: '类型', value: typeLabel.value },
  { label: '供应商', value: providerLabel.value },
  { label: '最大令牌', value: props.item.maxToken || '--' },
  { label: '地址', value: props.item.baseUrl || '默认' },
]);

function formatTime(timestamp?: number) {
  if (!timestamp) {
    return '刚刚';
  }
  return new Date(timestamp).toLocaleString('zh-CN', { hour12: false });
}
</script>

<template>
  <LcCard :icon-size="36" :meta-items="metaItems" class="cursor-pointer" hoverable @click="emit('edit', item)">
    <template #icon>
      <span
        :style="{ height: '36px', width: '36px' }"
        class="inline-flex shrink-0 items-center justify-center overflow-hidden rounded-lg border border-border bg-background/75"
      >
        <img
          v-if="providerIconUrl"
          :src="providerIconUrl"
          alt=""
          class="h-[58%] w-[58%] object-contain"
        />
        <component :is="typeIcon" v-else class="h-[58%] w-[58%] text-primary" />
      </span>
    </template>
    <template #header>
      <div class="min-w-0">
        <div
          class="flex items-center gap-1.5 truncate text-[13px] font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.name || '未命名模型' }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{ item.model || '未配置模型标识' }}
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag :label="providerLabel" type="primary" />
    </template>

    <template #footer-extra>
      <div class="truncate whitespace-nowrap text-[9px] text-muted-foreground">
        {{ formatTime(item.updateTime) }}
      </div>
    </template>

    <template #footer>
      <NSpace :size="2">
        <NButton
          v-tippy="'编辑'"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click.stop="emit('edit', item)"
        >
          <SquarePen class="size-3" />
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
          <Trash2 class="size-3" />
        </NButton>
      </NSpace>
    </template>
  </LcCard>
</template>
