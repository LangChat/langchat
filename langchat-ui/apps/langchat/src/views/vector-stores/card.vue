<script setup lang="ts">
import type { AigcVectorStore } from '#/api/aigc/vector-store';

import { computed } from 'vue';

import { DatabaseZap, SquarePen, Trash2 } from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';
import {
  findOptionLabel,
  VECTOR_PROVIDER_OPTIONS,
} from '#/views/shared/aigc/options';
import { formatRelativeTime } from '#/views/shared/aigc/time';

interface Props {
  item: AigcVectorStore;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  delete: [item: AigcVectorStore];
  edit: [item: AigcVectorStore];
}>();
const metaItems = computed(() => [
  {
    label: $t('vectorStores.card.providerLabel'),
    value: findOptionLabel(VECTOR_PROVIDER_OPTIONS, props.item.provider),
  },
  {label: $t('vectorStores.card.database'), value: props.item.databaseName || '--'},
  {label: $t('vectorStores.card.dimension'), value: props.item.dimension ?? '--'},
  {
    label: $t('common.labels.host'),
    value: props.item.host
      ? `${props.item.host}:${props.item.port ?? ''}`
      : '--',
  },
]);
</script>

<template>
  <LcCard
    :icon-component="DatabaseZap"
    :meta-items="metaItems"
    class="cursor-pointer"
    hoverable
    @click="emit('edit', item)"
  >
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.name || $t('vectorStores.card.unnamed') }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{
          $t('vectorStores.card.nodeSummary', {
            host: item.host || '--',
            port: item.port || '--',
          })
        }}{{ item.tableName || $t('vectorStores.card.defaultTable')
        }}{{ $t('vectorStores.card.tableSummary') }}
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag
        :label="findOptionLabel(VECTOR_PROVIDER_OPTIONS, item.provider)"
        type="primary"
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
          @click.stop="emit('edit', item)"
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
          @click.stop="emit('delete', item)"
        >
          <Trash2 class="size-3.5" />
        </NButton>
      </NSpace>
    </template>
  </LcCard>
</template>
