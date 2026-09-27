<script setup lang="ts">
import type { AigcKnowledge } from '#/api/aigc/knowledge';
import type { LabelOption } from '#/views/shared/aigc/options';

import { computed } from 'vue';

import { Database, SquarePen, Trash2 } from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import { formatRelativeTime } from '#/views/shared/aigc/time';

interface Props {
  item: AigcKnowledge;
  modelOptions: LabelOption[];
  vectorStoreOptions: LabelOption[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  delete: [item: AigcKnowledge];
  docs: [item: AigcKnowledge];
  edit: [item: AigcKnowledge];
  index: [item: AigcKnowledge];
}>();

const vectorModelLabel = computed(() => {
  const found = props.modelOptions.find(
    (o) => o.value === props.item.vectorModelId,
  );
  return found?.label || $t('common.status.notConfigured');
});

const vectorStoreLabel = computed(() => {
  const found = props.vectorStoreOptions.find(
    (o) => o.value === props.item.vectorStoreId,
  );
  return found?.label || $t('common.status.notConfigured');
});

const metaItems = computed(() => [
  { label: $t('knowledge.card.vectorModel'), value: vectorModelLabel.value },
  { label: $t('knowledge.card.vectorStore'), value: vectorStoreLabel.value },
  {
    label: $t('knowledge.card.recall'),
    value: props.item.maxResults
      ? $t('knowledge.card.topN', { count: props.item.maxResults })
      : $t('common.status.default'),
  },
]);
</script>

<template>
  <LcCard
    :hoverable="true"
    :icon-component="Database"
    :meta-items="metaItems"
    class="h-full cursor-pointer"
    @click="emit('docs', item)"
  >
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground group-hover:text-primary"
        >
          {{ item.name || $t('knowledge.card.unnamed') }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{ item.description || $t('knowledge.card.defaultDescription') }}
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
