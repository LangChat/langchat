<script setup lang="ts">
import type { AigcRole } from '#/api/auth/role';

import { computed } from 'vue';

import { ShieldCheck, SquarePen, Trash2 } from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import { formatRelativeTime } from '#/views/shared/aigc/time';

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
  { label: $t('roles.card.code'), value: props.item.code || '--' },
  {
    label: $t('roles.card.menuAuth'),
    value:
      props.menuLabels.length > 0
        ? $t('roles.card.menuCount', { count: props.menuLabels.length })
        : $t('roles.card.notAssigned'),
  },
]);
</script>

<template>
  <LcCard :icon-component="ShieldCheck" :meta-items="metaItems">
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.name || $t('roles.card.unnamed') }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{ item.description || $t('roles.card.defaultDescription') }}
      </p>
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
