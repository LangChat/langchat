<script setup lang="ts">
import type { AigcMenu } from '#/api/auth/menu';

import { computed } from 'vue';

import { Menu, SquarePen, Trash2 } from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton, NSpace, NTag } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import { formatRelativeTime } from '#/views/shared/aigc/time';
import {
  findAuthOptionLabel,
  menuTypeOptions,
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
    label: $t('menus.card.typeLabel'),
    value: findAuthOptionLabel(menuTypeOptions(), props.item.type),
  },
  { label: $t('menus.card.route'), value: props.item.path || '--' },
  {
    label: $t('menus.card.parent'),
    value: props.parentName || $t('common.labels.none'),
  },
  {
    label: $t('menus.card.order'),
    value: props.item.orderNo ?? '--',
  },
]);
</script>

<template>
  <LcCard :icon-component="Menu" :meta-items="metaItems">
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.name || $t('menus.card.unnamed') }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{ item.component || item.path || $t('menus.card.noComponentPath') }}
      </p>
    </template>

    <template #header-extra>
      <div class="flex items-center gap-2">
        <NTag :bordered="false" round type="primary">
          {{ findAuthOptionLabel(menuTypeOptions(), item.type) }}
        </NTag>
        <NTag
          v-if="item.isShow === false"
          :bordered="false"
          round
          type="warning"
        >
          {{ $t('menus.card.hidden') }}
        </NTag>
      </div>
    </template>

    <template #footer-extra>
      <div class="truncate whitespace-nowrap text-[11px] text-muted-foreground">
        {{ formatRelativeTime(item.updateTime) }}
      </div>
    </template>

    <template #footer>
      <NSpace :size="4">
        <NButton
          v-tippy="$t('common.actions.edit')"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click="emit('edit', item)"
        >
          <SquarePen class="size-3.5" />
        </NButton>
        <NButton
          v-tippy="$t('common.actions.delete')"
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
