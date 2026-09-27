<script lang="ts" setup>
import type { VxeGridPropTypes } from '#/adapter/vxe-table';
import type {
  AigcDocs,
  KnowledgeDocumentIndexStatus,
  KnowledgeIndexStatusResult,
} from '#/api/aigc/docs';
import type {AigcKnowledge} from '#/api/aigc/knowledge';

import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {
  ArrowLeft,
  FileText,
  PlayCircle,
  RotateCcw,
  Search,
  SquarePen,
  Trash2,
  Upload,
} from '@vben/icons';
import {$t} from '@vben/locales';

import {NButton, NDrawer, NDrawerContent, NInput, NPopover, NSelect, NTag, NText,} from 'naive-ui';

import {dialog, message} from '#/adapter/naive';
import {useVbenVxeGrid} from '#/adapter/vxe-table';
import {docsApi, getKnowledgeIndexStatusApi, indexKnowledgeApi,} from '#/api/aigc/docs';
import {knowledgeApi} from '#/api/aigc/knowledge';
import LcCard from '#/components/LcCard/index.vue';
import {
  formatDocsDuration,
  formatDocsTimestamp,
  resolveDocsStatusLabel,
} from '#/views/shared/aigc/docs-status';

import DocsEdit from './edit.vue';
import {
  buildKnowledgePreviewRouteLocation,
  buildKnowledgeUploadRouteLocation,
  DOC_EMBED_STATUS,
  formatDocsFileSize,
  normalizeRouteParam,
} from './shared';

const route = useRoute();
const router = useRouter();

const initialized = ref(false);
const saving = ref(false);
const showEdit = ref(false);
const keyword = ref('');
const currentItem = ref<null | Partial<AigcDocs>>(null);
const selectedDocStatus = ref('');
const failedWorkbenchVisible = ref(false);
const statusSummary = ref<KnowledgeIndexStatusResult | null>(null);
const statusVisible = ref(false);
const statusDetail = ref<KnowledgeDocumentIndexStatus | null>(null);
const statusDetailTitle = ref('');
const knowledge = ref<AigcKnowledge | null>(null);
const selectedRowIds = ref<string[]>([]);

const knowledgeId = computed(() =>
  normalizeRouteParam(route.query.knowledgeId),
);

const statusFilterOptions = computed(() => [
  { label: $t('docs.status.all'), value: '' },
  { label: $t('docs.status.pending'), value: DOC_EMBED_STATUS.PENDING },
  { label: $t('docs.status.running'), value: DOC_EMBED_STATUS.RUNNING },
  { label: $t('docs.status.completed'), value: DOC_EMBED_STATUS.COMPLETED },
  { label: $t('docs.status.failed'), value: DOC_EMBED_STATUS.FAILED },
]);

const failedDocs = computed(() =>
  [...(statusSummary.value?.docs ?? [])]
    .filter((item) => item.embedStatus === DOC_EMBED_STATUS.FAILED)
    .sort((left, right) => (right.updateTime ?? 0) - (left.updateTime ?? 0)),
);

const hasFailedDocs = computed(() => failedDocs.value.length > 0);
const selectedRowCount = computed(() => selectedRowIds.value.length);

function resolveRowStatusDetail(row: AigcDocs) {
  return statusSummary.value?.docs.find((item) => item.docsId === row.id);
}

function resolveStatusChipClass(status?: string) {
  if (status === DOC_EMBED_STATUS.COMPLETED) {
    return 'border-success/40 bg-success/10 text-success';
  }
  if (status === DOC_EMBED_STATUS.FAILED) {
    return 'border-destructive/40 bg-destructive/10 text-destructive';
  }
  if (status === DOC_EMBED_STATUS.RUNNING) {
    return 'border-warning/40 bg-warning/10 text-warning';
  }
  return 'border-info/40 bg-info/10 text-info';
}

