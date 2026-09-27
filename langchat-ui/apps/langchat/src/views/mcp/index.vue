<script lang="ts" setup>
import type {AigcMcp} from '#/api/aigc/mcp';

import {computed, onMounted, ref, watch} from 'vue';

import {Page} from '@vben/common-ui';
import {Plus, RefreshCcw} from '@vben/icons';
import {$t} from '@vben/locales';

import {NButton, NPagination} from 'naive-ui';

import {dialog, message} from '#/adapter/naive';
import {mcpApi} from '#/api/aigc/mcp';
import LcActionCard from '#/components/LcActionCard/index.vue';
import LcListCard from '#/components/LcListCard/index.vue';
import {aigcCommonTagOptions} from '#/views/shared/aigc/options';
import {parseTagList} from '#/views/shared/aigc/tags';

import McpCard from './card.vue';
import McpEdit from './edit.vue';

const loading = ref(false);
const saving = ref(false);
const showEdit = ref(false);
const keyword = ref('');
const selectedTag = ref('ALL');
const currentPage = ref(1);
const pageSize = ref(9);
const items = ref<AigcMcp[]>([]);
const currentItem = ref<null | Partial<AigcMcp>>(null);
const tagFilterOptions = computed(() => [
  {label: $t('common.labels.all'), value: 'ALL'},
  ...aigcCommonTagOptions().map((item) => ({
    label: item.label,
    value: String(item.value),
  })),
]);

const filteredItems = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return items.value.filter((item) => {
    const matchKeyword =
      !query ||
      [item.name, item.uuid, item.transport, item.siteUrl, item.tags]
        .map((value) => String(value ?? '').toLowerCase())
        .some((value) => value.includes(query));
    const tags = parseTagList(item.tags);
    const matchTag =
      selectedTag.value === 'ALL' || tags.includes(selectedTag.value);
    return matchKeyword && matchTag;
  });
});

const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredItems.value.slice(start, start + pageSize.value);
});

watch([keyword, selectedTag], () => {
  currentPage.value = 1;
});
const actionItems = computed(() => [
  {key: 'refresh', label: $t('mcp.actions.refreshList'), icon: RefreshCcw},
  {key: 'create', label: $t('mcp.actions.create'), icon: Plus},
]);

async function loadList() {
  loading.value = true;
  try {
    items.value = await mcpApi.list();
  } finally {
    loading.value = false;
  }
}

function handleCreate() {
  currentItem.value = null;
  showEdit.value = true;
}

function handleEdit(item: AigcMcp) {
  currentItem.value = item;
  showEdit.value = true;
}

function handleAction(action: { key: string }) {
  if (action.key === 'refresh') {
    void loadList();
    return;
  }
  if (action.key === 'create') {
    handleCreate();
  }
}

async function handleDelete(item: AigcMcp) {
  if (!item.id) {
    return;
  }
  dialog.warning({
    closable: false,
    content: $t('common.messages.deleteConfirmContent', {
      name: item.name || $t('mcp.card.unnamed'),
    }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('mcp.messages.deleteTitle'),
    onPositiveClick: async () => {
      await mcpApi.remove(item.id!);
      message.success($t('mcp.messages.deleted'));
      await loadList();
    },
  });
}

async function handleSave(payload: Partial<AigcMcp>) {
  saving.value = true;
  try {
    if (currentItem.value?.id) {
      await mcpApi.update(currentItem.value.id, payload);
      message.success($t('mcp.messages.updated'));
    } else {
      await mcpApi.create(payload);
      message.success($t('mcp.messages.created'));
    }
    showEdit.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

onMounted(loadList);
</script>

<template>
  <Page>
    <div class="flex flex-col gap-2">
      <LcListCard
        :active-tag="selectedTag"
        :items="pagedItems"
        :loading="loading"
        :search-placeholder="$t('mcp.list.searchPlaceholder')"
        :search-value="keyword"
        :tags="tagFilterOptions"
        @update:active-tag="selectedTag = $event"
        @update:search-value="keyword = $event"
      >
        <template #leading-card>
          <LcActionCard
            :actions="actionItems"
            :description="$t('common.messages.quickActionsDescription')"
            :title="$t('mcp.list.actionsTitle')"
            @action="handleAction"
          />
        </template>
        <template #item="{ item }">
          <McpCard :item="item" @delete="handleDelete" @edit="handleEdit" />
        </template>
        <template #empty-extra>
          <NButton type="primary" @click="handleCreate">
{{
            $t('mcp.actions.create')
          }}
</NButton>
        </template>
      </LcListCard>

      <div v-if="filteredItems.length > pageSize" class="flex justify-end">
        <NPagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="filteredItems.length"
          :page-sizes="[6, 9, 12, 18]"
          show-size-picker
        />
      </div>

      <McpEdit
        v-model:show="showEdit"
        :model-value="currentItem"
        :saving="saving"
        @save="handleSave"
      />
    </div>
  </Page>
</template>
