<script setup lang="ts">
import type { AigcMenu } from '#/api/auth/menu';

import { computed } from 'vue';

import { Menu, SquarePen, Trash2 } from '@vben/icons';

import { NButton, NSpace, NTag } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import {
  findAuthOptionLabel,
  MENU_TYPE_OPTIONS,
} from '#/views/shared/auth/options';

interface Props {
  item: AigcMenu;
  parentName?: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  delete: [item: AigcMenu];
  edit: [item: AigcMenu];
}>();
const metaItems = computed(() => [
  {
    label: '类型',
    value: findAuthOptionLabel(MENU_TYPE_OPTIONS, props.item.type),
  },
  { label: '路由', value: props.item.path || '--' },
  {
    label: '上级',
    value: props.parentName || '无',
  },
  {
    label: '排序',
    value: props.item.orderNo ?? '--',
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
  <LcCard :icon-component="Menu" :meta-items="metaItems">
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.name || '未命名菜单' }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{ item.component || item.path || '未配置组件路径' }}
      </p>
    </template>

    <template #header-extra>
      <div class="flex items-center gap-2">
        <NTag :bordered="false" round type="primary">
          {{ findAuthOptionLabel(MENU_TYPE_OPTIONS, item.type) }}
        </NTag>
        <NTag
          v-if="item.isShow === false"
          :bordered="false"
          round
          type="warning"
        >
          已隐藏
        </NTag>
      </div>
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
