<script setup lang="ts">
import type { AigcMcp } from '#/api/aigc/mcp';

import { computed } from 'vue';

import { PlugZap, SquarePen, Trash2 } from '@vben/icons';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';
import {
  findOptionLabel,
  MCP_TRANSPORT_OPTIONS,
} from '#/views/shared/aigc/options';

interface Props {
  item: AigcMcp;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  delete: [item: AigcMcp];
  edit: [item: AigcMcp];
}>();

const metaItems = computed(() => [
  {
    label: '传输协议',
    value: findOptionLabel(MCP_TRANSPORT_OPTIONS, props.item.transport),
  },
  { label: '站点', value: props.item.siteUrl || '--' },
  { label: '唯一标识', value: props.item.uuid || '--' },
  {
    label: '超时',
    value: props.item.timeout ? `${props.item.timeout}s` : '默认',
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
  <LcCard :icon-component="PlugZap" :meta-items="metaItems" class="cursor-pointer" hoverable @click="emit('edit', item)">
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.name || '未命名服务' }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        当前传输协议
        {{ findOptionLabel(MCP_TRANSPORT_OPTIONS, item.transport) }}，站点
        {{ item.siteUrl || '--' }}。
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag
        :label="item.authorized ? '已授权' : '待授权'"
        :type="item.authorized ? 'success' : 'warning'"
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
