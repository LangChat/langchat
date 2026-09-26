<script setup lang="ts">
import type { AigcUser } from '#/api/auth/user';

import { computed } from 'vue';

import { SquarePen, Trash2, UserRound } from '@vben/icons';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';

interface Props {
  item: AigcUser;
  roleLabels: string[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  delete: [item: AigcUser];
  edit: [item: AigcUser];
}>();

const metaItems = computed(() => [
  {
    label: '角色',
    value:
      props.roleLabels.length > 0
        ? props.roleLabels.slice(0, 2).join('、')
        : '未分配',
  },
  { label: '邮箱', value: props.item.email || '--' },
  { label: '手机', value: props.item.phone || '--' },
]);

function formatTime(timestamp?: number) {
  if (!timestamp) {
    return '刚刚';
  }
  return new Date(timestamp).toLocaleString('zh-CN', { hour12: false });
}
</script>

<template>
  <LcCard :icon-component="UserRound" :meta-items="metaItems">
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.realName || item.username || '未命名用户' }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        当前用户已分配
        {{ roleLabels.length }} 个角色，可在编辑抽屉中维护角色关系。
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag
        :label="item.status === 1 ? '启用' : '锁定'"
        :type="item.status === 1 ? 'success' : 'warning'"
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