const failedDocsSummary = computed(() => {
  if (failedDocs.value.length === 0) {
    return $t('docs.messages.noFailedSummary');
  }
  return $t('docs.messages.failedCount', {
    count: failedDocs.value.length,
    time: formatDocsTimestamp(failedDocs.value[0]?.updateTime),
  });
});

function matchKeyword(item: AigcDocs) {
  const query = keyword.value.trim().toLowerCase();
  if (!query) {
    return true;
  }
  return [item.name, item.ext, item.url, item.content]
    .map((value) => String(value ?? '').toLowerCase())
    .some((value) => value.includes(query));
}

async function queryDocs(params: {
  page?: { currentPage: number; pageSize: number };
}) {
  if (!knowledgeId.value) {
    return {
      items: [],
      total: 0,
    };
  }

  const docs = await docsApi.list({
    knowledgeId: knowledgeId.value,
  });
  const filtered = docs.filter((item) => {
    const matchStatus =
      !selectedDocStatus.value || item.embedStatus === selectedDocStatus.value;
    return matchStatus && matchKeyword(item);
  });

  const currentPage = params.page?.currentPage ?? 1;
  const pageSize = params.page?.pageSize ?? 20;
  const start = (currentPage - 1) * pageSize;

  return {
    items: filtered.slice(start, start + pageSize),
    total: filtered.length,
  };
}

function syncSelectedRows() {
  const rows = ((gridApi.grid as any)?.getCheckboxRecords?.() ??
    []) as AigcDocs[];
  selectedRowIds.value = rows.map((item) => item.id ?? '').filter(Boolean);
}

const gridColumns = computed<VxeGridPropTypes.Columns<AigcDocs>>(() => [
  { type: 'checkbox', width: 54 },
  {
    field: 'name',
    minWidth: 240,
    align: 'left',
    slots: { default: 'docName' },
    title: $t('docs.columns.name'),
  },
  {
    field: 'ext',
    title: $t('docs.columns.ext'),
    width: 90,
    align: 'center',
  },
  {
    field: 'size',
    title: $t('docs.columns.size'),
    width: 110,
    align: 'center',
    formatter: ({ cellValue }: { cellValue: number }) =>
      formatDocsFileSize(cellValue),
  },
  {
    field: 'embedStatus',
    title: $t('docs.columns.status'),
    width: 140,
    align: 'center',
    slots: { default: 'statusColumn' },
  },
  {
    field: 'updateTime',
    width: 160,
    align: 'center',
    title: $t('docs.columns.updateTime'),
    formatter: ({ cellValue }: { cellValue: number }) =>
      formatDocsTimestamp(cellValue),
  },
  {
    field: 'actions',
    fixed: 'right',
    width: 150,
    align: 'center',
    slots: { default: 'actionColumn' },
    title: $t('common.labels.actions'),
  },
]);

const [Grid, gridApi] = useVbenVxeGrid<AigcDocs>({
  class: 'bg-transparent shadow-none',
  gridClass: 'px-0 pb-0',
  gridEvents: {
    checkboxAll: syncSelectedRows,
    checkboxChange: syncSelectedRows,
  },
  gridOptions: {
    checkboxConfig: {
      highlight: true,
    },
    columns: gridColumns.value,
    height: 'auto',
    pagerConfig: {
      pageSize: 20,
      pageSizes: [10, 20, 50, 100],
    },
    proxyConfig: {
      ajax: {
        query: queryDocs,
      },
    },
    rowConfig: {
      keyField: 'id',
    },
    toolbarConfig: {
      custom: true,
      export: true,
      refresh: true,
      zoom: true,
    },
  },
});

watch(
  gridColumns,
  (columns) => {
    gridApi.setState({ gridOptions: { columns: [...columns] } });
  },
  { immediate: true },
);

watch(selectedDocStatus, () => {
  if (initialized.value) {
    void refreshDashboard();
  }
});

