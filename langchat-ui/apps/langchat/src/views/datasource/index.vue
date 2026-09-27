<script lang="ts" setup>
import type { AigcDatasource } from '#/api/aigc/datasource';

import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { Plus, RefreshCcw } from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton, NPagination } from 'naive-ui';

import { dialog, message } from '#/adapter/naive';
import { deleteDataSource, listDataSources } from '#/api/aigc/datasource';
import LcActionCard from '#/components/LcActionCard/index.vue';
import LcListCard from '#/components/LcListCard/index.vue';

import DatasourceCard from './card.vue';
import DatasourceEdit from './edit.vue';

const router = useRouter();

const loading = ref(false);
const saving = ref(false);
const currentPage = ref(1);
const pageSize = ref(6);
const items = ref<AigcDatasource[]>([]);
const currentItem = ref<null | Partial<AigcDatasource>>(null);
const showEdit = ref(false);

const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return items.value.slice(start, start + pageSize.value);
});

const actionItems = computed(() => [
  {
    key: 'refresh',
    label: $t('common.actions.refreshList'),
    icon: RefreshCcw,
  },
  { key: 'create', label: $t('datasource.list.create'), icon: Plus },
]);

async function loadList() {
  loading.value = true;
  try {
    items.value = await listDataSources();
  } finally {
    loading.value = false;
  }
}

function openDetail(item: AigcDatasource) {
  if (!item.id) {
    return;
  }
  void router.push(`/datasources/${item.id}/detail`);
}

function openCreate() {
  currentItem.value = null;
  showEdit.value = true;
}

function handleEdit(item: AigcDatasource) {
  currentItem.value = item;
  showEdit.value = true;
}

async function handleRemove(item: AigcDatasource) {
  if (!item.id) {
    return;
  }
  dialog.warning({
    closable: false,
    content: $t('common.messages.deleteConfirmContent', {
      name: item.name || $t('datasource.card.unnamed'),
    }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('datasource.messages.deleteTitle'),
    onPositiveClick: async () => {
      await deleteDataSource(item.id!);
      message.success($t('datasource.messages.deleted'));
      await loadList();
    },
  });
}

function handleAction(action: { key: string }) {
  if (action.key === 'refresh') {
    void loadList();
    return;
  }
  if (action.key === 'create') {
    openCreate();
  }
}

async function handleSave(_payload: Partial<AigcDatasource>) {
  saving.value = true;
  try {
    // 具体保存逻辑由 DatasourceEdit 通过 api 调用，这里只刷新列表
    await loadList();
    showEdit.value = false;
  } finally {
    saving.value = false;
  }
}

async function initializePage() {
  await loadList();
}

onMounted(initializePage);
</script>

<template>
  <Page>
    <div class="flex flex-col gap-2">
      <LcListCard
        :empty-description="$t('datasource.list.emptyDescription')"
        :empty-hint="$t('datasource.list.emptyHint')"
        :items="pagedItems"
        :loading="loading"
        :search-placeholder="$t('datasource.list.searchPlaceholder')"
        @update:search-value="currentPage = 1"
      >
        <template #leading-card>
          <LcActionCard
            :actions="actionItems"
            :description="$t('datasource.list.actionsDescription')"
            :title="$t('datasource.list.actionsTitle')"
            @action="handleAction"
          />
        </template>
        <template #item="{ item }">
          <DatasourceCard
            :item="item"
            @delete="handleRemove"
            @detail="openDetail"
            @edit="handleEdit"
          />
        </template>
        <template #empty-extra>
          <NButton type="primary" @click="openCreate">{{
            $t('datasource.list.create')
          }}</NButton>
        </template>
      </LcListCard>

      <div v-if="items.length > pageSize" class="flex justify-end">
        <NPagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="items.length"
          :page-sizes="[6, 9, 12, 18]"
          show-size-picker
        />
      </div>

      <DatasourceEdit
        v-model:show="showEdit"
        :model-value="currentItem"
        :saving="saving"
        @save="handleSave"
      />
    </div>
  </Page>
</template>
