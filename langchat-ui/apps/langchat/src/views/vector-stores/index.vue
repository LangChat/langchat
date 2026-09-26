<script lang="ts" setup>
import type {AigcVectorStore} from '#/api/aigc/vector-store';

import {computed, onMounted, ref, watch} from 'vue';

import {Page} from '@vben/common-ui';
import {LayoutGrid, Plus, RefreshCcw, Tag} from '@vben/icons';

import {NButton, NPagination} from 'naive-ui';

import {dialog, message} from '#/adapter/naive';
import {vectorStoreApi} from '#/api/aigc/vector-store';
import LcActionCard from '#/components/LcActionCard/index.vue';
import LcListCard from '#/components/LcListCard/index.vue';
import {VECTOR_PROVIDER_OPTIONS} from '#/views/shared/aigc/options';

import VectorStoreCard from './card.vue';
import VectorStoreEdit from './edit.vue';

const loading = ref(false);
const saving = ref(false);
const showEdit = ref(false);
const keyword = ref('');
const selectedProvider = ref('ALL');
const currentPage = ref(1);
const pageSize = ref(9);
const items = ref<AigcVectorStore[]>([]);
const currentItem = ref<null | Partial<AigcVectorStore>>(null);
const providerTagOptions = computed(() => [
  { label: '全部', value: 'ALL' },
  ...VECTOR_PROVIDER_OPTIONS.map((item) => ({
    label: item.label,
    value: String(item.value),
  })),
]);

const filteredItems = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  return items.value.filter((item) => {
    const matchKeyword =
      !query ||
      [item.name, item.provider, item.host, item.databaseName, item.tableName]
        .map((value) => String(value ?? '').toLowerCase())
        .some((value) => value.includes(query));
    const matchProvider =
      selectedProvider.value === 'ALL' ||
      item.provider === selectedProvider.value;
    return matchKeyword && matchProvider;
  });
});

const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredItems.value.slice(start, start + pageSize.value);
});

watch([keyword, selectedProvider], () => {
  currentPage.value = 1;
});

const actionItems = computed(() => [
  { key: 'refresh', label: '刷新列表', icon: RefreshCcw },
  { key: 'create', label: '新建向量库', icon: Plus },
]);

const summaryText = computed(() => {
  const total = items.value.length;
  const providers = new Set(
    items.value.map((item) => item.provider).filter(Boolean),
  ).size;
  const ready = items.value.filter((item) => Boolean(item.tableName)).length;
  return `当前共 ${total} 个向量库 · 供应商 ${providers} 类 · 已完成配置 ${ready}`;
});

async function loadList() {
  loading.value = true;
  try {
    items.value = await vectorStoreApi.list();
  } finally {
    loading.value = false;
  }
}

function handleCreate() {
  currentItem.value = null;
  showEdit.value = true;
}

function handleEdit(item: AigcVectorStore) {
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

async function handleDelete(item: AigcVectorStore) {
  if (!item.id) {
    return;
  }
  dialog.warning({
    closable: false,
    content: `删除后不可恢复，确认删除「${item.name || '未命名向量库'}」吗？`,
    negativeText: '取消',
    positiveText: '确认删除',
    title: '删除向量库',
    onPositiveClick: async () => {
      await vectorStoreApi.remove(item.id!);
      message.success('向量库已删除');
      await loadList();
    },
  });
}

async function handleSave(payload: Partial<AigcVectorStore>) {
  saving.value = true;
  try {
    if (currentItem.value?.id) {
      await vectorStoreApi.update(currentItem.value.id, payload);
      message.success('向量库已更新');
    } else {
      await vectorStoreApi.create(payload);
      message.success('向量库已创建');
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
      <div
        class="rounded-xl border border-border bg-card px-3 py-2.5 text-xs text-muted-foreground"
      >
        <span class="font-medium text-primary">{{ summaryText }}</span>
      </div>

      <LcListCard
        :active-tag="selectedProvider"
        :all-tag-icon="LayoutGrid"
        :common-tag-icon="Tag"
        :items="pagedItems"
        :loading="loading"
        search-placeholder="按名称、供应商、主机地址搜索"
        :search-value="keyword"
        :tags="providerTagOptions"
        empty-description="当前还没有向量库数据。"
        @update:active-tag="selectedProvider = $event"
        @update:search-value="keyword = $event"
      >
        <template #leading-card>
          <LcActionCard
            :actions="actionItems"
            description="将操作抽离到独立卡片，便于列表头部保持干净。"
            title="向量库操作"
            @action="handleAction"
          />
        </template>
        <template #item="{ item }">
          <VectorStoreCard
            :item="item"
            @delete="handleDelete"
            @edit="handleEdit"
          />
        </template>
        <template #empty-extra>
          <NButton type="primary" @click="handleCreate">新建向量库</NButton>
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

      <VectorStoreEdit
        v-model:show="showEdit"
        :model-value="currentItem"
        :saving="saving"
        @save="handleSave"
      />
    </div>
  </Page>
</template>