watch(
  knowledgeId,
  async (value) => {
    if (!value) {
      void router.replace('/knowledges');
      return;
    }
    if (!initialized.value) {
      return;
    }
    await loadKnowledgeContext();
    await refreshDashboard();
  },
  { immediate: true },
);

onMounted(async () => {
  if (!knowledgeId.value) {
    void router.replace('/knowledges');
    return;
  }
  initialized.value = true;
  await loadKnowledgeContext();
  await refreshDashboard();
});

async function loadKnowledgeContext() {
  if (!knowledgeId.value) {
    knowledge.value = null;
    statusSummary.value = null;
    return;
  }
  const [detail, status] = await Promise.all([
    knowledgeApi.detail(knowledgeId.value),
    getKnowledgeIndexStatusApi(knowledgeId.value),
  ]);
  knowledge.value = detail;
  statusSummary.value = status;
}

function buildDocsTitle(docs?: null | Pick<AigcDocs, 'name'>) {
  return docs?.name || $t('docs.list.unnamed');
}

function openUploadPage() {
  void router.push(buildKnowledgeUploadRouteLocation(knowledgeId.value));
}

function openPreviewPage(
  docs: AigcDocs | Pick<AigcDocs, 'id' | 'knowledgeId' | 'name'>,
) {
  if (!docs.id || !docs.knowledgeId) {
    message.error($t('docs.messages.missingKnowledgeOrDocId'));
    return;
  }
  void router.push(
    buildKnowledgePreviewRouteLocation({
      docsId: docs.id,
      knowledgeId: docs.knowledgeId,
    }),
  );
}

async function refreshDashboard() {
  await gridApi.reload();
  syncSelectedRows();
  if (knowledgeId.value) {
    statusSummary.value = await getKnowledgeIndexStatusApi(knowledgeId.value);
  }
}

function handleCreate() {
  currentItem.value = {
    knowledgeId: knowledgeId.value,
  };
  showEdit.value = true;
}

function handleEdit(item: AigcDocs) {
  currentItem.value = item;
  showEdit.value = true;
}

