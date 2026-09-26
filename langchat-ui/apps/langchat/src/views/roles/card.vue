<script setup lang="ts">
import type { AigcRole } from '#/api/auth/role';

import { computed } from 'vue';

import { ShieldCheck, SquarePen, Trash2 } from '@vben/icons';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';

interface Props {
  item: AigcRole;
  menuLabels: string[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  delete: [item: AigcRole];
  edit: [item: AigcRole];
}>();
const metaItems = computed(() => [
  { label: '编码', value: props.item.code || '--' },
  {
    label: '菜单权限',
    value:
      props.menuLabels.length > 0 ? `${props.menuLabels.length} 个` : '未分配',
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
  <LcCard :icon-component="ShieldCheck" :meta-items="metaItems">
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.name || '未命名角色' }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{ item.description || '当前角色用于承接菜单权限分配。' }}
      </p>
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
