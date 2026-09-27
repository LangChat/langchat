<script lang="ts" setup>
import type {AigcVectorStore} from '#/api/aigc/vector-store';

import {computed, onMounted, ref, watch} from 'vue';

import {Page} from '@vben/common-ui';
import {LayoutGrid, Plus, RefreshCcw, Tag} from '@vben/icons';
import {$t} from '@vben/locales';

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
  {label: $t('common.labels.all'), value: 'ALL'},
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
  {key: 'refresh', label: $t('vectorStores.actions.refreshList'), icon: RefreshCcw},
  {key: 'create', label: $t('vectorStores.actions.create'), icon: Plus},
]);

const summaryText = computed(() => {
  const total = items.value.length;
  const providers = new Set(
    items.value.map((item) => item.provider).filter(Boolean),
  ).size;
  const ready = items.value.filter((item) => Boolean(item.tableName)).length;
  return $t('vectorStores.list.summary', {providers, ready, total});
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
    content: $t('common.messages.deleteConfirmContent', {
      name: item.name || $t('vectorStores.card.unnamed'),
    }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('vectorStores.messages.deleteTitle'),
    onPositiveClick: async () => {
      await vectorStoreApi.remove(item.id!);
      message.success($t('vectorStores.messages.deleted'));
      await loadList();
    },
  });
}

async function handleSave(payload: Partial<AigcVectorStore>) {
  saving.value = true;
  try {
    if (currentItem.value?.id) {
      await vectorStoreApi.update(currentItem.value.id, payload);
      message.success($t('vectorStores.messages.updated'));
    } else {
      await vectorStoreApi.create(payload);
      message.success($t('vectorStores.messages.created'));
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
        :search-placeholder="$t('vectorStores.list.searchPlaceholder')"
        :search-value="keyword"
        :tags="providerTagOptions"
        :empty-description="$t('vectorStores.list.emptyDescription')"
        @update:active-tag="selectedProvider = $event"
        @update:search-value="keyword = $event"
      >
        <template #leading-card>
          <LcActionCard
            :actions="actionItems"
            :description="$t('vectorStores.list.actionsDescription')"
            :title="$t('vectorStores.list.actionsTitle')"
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
          <NButton type="primary" @click="handleCreate">
{{
            $t('vectorStores.actions.create')
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

      <VectorStoreEdit
        v-model:show="showEdit"
        :model-value="currentItem"
        :saving="saving"
        @save="handleSave"
      />
    </div>
  </Page>
</template>
