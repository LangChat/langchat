<script setup lang="ts">
import type { AigcDocs } from '#/api/aigc/docs';
import type { LabelOption } from '#/views/shared/aigc/options';

import { computed } from 'vue';

import {
  Activity,
  FileSearch2,
  FileText,
  PlayCircle,
  RotateCcw,
  SquarePen,
  Trash2,
} from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';
import {
  resolveDocsStatusLabel,
  resolveDocsStatusType,
} from '#/views/shared/aigc/docs-status';
import { formatRelativeTime } from '#/views/shared/aigc/time';

import { formatDocsFileSize } from './shared';

interface Props {
  item: AigcDocs;
  knowledgeOptions: LabelOption[];
}

const props = defineProps<Props>();

const emit = defineEmits<{
  delete: [item: AigcDocs];
  edit: [item: AigcDocs];
  index: [item: AigcDocs];
  preview: [item: AigcDocs];
  retry: [item: AigcDocs];
  status: [item: AigcDocs];
}>();

const knowledgeName = computed(
  () =>
    props.knowledgeOptions.find((item) => item.value === props.item.knowledgeId)
      ?.label || $t('common.status.notConfigured'),
);

const metaItems = computed(() => [
  { label: $t('docs.card.knowledgeLabel'), value: knowledgeName.value },
  { label: $t('docs.card.typeLabel'), value: props.item.ext || props.item.type || '--' },
  {
    label: $t('docs.card.sizeLabel'),
    value:
      (props.item.size ?? 0) > 0 ? formatDocsFileSize(props.item.size!) : '--',
  },
  {
    label: $t('docs.card.statusLabel'),
    value: resolveDocsStatusLabel(props.item.embedStatus),
  },
]);
</script>

<template>
  <LcCard :icon-component="FileText" :meta-items="metaItems" :show-icon="true">
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.name || $t('docs.card.unnamed') }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{
          item.content ||
          $t('docs.card.knowledgeStatus', {
            knowledge: knowledgeName,
            status: resolveDocsStatusLabel(item.embedStatus),
          })
        }}
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag
        :label="resolveDocsStatusLabel(item.embedStatus)"
        :type="resolveDocsStatusType(item.embedStatus)"
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
          v-tippy="$t('docs.actions.parsePreview')"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click="emit('preview', item)"
        >
          <FileSearch2 class="size-3.5" />
        </NButton>
        <NButton
          v-tippy="$t('docs.actions.startVectorize')"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click="emit('index', item)"
        >
          <PlayCircle class="size-3.5" />
        </NButton>
        <NButton
          v-if="item.embedStatus === 'failed'"
          v-tippy="$t('docs.actions.retryFailed')"
          circle
          class="text-muted-foreground hover:text-destructive"
          quaternary
          size="small"
          type="error"
          @click="emit('retry', item)"
        >
          <RotateCcw class="size-3.5" />
        </NButton>
        <NButton
          v-tippy="$t('docs.actions.statusDetail')"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click="emit('status', item)"
        >
          <Activity class="size-3.5" />
        </NButton>
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
          v-tippy="$t('docs.actions.deleteDoc')"
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