async function handleDelete(item: AigcDocs) {
  if (!item.id) {
    return;
  }
  dialog.warning({
    closable: false,
    content: $t('common.messages.deleteConfirmContent', {
      name: buildDocsTitle(item),
    }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('docs.messages.deleteTitle'),
    onPositiveClick: async () => {
      await docsApi.remove(item.id!);
      message.success($t('docs.messages.deleted'));
      await refreshDashboard();
    },
  });
}

async function handleBatchDelete() {
  const ids = [...selectedRowIds.value];
  if (ids.length === 0) {
    message.warning($t('docs.messages.selectDocsToDelete'));
    return;
  }
  dialog.warning({
    closable: false,
    content: $t('docs.messages.batchDeleteConfirm', { count: ids.length }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('docs.messages.batchDeleteTitle'),
    onPositiveClick: async () => {
      await Promise.all(ids.map((id) => docsApi.remove(id)));
      message.success($t('docs.messages.deletedCount', { count: ids.length }));
      selectedRowIds.value = [];
      await refreshDashboard();
    },
  });
}

async function handleSave(payload: Partial<AigcDocs>) {
  saving.value = true;
  try {
    if (currentItem.value?.id) {
      await docsApi.update(currentItem.value.id, payload);
      message.success($t('docs.messages.updated'));
    } else {
      await docsApi.create(payload);
      message.success($t('docs.messages.created'));
    }
    showEdit.value = false;
    await refreshDashboard();
  } finally {
    saving.value = false;
  }
}

async function openStatusForDocs(docs: AigcDocs) {
  if (!docs.id || !docs.knowledgeId) {
    message.error($t('docs.messages.missingKnowledgeOrDocId'));
    return;
  }
  const result = await getKnowledgeIndexStatusApi(docs.knowledgeId, [docs.id]);
  const status = result.docs[0];
  if (!status) {
    message.warning($t('docs.messages.noIndexStatus'));
    return;
  }
  statusDetail.value = status;
  statusDetailTitle.value = docs.name
    ? $t('docs.messages.statusDetailTitle', { name: docs.name })
    : $t('docs.title.statusDetail');
  statusVisible.value = true;
}

async function submitDocsIndexTask(docs: AigcDocs, successText: string) {
  if (!docs.id || !docs.knowledgeId) {
    message.error($t('docs.messages.missingKnowledgeOrDocId'));
    return;
  }
  await indexKnowledgeApi(docs.knowledgeId, {
    docsIds: [docs.id],
  });
  message.success(successText);
  await refreshDashboard();
}

async function retrySingleDocs(docs: AigcDocs) {
  await submitDocsIndexTask(
    docs,
    $t('docs.messages.retrySubmittedSingle', { name: buildDocsTitle(docs) }),
  );
}

async function retryFailedDocs() {
  if (!knowledgeId.value) {
    message.error($t('docs.messages.retryNoKnowledge'));
    return;
  }
  const docsIds = failedDocs.value
    .map((item) => item.docsId)
    .filter((id): id is string => Boolean(id));
  if (docsIds.length === 0) {
    message.warning($t('docs.messages.noFailedDocs'));
    return;
  }
  await indexKnowledgeApi(knowledgeId.value, {
    docsIds,
  });
  message.success(
    $t('docs.messages.retrySubmitted', { count: docsIds.length }),
  );
  await refreshDashboard();
}

async function handleRetrySelected() {
  if (!knowledgeId.value) {
    message.error($t('knowledge.messages.missingId'));
    return;
  }
  const docsIds = [...selectedRowIds.value];
  if (docsIds.length === 0) {
    message.warning($t('docs.messages.checkAllRequired'));
    return;
  }
  await indexKnowledgeApi(knowledgeId.value, {
    docsIds,
  });
  message.success(
    $t('docs.messages.vectorizeSubmittedCount', { count: docsIds.length }),
  );
  await refreshDashboard();
}

async function handleFailedDocsStatus(
  docsStatus: KnowledgeDocumentIndexStatus,
) {
  if (!docsStatus.docsId || !knowledgeId.value) {
    message.error($t('docs.messages.missingId'));
    return;
  }
  await openStatusForDocs({
    id: docsStatus.docsId,
    knowledgeId: knowledgeId.value,
    name: docsStatus.name,
  });
}

function handleFailedDocsPreview(docsStatus: KnowledgeDocumentIndexStatus) {
  if (!docsStatus.docsId || !knowledgeId.value) {
    message.error($t('docs.messages.missingId'));
    return;
  }
  openPreviewPage({
    id: docsStatus.docsId,
    knowledgeId: knowledgeId.value,
    name: docsStatus.name,
  });
}
</script>

<template>
  <Page>
    <div
      class="flex h-full min-h-0 flex-col overflow-hidden rounded-lg border border-border bg-card p-4"
    >
      <div class="flex min-h-0 flex-1 flex-col gap-2">
        <div
          class="flex flex-col gap-2 xl:flex-row xl:items-start xl:justify-between"
        >
          <div class="min-w-0">
            <div class="text-lg font-semibold text-foreground">
              {{ knowledge?.name || $t('docs.title.list') }}
            </div>
            <div class="mt-1 text-sm leading-6 text-muted-foreground">
              {{ knowledge?.description || $t('docs.list.noKnowledgeDescription') }}
            </div>
            <div
              class="mt-1 flex flex-wrap items-center gap-x-4 gap-y-0.5 text-xs text-muted-foreground"
            >
              <span>
                {{
                  $t('docs.list.createdAt', {
                    time: formatDocsTimestamp(knowledge?.createTime),
                  })
                }}
              </span>
              <span>
                {{
                  $t('docs.list.updatedAt', {
                    time: formatDocsTimestamp(knowledge?.updateTime),
                  })
                }}
              </span>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <NButton type="primary" ghost @click="router.push('/knowledges')">
              <template #icon>
                <ArrowLeft class="size-4" />
              </template>
              {{ $t('common.actions.back') }}
            </NButton>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-3 border-t border-border pt-3">
          <NInput
            v-model:value="keyword"
            clearable
            :placeholder="$t('docs.search.placeholder')"
            style="width: 200px"
            @keyup.enter="refreshDashboard"
          />
          <NSelect
            v-model:value="selectedDocStatus"
            :options="statusFilterOptions"
            :placeholder="$t('docs.status.filterPlaceholder')"
            style="width: 150px"
          />
          <NButton type="primary" @click="refreshDashboard">
            <Search class="size-4" />
            {{ $t('docs.actions.search') }}
          </NButton>
          <div class="ml-auto">
            <NButton
              :disabled="!knowledgeId"
              type="primary"
              @click="openUploadPage"
            >
              <template #icon>
                <Upload class="size-4" />
              </template>
              {{ $t('docs.actions.upload') }}
            </NButton>
          </div>
        </div>

        <Grid class="mt-4 min-h-0 flex-1 bg-transparent">
          <template #toolbar-tools>
            <div class="flex items-center gap-2">
              <NButton
                v-if="selectedRowCount > 0"
                ghost
                size="small"
                type="primary"
                @click="handleRetrySelected"
              >
                <RotateCcw class="size-3.5" />
                {{ $t('docs.actions.reVectorize') }}
              </NButton>
              <NButton
                v-if="selectedRowCount > 0"
                ghost
                size="small"
                type="error"
                @click="handleBatchDelete"
              >
                <Trash2 class="size-3.5" />
                {{ $t('docs.actions.batchDelete') }} ({{ selectedRowCount }})
              </NButton>
            </div>
          </template>

          <template #docName="{ row }">
            <div class="min-w-0 text-left">
              <button
                class="flex max-w-full cursor-pointer items-center gap-2 rounded-md border border-transparent p-1 text-left text-sm font-medium text-primary transition-all hover:border-dashed hover:border-primary hover:bg-primary/5"
                type="button"
                @click="openPreviewPage(row)"
              >
                <FileText class="size-4 shrink-0" />
                <span class="truncate">{{ row.name || $t('docs.list.unnamed') }}</span>
              </button>
            </div>
          </template>

          <template #statusColumn="{ row }">
            <NPopover trigger="hover" :show-arrow="true">
              <template #trigger>
                <button
                  :class="resolveStatusChipClass(row.embedStatus)"
                  class="inline-flex cursor-pointer items-center gap-1.5 rounded-[var(--radius)] border px-2 py-0.5 text-xs font-medium transition-colors hover:opacity-85"
                  type="button"
                  @click="openStatusForDocs(row)"
                >
                  <span class="size-1.5 rounded-full bg-current"></span>
                  {{ resolveDocsStatusLabel(row.embedStatus) }}
                </button>
              </template>
              <div class="w-64 space-y-1.5 text-xs">
                <div class="flex items-center justify-between gap-3">
                  <span class="text-muted-foreground">
                    {{ $t('docs.card.statusLabel') }}
                  </span>
                  <span class="text-foreground">
                    {{ resolveDocsStatusLabel(row.embedStatus) }}
                  </span>
                </div>
                <div class="flex items-center justify-between gap-3">
                  <span class="text-muted-foreground">
                    {{ $t('docs.list.durationLabel') }}
                  </span>
                  <span class="text-foreground">
                    {{ formatDocsDuration(resolveRowStatusDetail(row)?.costMs) }}
                  </span>
                </div>
                <div class="flex items-center justify-between gap-3">
                  <span class="text-muted-foreground">
                    {{ $t('docs.list.lastUpdate') }}
                  </span>
                  <span class="text-foreground">
                    {{
                      formatDocsTimestamp(resolveRowStatusDetail(row)?.updateTime)
                    }}
                  </span>
                </div>
                <div
                  v-if="resolveRowStatusDetail(row)?.embedError"
                  class="line-clamp-2 rounded-md bg-destructive/10 px-2 py-1 leading-5 text-destructive"
                >
                  {{ resolveRowStatusDetail(row)?.embedError }}
                </div>
                <div class="pt-0.5 text-[11px] text-muted-foreground">
                  {{ $t('docs.list.viewFullStatus') }}
                </div>
              </div>
            </NPopover>
          </template>

          <template #actionColumn="{ row }">
            <div class="flex items-center justify-center gap-1 whitespace-nowrap">
              <NButton
                v-tippy="$t('docs.actions.vectorize')"
                quaternary
                size="small"
                type="primary"
                @click="
                  submitDocsIndexTask(
                    row,
                    $t('docs.messages.vectorizeSubmitted', {
                      name: buildDocsTitle(row),
                    }),
                  )
                "
              >
                <PlayCircle class="size-3.5" />
              </NButton>
              <NButton
                v-tippy="$t('docs.title.edit')"
                quaternary
                size="small"
                @click="handleEdit(row)"
              >
                <SquarePen class="size-3.5" />
              </NButton>
              <NButton
                v-tippy="$t('docs.actions.deleteDoc')"
                quaternary
                size="small"
                type="error"
                @click="handleDelete(row)"
              >
                <Trash2 class="size-3.5" />
              </NButton>
            </div>
          </template>

          <template #empty>
            <div
              class="flex min-h-[280px] flex-col items-center justify-center gap-3 text-center"
            >
              <div class="text-sm font-semibold text-foreground">
                {{ $t('docs.list.empty') }}
              </div>
              <div class="max-w-md text-sm leading-6 text-muted-foreground">
                {{ $t('docs.list.emptyHint') }}
              </div>
              <div class="flex flex-wrap items-center gap-2">
                <NButton secondary @click="openUploadPage">
                  {{ $t('docs.actions.uploadAndVectorize') }}
                </NButton>
                <NButton type="primary" @click="handleCreate">
                  {{ $t('docs.actions.createDoc') }}
                </NButton>
              </div>
            </div>
          </template>
        </Grid>
      </div>
    </div>

    <DocsEdit
      v-model:show="showEdit"
      :knowledge-options="
        knowledgeId
          ? [
              {
                label: knowledge?.name || $t('docs.list.currentKnowledge'),
                value: knowledgeId,
              },
            ]
          : []
      "
      :model-value="currentItem"
      :saving="saving"
      @save="handleSave"
    />

    <NDrawer v-model:show="failedWorkbenchVisible" :width="860">
      <NDrawerContent closable :title="$t('docs.title.failedWorkspace')">
        <div class="space-y-5">
          <div
            class="rounded-xl border border-dashed border-border bg-muted/40 p-3"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0">
                <div class="text-sm font-semibold text-foreground">
                  {{ $t('docs.failedDocs.description') }}
                </div>
                <div class="mt-1 text-xs leading-5 text-muted-foreground">
                  {{ failedDocsSummary }}
                </div>
              </div>
              <div class="flex items-center gap-2">
                <NButton
                  :disabled="!hasFailedDocs"
                  ghost
                  type="error"
                  @click="retryFailedDocs"
                >
                  {{ $t('docs.actions.retryAll') }}
                </NButton>
                <NButton
                  secondary
                  @click="selectedDocStatus = DOC_EMBED_STATUS.FAILED"
                >
                  {{ $t('docs.actions.syncFailedFilter') }}
                </NButton>
              </div>
            </div>
          </div>

          <div v-if="failedDocs.length > 0" class="space-y-4">
            <div
              v-for="docsStatus in failedDocs"
              :key="docsStatus.docsId"
              class="rounded-xl border border-border bg-card p-3"
            >
              <div class="flex items-start justify-between gap-4">
                <div class="min-w-0">
                  <div class="truncate text-sm font-semibold text-foreground">
                    {{
                      docsStatus.name ||
                      docsStatus.docsId ||
                      $t('docs.list.unnamed')
                    }}
                  </div>
                  <div
                    class="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted-foreground"
                  >
                    <NTag :bordered="false" round type="error">
                      {{ resolveDocsStatusLabel(docsStatus.embedStatus) }}
                    </NTag>
                    <span>
                      {{ $t('docs.failedDocs.lastUpdate') }}
                      {{ formatDocsTimestamp(docsStatus.updateTime) }}
                    </span>
                    <span>
                      {{ $t('docs.list.durationLabel') }}
                      {{ formatDocsDuration(docsStatus.costMs) }}
                    </span>
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <NButton
                    quaternary
                    size="small"
                    type="info"
                    @click="handleFailedDocsPreview(docsStatus)"
                  >
                    {{ $t('docs.actions.parsePreview') }}
                  </NButton>
                  <NButton
                    quaternary
                    size="small"
                    type="warning"
                    @click="handleFailedDocsStatus(docsStatus)"
                  >
                    {{ $t('docs.actions.statusDetail') }}
                  </NButton>
                  <NButton
                    v-if="docsStatus.docsId"
                    size="small"
                    type="error"
                    @click="
                      retrySingleDocs({
                        id: docsStatus.docsId,
                        knowledgeId,
                        name: docsStatus.name,
                      })
                    "
                  >
                    {{ $t('docs.actions.retryNow') }}
                  </NButton>
                </div>
              </div>

              <div class="mt-3 grid gap-3 lg:grid-cols-[1.4fr,1fr]">
                <div class="rounded-xl border border-border bg-muted/40 p-3">
                  <div
                    class="text-[10px] font-semibold uppercase tracking-[0.18em] text-destructive"
                  >
                    {{ $t('docs.failedDocs.errorSummary') }}
                  </div>
                  <div
                    class="mt-2 whitespace-pre-wrap text-xs leading-6 text-destructive"
                  >
                    {{ docsStatus.embedError || $t('docs.failedDocs.emptySummary') }}
                  </div>
                </div>

                <div class="rounded-xl border border-border bg-muted/40 p-3">
                  <div class="space-y-3 text-xs text-muted-foreground">
                    <div>
                      <div
                        class="text-[10px] font-semibold uppercase tracking-[0.18em] text-muted-foreground"
                      >
                        {{ $t('docs.labels.docId') }}
                      </div>
                      <div class="mt-1 break-all text-xs leading-6">
                        {{ docsStatus.docsId || '--' }}
                      </div>
                    </div>
                    <div>
                      <div
                        class="text-[10px] font-semibold uppercase tracking-[0.18em] text-muted-foreground"
                      >
                        {{ $t('docs.labels.startTime') }}
                      </div>
                      <div class="mt-1">
                        {{ formatDocsTimestamp(docsStatus.embedStartTime) }}
                      </div>
                    </div>
                    <div>
                      <div
                        class="text-[10px] font-semibold uppercase tracking-[0.18em] text-muted-foreground"
                      >
                        {{ $t('docs.labels.endTime') }}
                      </div>
                      <div class="mt-1">
                        {{ formatDocsTimestamp(docsStatus.embedEndTime) }}
                      </div>
                    </div>
                    <div>
                      <div
                        class="text-[10px] font-semibold uppercase tracking-[0.18em] text-muted-foreground"
                      >
                        {{ $t('docs.labels.indexStatus') }}
                      </div>
                      <div class="mt-1">
                        {{ docsStatus.indexingStatus ?? '--' }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            v-else
            class="rounded-xl border border-dashed border-border bg-card px-6 py-10 text-center text-sm text-muted-foreground"
          >
            {{ $t('docs.failedDocs.currentKnowledgeEmpty') }}
          </div>
        </div>
      </NDrawerContent>
    </NDrawer>

    <NDrawer v-model:show="statusVisible" :width="620">
      <NDrawerContent :title="statusDetailTitle" closable>
        <div v-if="statusDetail" class="space-y-5">
          <div class="grid gap-4 md:grid-cols-1 xl:grid-cols-3">
            <div class="rounded-xl border border-border bg-card p-3">
              <div
                class="text-[10px] uppercase tracking-[0.18em] text-muted-foreground"
              >
                {{ $t('docs.card.statusLabel') }}
              </div>
              <div class="mt-2 text-base font-semibold text-foreground">
                {{ resolveDocsStatusLabel(statusDetail.embedStatus) }}
              </div>
            </div>
            <div class="rounded-xl border border-border bg-card p-3">
              <div
                class="text-[10px] uppercase tracking-[0.18em] text-muted-foreground"
              >
                {{ $t('docs.list.durationLabel') }}
              </div>
              <div class="mt-2 text-base font-semibold text-warning">
                {{ formatDocsDuration(statusDetail.costMs) }}
              </div>
            </div>
            <div class="rounded-xl border border-border bg-card p-3">
              <div
                class="text-[10px] uppercase tracking-[0.18em] text-muted-foreground"
              >
                {{ $t('docs.labels.indexStatus') }}
              </div>
              <div class="mt-2 text-base font-semibold text-success">
                {{ statusDetail.indexingStatus ?? '--' }}
              </div>
            </div>
            <div class="rounded-xl border border-border bg-card p-3">
              <div
                class="text-[10px] uppercase tracking-[0.18em] text-muted-foreground"
              >
                {{ $t('docs.columns.updateTime') }}
              </div>
              <div class="mt-2 text-xs font-semibold text-foreground">
                {{ formatDocsTimestamp(statusDetail.updateTime) }}
              </div>
            </div>
          </div>

          <LcCard :hoverable="false">
            <template #header>
              <div class="text-sm font-semibold text-foreground">
                {{ $t('docs.failedDocs.timeline') }}
              </div>
            </template>
            <div class="space-y-3 text-sm text-muted-foreground">
              <div class="rounded-xl border border-border bg-card p-3">
                <div
                  class="text-[10px] uppercase tracking-[0.18em] text-muted-foreground"
                >
                  {{ $t('docs.labels.startTime') }}
                </div>
                <div class="mt-2">
                  {{ formatDocsTimestamp(statusDetail.embedStartTime) }}
                </div>
              </div>
              <div class="rounded-xl border border-border bg-card p-3">
                <div
                  class="text-[10px] uppercase tracking-[0.18em] text-muted-foreground"
                >
                  {{ $t('docs.labels.endTime') }}
                </div>
                <div class="mt-2">
                  {{ formatDocsTimestamp(statusDetail.embedEndTime) }}
                </div>
              </div>
            </div>
          </LcCard>

          <div class="flex justify-end gap-2">
            <NButton
              v-if="
                statusDetail.embedStatus === DOC_EMBED_STATUS.FAILED &&
                statusDetail.docsId
              "
              type="error"
              @click="
                retrySingleDocs({
                  id: statusDetail.docsId,
                  knowledgeId,
                  name: statusDetail.name,
                })
              "
            >
              {{ $t('docs.actions.retryCurrent') }}
            </NButton>
          </div>

          <LcCard :hoverable="false">
            <template #header>
              <div class="text-sm font-semibold text-foreground">
                {{ $t('docs.failedDocs.errorDetail') }}
              </div>
            </template>
            <div
              class="rounded-xl border border-dashed border-border bg-muted/40 p-3"
            >
              <NText
                class="whitespace-pre-wrap text-sm leading-7 text-muted-foreground"
              >
                {{ statusDetail.embedError || $t('docs.failedDocs.errorInfo') }}
              </NText>
            </div>
          </LcCard>
        </div>

        <div v-else class="py-10 text-center text-sm text-muted-foreground">
          {{ $t('docs.preview.emptyStatus') }}
        </div>
      </NDrawerContent>
    </NDrawer>
  </Page>
</template>
