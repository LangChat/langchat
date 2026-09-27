<script setup lang="ts">
import type { AigcMcp } from '#/api/aigc/mcp';

import { computed } from 'vue';

import { PlugZap, SquarePen, Trash2 } from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton, NSpace } from 'naive-ui';

import LcCard from '#/components/LcCard/index.vue';
import LcStatusTag from '#/components/LcStatusTag/index.vue';
import {
  findOptionLabel,
  MCP_TRANSPORT_OPTIONS,
} from '#/views/shared/aigc/options';
import { formatRelativeTime } from '#/views/shared/aigc/time';

interface Props {
  item: AigcMcp;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  delete: [item: AigcMcp];
  edit: [item: AigcMcp];
}>();

const metaItems = computed(() => [
  {
    label: $t('mcp.card.transportLabel'),
    value: findOptionLabel(MCP_TRANSPORT_OPTIONS, props.item.transport),
  },
  {label: $t('mcp.card.site'), value: props.item.siteUrl || '--'},
  {label: $t('mcp.card.uuid'), value: props.item.uuid || '--'},
  {
    label: $t('mcp.card.timeout'),
    value: props.item.timeout
      ? $t('mcp.card.timeoutValue', {value: props.item.timeout})
      : $t('common.status.default'),
  },
]);
</script>

<template>
  <LcCard :icon-component="PlugZap" :meta-items="metaItems" class="cursor-pointer" hoverable @click="emit('edit', item)">
    <template #header>
      <div class="min-w-0">
        <div
          class="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary"
        >
          {{ item.name || $t('mcp.card.unnamed') }}
        </div>
      </div>
    </template>

    <template #description>
      <p class="text-[11px] text-muted-foreground line-clamp-2">
        {{
          $t('mcp.card.transportSummary', {
            transport: findOptionLabel(MCP_TRANSPORT_OPTIONS, item.transport),
            siteUrl: item.siteUrl || '--',
          })
        }}
      </p>
    </template>

    <template #header-extra>
      <LcStatusTag
        :label="item.authorized ? $t('mcp.card.authorized') : $t('mcp.card.pendingAuthorized')"
        :type="item.authorized ? 'success' : 'warning'"
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
