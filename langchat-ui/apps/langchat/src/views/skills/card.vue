<script lang="ts" setup>
import type { AigcSkill } from '#/api/aigc/skill';

import { computed } from 'vue';

import { Blocks, Download, SquarePen, Trash2 } from '@vben/icons';

import { NButton, NSpace } from 'naive-ui';

import { skillApi } from '#/api/aigc/skill';
import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';
import { parseTagList } from '#/views/shared/aigc/tags';

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

const title = computed(() => props.item.title || props.item.name || '未命名技能');
const tags = computed(() => parseTagList(props.item.tags));

const metaItems = computed(() => [
  { label: '版本', value: props.item.version || '--' },
  { label: '文件数', value: props.item.fileCount ?? '--' },
  { label: '包大小', value: formatSize(props.item.packageSize) },
  { label: '标签', value: tags.value.length > 0 ? tags.value.join(' / ') : '--' },
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

function formatTime(timestamp?: number) {
  if (!timestamp) {
    return '刚刚';
  }
  return new Date(timestamp).toLocaleString('zh-CN', { hour12: false });
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
        {{ item.description || '未填写技能描述' }}
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag
        :label="item.enabled ? '已启用' : '已停用'"
        :type="item.enabled ? 'success' : 'default'"
      />
    </template>

    <template #footer-extra>
      <div class="truncate whitespace-nowrap text-[9px] text-muted-foreground">
        {{ formatTime(item.updateTime) }}
      </div>
    </template>

    <template #footer>
      <NSpace :size="2">
        <NButton
          v-tippy="'编辑文档'"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click.stop="emit('edit', item)"
        >
          <SquarePen class="size-3" />
        </NButton>
        <NButton
          v-tippy="'启用/停用'"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click.stop="emit('toggle', item)"
        >
          <span class="text-[11px] leading-none">{{ item.enabled ? '停' : '启' }}</span>
        </NButton>
        <NButton
          v-tippy="'下载原始包'"
          circle
          class="text-muted-foreground hover:text-primary"
          quaternary
          size="small"
          @click.stop="handleDownload(item)"
        >
          <Download class="size-3" />
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
          <Trash2 class="size-3" />
        </NButton>
      </NSpace>
    </template>
  </LcCard>
</template>
