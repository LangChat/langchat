<script lang="ts" setup>
import type { AigcSkill } from '#/api/aigc/skill';

import { computed } from 'vue';

import {
  Blocks,
  Download,
  Power,
  PowerOff,
  SquarePen,
  Trash2,
} from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton, NSpace } from 'naive-ui';

import { skillApi } from '#/api/aigc/skill';
import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';
import { parseTagList } from '#/views/shared/aigc/tags';
import { formatRelativeTime } from '#/views/shared/aigc/time';

interface Props {
  item: AigcSkill;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  delete: [item: AigcSkill];
  edit: [item: AigcSkill];
  toggle: [item: AigcSkill];
}>();

async function handleDownload(item: AigcSkill) {
  if (!item.id) {
    return;
  }
  const blob = await skillApi.downloadPackage(item.id);
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = item.ossFilename || `${item.name || 'skill'}.zip`;
  anchor.click();
  URL.revokeObjectURL(url);
}

const title = computed(
  () => props.item.title || props.item.name || $t('skills.empty.unnamed'),
);
const tags = computed(() => parseTagList(props.item.tags));

const metaItems = computed(() => [
  { label: $t('skills.card.version'), value: props.item.version || '--' },
  { label: $t('skills.card.filesCount'), value: props.item.fileCount ?? '--' },
  {
    label: $t('skills.card.packageSize'),
    value: formatSize(props.item.packageSize),
  },
  {
    label: $t('skills.card.tags'),
    value: tags.value.length > 0 ? tags.value.join(' / ') : '--',
  },
]);

function formatSize(size?: number) {
  if (!size || size <= 0) {
    return '--';
  }
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}
</script>

<template>
  <LcCard
    :icon-component="Blocks"
    :icon-size="36"
    :meta-items="metaItems"
    class="cursor-pointer"
    hoverable
    @click="emit('edit', item)"
  >
    <template #header>
      <div class="min-w-0">
        <div
          class="flex items-center gap-1.5 truncate text-[13px] font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ title }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{ item.description || $t('skills.empty.noDescription') }}
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag
        :label="item.enabled ? $t('skills.card.enabled') : $t('skills.card.disabled')"
        :type="item.enabled ? 'success' : 'default'"
      />
    </template>

    <template #footer-extra>
      <div class="truncate whitespace-nowrap text-[9px] text-muted-foreground">
        {{ formatRelativeTime(item.updateTime) }}
      </div>
    </template>

    <template #footer>
      <NSpace :size="2">
        <NButton
          v-tippy="$t('skills.card.editDocs')"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click.stop="emit('edit', item)"
        >
          <SquarePen class="size-3" />
        </NButton>
        <NButton
          v-tippy="
            item.enabled
              ? $t('skills.card.toggleDisable')
              : $t('skills.card.toggleEnable')
          "
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click.stop="emit('toggle', item)"
        >
          <PowerOff v-if="item.enabled" class="size-3.5" />
          <Power v-else class="size-3.5" />
        </NButton>
        <NButton
          v-tippy="$t('skills.card.downloadPackage')"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click.stop="handleDownload(item)"
        >
          <Download class="size-3" />
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
          <Trash2 class="size-3" />
        </NButton>
      </NSpace>
    </template>
  </LcCard>
</template>
