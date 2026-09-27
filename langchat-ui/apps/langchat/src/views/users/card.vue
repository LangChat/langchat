<script setup lang="ts">
import type { AigcUser } from '#/api/auth/user';

import { computed } from 'vue';

import { SquarePen, Trash2, UserRound } from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';
import { formatRelativeTime } from '#/views/shared/aigc/time';

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
    label: $t('users.card.rolesLabel'),
    value:
      props.roleLabels.length > 0
        ? props.roleLabels.slice(0, 2).join('、')
        : $t('users.card.notAssigned'),
  },
  { label: $t('users.form.email'), value: props.item.email || '--' },
  { label: $t('users.card.phone'), value: props.item.phone || '--' },
]);
</script>

<template>
  <LcCard :icon-component="UserRound" :meta-items="metaItems">
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.realName || item.username || $t('users.card.unnamed') }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{
          $t('users.card.roleCount', {
            count: roleLabels.length,
          })
        }}
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag
        :label="
          item.status === 1
            ? $t('common.status.enabled')
            : $t('users.card.locked')
        "
        :type="item.status === 1 ? 'success' : 'warning'"
      />
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
