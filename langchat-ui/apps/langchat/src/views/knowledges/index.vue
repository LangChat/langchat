<script lang="ts" setup>
import type {AigcKnowledge} from '#/api/aigc/knowledge';

import {computed, onMounted, ref, watch} from 'vue';
import {useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {Plus, RefreshCcw} from '@vben/icons';
import {$t} from '@vben/locales';

import {NButton, NPagination} from 'naive-ui';

import {dialog, message} from '#/adapter/naive';
import {indexKnowledgeApi} from '#/api/aigc/docs';
import {knowledgeApi} from '#/api/aigc/knowledge';
import LcActionCard from '#/components/LcActionCard/index.vue';
import LcListCard from '#/components/LcListCard/index.vue';
import {buildKnowledgeDocsRouteLocation} from '#/views/docs/shared';
import {useAigcLookups} from '#/views/shared/aigc/lookups';
import {aigcCommonTagOptions} from '#/views/shared/aigc/options';
import {parseTagList} from '#/views/shared/aigc/tags';

import KnowledgeCard from './card.vue';
import KnowledgeEdit from './edit.vue';

const router = useRouter();
const { loadLookups, lookups } = useAigcLookups({
  models: true,
  vectorStores: true,
});

const loading = ref(false);
const saving = ref(false);
const showEdit = ref(false);
const keyword = ref('');
const selectedTag = ref('ALL');
const currentPage = ref(1);
const pageSize = ref(6);
const items = ref<AigcKnowledge[]>([]);
const currentItem = ref<null | Partial<AigcKnowledge>>(null);
const tagFilterOptions = computed(() => [
  { label: $t('common.labels.all'), value: 'ALL' },
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
      [item.name, item.description, item.tags]
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
  { key: 'refresh', label: $t('common.actions.refreshList'), icon: RefreshCcw },
  { key: 'create', label: $t('knowledge.actions.create'), icon: Plus },
]);

function jumpToKnowledge(knowledgeId?: string) {
  void router.push(buildKnowledgeDocsRouteLocation(knowledgeId));
}

async function loadList() {
  loading.value = true;
  try {
    items.value = await knowledgeApi.list();
  } finally {
    loading.value = false;
  }
}

async function initializePage() {
  await Promise.all([loadLookups(), loadList()]);
}

function handleCreate() {
  currentItem.value = null;
  showEdit.value = true;
}

function handleEdit(item: AigcKnowledge) {
  currentItem.value = item;
  showEdit.value = true;
}

function handleAction(action: { key: string }) {
  if (action.key === 'refresh') {
    void initializePage();
    return;
  }
  if (action.key === 'create') {
    handleCreate();
  }
}

async function handleDelete(item: AigcKnowledge) {
  if (!item.id) {
    return;
  }
  dialog.warning({
    closable: false,
    content: $t('common.messages.deleteConfirmContent', {
      name: item.name || $t('knowledge.card.unnamed'),
    }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('knowledge.messages.deleteTitle'),
    onPositiveClick: async () => {
      await knowledgeApi.remove(item.id!);
      message.success($t('knowledge.messages.deleted'));
      await loadList();
    },
  });
}

async function handleSave(payload: Partial<AigcKnowledge>) {
  saving.value = true;
  try {
    if (currentItem.value?.id) {
      await knowledgeApi.update(currentItem.value.id, payload);
      message.success($t('knowledge.messages.updated'));
    } else {
      await knowledgeApi.create(payload);
      message.success($t('knowledge.messages.created'));
    }
    showEdit.value = false;
    await loadList();
  } finally {
    saving.value = false;
  }
}

async function handleIndex(item?: AigcKnowledge | string) {
  const knowledgeId = typeof item === 'string' ? item : item?.id;
  if (!knowledgeId) {
    message.error($t('knowledge.messages.missingId'));
    return;
  }
  await indexKnowledgeApi(knowledgeId, {});
  message.success($t('knowledge.messages.vectorizeSubmitted'));
}

onMounted(initializePage);
</script>

<template>
  <Page>
    <div class="flex flex-col gap-2">
      <LcListCard
        :active-tag="selectedTag"
        :items="pagedItems"
        :loading="loading"
        :search-value="keyword"
        :tags="tagFilterOptions"
        :search-placeholder="$t('knowledge.list.searchPlaceholder')"
        @update:active-tag="selectedTag = $event"
        @update:search-value="keyword = $event"
      >
        <template #leading-card>
          <LcActionCard
            :actions="actionItems"
            :description="$t('common.messages.quickActionsDescription')"
            :title="$t('knowledge.quickActions.title')"
            @action="handleAction"
          />
        </template>
        <template #item="{ item }">
          <KnowledgeCard
            :item="item"
            :model-options="lookups.models"
            :vector-store-options="lookups.vectorStores"
            @delete="handleDelete"
            @docs="jumpToKnowledge(item.id)"
            @edit="handleEdit"
            @index="handleIndex"
          />
        </template>
        <template #empty-extra>
          <NButton type="primary" @click="handleCreate">{{ $t('knowledge.actions.create') }}</NButton>
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

      <KnowledgeEdit
        v-model:show="showEdit"
        :model-options="lookups.models"
        :model-value="currentItem"
        :saving="saving"
        :vector-store-options="lookups.vectorStores"
        @save="handleSave"
      />
    </div>
  </Page>
</template>
