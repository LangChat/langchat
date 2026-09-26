<script lang="ts" setup>
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
  { label: '全部状态', value: '' },
  { label: '待处理', value: DOC_EMBED_STATUS.PENDING },
  { label: '执行中', value: DOC_EMBED_STATUS.RUNNING },
  { label: '已完成', value: DOC_EMBED_STATUS.COMPLETED },
  { label: '失败', value: DOC_EMBED_STATUS.FAILED },
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
    return '当前没有失败文档。';
  }
  return `共 ${failedDocs.value.length} 条失败文档，最近更新时间 ${formatDocsTimestamp(failedDocs.value[0]?.updateTime)}。`;
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
    columns: [
      { type: 'checkbox', width: 54 },
      {
        field: 'name',
        minWidth: 240,
        align: 'left',
        slots: { default: 'docName' },
        title: '文档名称',
      },
      { field: 'ext', title: '后缀', width: 90, align: 'center' },
      {
        field: 'size',
        title: '大小',
        width: 110,
        align: 'center',
        formatter: ({ cellValue }: { cellValue: number }) =>
          formatDocsFileSize(cellValue),
      },
      {
        field: 'embedStatus',
        title: '向量化状态',
        width: 140,
        align: 'center',
        slots: { default: 'statusColumn' },
      },
      {
        field: 'updateTime',
        width: 160,
        align: 'center',
        title: '更新时间',
        formatter: ({ cellValue }: { cellValue: number }) =>
          formatDocsTimestamp(cellValue),
      },
      {
        field: 'actions',
        fixed: 'right',
        width: 150,
        align: 'center',
        slots: { default: 'actionColumn' },
        title: '操作',
      },
    ],
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
  return docs?.name || '未命名文档';
}

function openUploadPage() {
  void router.push(buildKnowledgeUploadRouteLocation(knowledgeId.value));
}

function openPreviewPage(
  docs: AigcDocs | Pick<AigcDocs, 'id' | 'knowledgeId' | 'name'>,
) {
  if (!docs.id || !docs.knowledgeId) {
    message.error('当前文档缺少知识库或文档 ID');
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
    content: `删除后不可恢复，确认删除「${buildDocsTitle(item)}」吗？`,
    negativeText: '取消',
    positiveText: '确认删除',
    title: '删除文档',
    onPositiveClick: async () => {
      await docsApi.remove(item.id!);
      message.success('文档已删除');
      await refreshDashboard();
    },
  });
}

async function handleBatchDelete() {
  const ids = [...selectedRowIds.value];
  if (ids.length === 0) {
    message.warning('请先勾选要删除的文档');
    return;
  }
  dialog.warning({
    closable: false,
    content: `删除后不可恢复，确认批量删除选中的 ${ids.length} 篇文档吗？`,
    negativeText: '取消',
    positiveText: '确认删除',
    title: '批量删除文档',
    onPositiveClick: async () => {
      await Promise.all(ids.map((id) => docsApi.remove(id)));
      message.success(`已删除 ${ids.length} 篇文档`);
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
      message.success('文档已更新');
    } else {
      await docsApi.create(payload);
      message.success('文档已创建');
    }
    showEdit.value = false;
    await refreshDashboard();
  } finally {
    saving.value = false;
  }
}

async function openStatusForDocs(docs: AigcDocs) {
  if (!docs.id || !docs.knowledgeId) {
    message.error('当前文档缺少知识库或文档 ID');
    return;
  }
  const result = await getKnowledgeIndexStatusApi(docs.knowledgeId, [docs.id]);
  const status = result.docs[0];
  if (!status) {
    message.warning('未查询到当前文档的索引状态');
    return;
  }
  statusDetail.value = status;
  statusDetailTitle.value = docs.name || '文档向量化状态';
  statusVisible.value = true;
}

async function submitDocsIndexTask(docs: AigcDocs, successText: string) {
  if (!docs.id || !docs.knowledgeId) {
    message.error('当前文档缺少知识库或文档 ID');
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
    `已重新提交「${buildDocsTitle(docs)}」的向量化任务`,
  );
}

async function retryFailedDocs() {
  if (!knowledgeId.value) {
    message.error('请先选择要重试的知识库');
    return;
  }
  const docsIds = failedDocs.value
    .map((item) => item.docsId)
    .filter(Boolean);
  if (docsIds.length === 0) {
    message.warning('当前没有失败文档可重试');
    return;
  }
  await indexKnowledgeApi(knowledgeId.value, {
    docsIds,
  });
  message.success(`已重新提交 ${docsIds.length} 个失败文档的向量化任务`);
  await refreshDashboard();
}

async function handleRetrySelected() {
  if (!knowledgeId.value) {
    message.error('当前知识库缺少 ID');
    return;
  }
  const docsIds = [...selectedRowIds.value];
  if (docsIds.length === 0) {
    message.warning('请先勾选要向量化的文档');
    return;
  }
  await indexKnowledgeApi(knowledgeId.value, {
    docsIds,
  });
  message.success(`已提交 ${docsIds.length} 篇文档的向量化任务`);
  await refreshDashboard();
}

async function handleFailedDocsStatus(
  docsStatus: KnowledgeDocumentIndexStatus,
) {
  if (!docsStatus.docsId || !knowledgeId.value) {
    message.error('当前失败文档缺少必要参数');
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
    message.error('当前失败文档缺少必要参数');
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
              {{ knowledge?.name || '文档列表' }}
            </div>
            <div class="mt-1 text-sm leading-6 text-muted-foreground">
              {{ knowledge?.description || '当前知识库暂无描述' }}
            </div>
            <div
              class="mt-1 flex flex-wrap items-center gap-x-4 gap-y-0.5 text-xs text-muted-foreground"
            >
              <span>创建时间 {{ formatDocsTimestamp(knowledge?.createTime) }}</span>
              <span>更新时间 {{ formatDocsTimestamp(knowledge?.updateTime) }}</span>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <NButton type="primary" ghost @click="router.push('/knowledges')">
              <template #icon>
                <ArrowLeft class="size-4" />
              </template>
              返回
            </NButton>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-3 border-t border-border pt-3">
          <NInput
            v-model:value="keyword"
            clearable
            placeholder="搜索文档"
            style="width: 200px"
            @keyup.enter="refreshDashboard"
          />
          <NSelect
            v-model:value="selectedDocStatus"
            :options="statusFilterOptions"
            placeholder="筛选向量化状态"
            style="width: 150px"
          />
          <NButton type="primary" @click="refreshDashboard">
            <Search class="size-4" />
            搜索
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
              上传文档
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
                重新向量化
              </NButton>
              <NButton
                v-if="selectedRowCount > 0"
                ghost
                size="small"
                type="error"
                @click="handleBatchDelete"
              >
                <Trash2 class="size-3.5" />
                批量删除 ({{ selectedRowCount }})
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
                <span class="truncate">{{ row.name || '未命名文档' }}</span>
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
                  <span class="text-muted-foreground">状态</span>
                  <span class="text-foreground">
                    {{ resolveDocsStatusLabel(row.embedStatus) }}
                  </span>
                </div>
                <div class="flex items-center justify-between gap-3">
                  <span class="text-muted-foreground">耗时</span>
                  <span class="text-foreground">
                    {{ formatDocsDuration(resolveRowStatusDetail(row)?.costMs) }}
                  </span>
                </div>
                <div class="flex items-center justify-between gap-3">
                  <span class="text-muted-foreground">最后更新</span>
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
                  点击查看完整状态详情
                </div>
              </div>
            </NPopover>
          </template>

          <template #actionColumn="{ row }">
            <div class="flex items-center justify-center gap-1 whitespace-nowrap">
              <NButton
                v-tippy="'执行向量化'"
                quaternary
                size="small"
                type="primary"
                @click="
                  submitDocsIndexTask(
                    row,
                    `已提交「${buildDocsTitle(row)}」向量化任务`,
                  )
                "
              >
                <PlayCircle class="size-3.5" />
              </NButton>
              <NButton
                v-tippy="'编辑文档'"
                quaternary
                size="small"
                @click="handleEdit(row)"
              >
                <SquarePen class="size-3.5" />
              </NButton>
              <NButton
                v-tippy="'删除文档'"
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
                当前知识库还没有文档
              </div>
              <div class="max-w-md text-sm leading-6 text-muted-foreground">
                你可以先进入上传向量化子页面批量上传文件，或者直接新建一条文档记录。
              </div>
              <div class="flex flex-wrap items-center gap-2">
                <NButton secondary @click="openUploadPage">
                  上传向量化
                </NButton>
                <NButton type="primary" @click="handleCreate">
                  新建文档
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
          ? [{ label: knowledge?.name || '当前知识库', value: knowledgeId }]
          : []
      "
      :model-value="currentItem"
      :saving="saving"
      @save="handleSave"
    />

    <NDrawer v-model:show="failedWorkbenchVisible" :width="860">
      <NDrawerContent closable title="失败文档工作台">
        <div class="space-y-5">
          <div
            class="rounded-xl border border-dashed border-border bg-muted/40 p-3"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0">
                <div class="text-sm font-semibold text-foreground">
                  失败文档集中处理
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
                  全部重试
                </NButton>
                <NButton
                  secondary
                  @click="selectedDocStatus = DOC_EMBED_STATUS.FAILED"
                >
                  同步失败筛选
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
                    {{ docsStatus.name || docsStatus.docsId || '未命名文档' }}
                  </div>
                  <div
                    class="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted-foreground"
                  >
                    <NTag :bordered="false" round type="error">
                      {{ resolveDocsStatusLabel(docsStatus.embedStatus) }}
                    </NTag>
                    <span>最后更新时间
                      {{ formatDocsTimestamp(docsStatus.updateTime) }}</span>
                    <span>耗时 {{ formatDocsDuration(docsStatus.costMs) }}</span>
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <NButton
                    quaternary
                    size="small"
                    type="info"
                    @click="handleFailedDocsPreview(docsStatus)"
                  >
                    分段预览
                  </NButton>
                  <NButton
                    quaternary
                    size="small"
                    type="warning"
                    @click="handleFailedDocsStatus(docsStatus)"
                  >
                    状态详情
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
                    立即重试
                  </NButton>
                </div>
              </div>

              <div class="mt-3 grid gap-3 lg:grid-cols-[1.4fr,1fr]">
                <div class="rounded-xl border border-border bg-muted/40 p-3">
                  <div
                    class="text-[10px] font-semibold uppercase tracking-[0.18em] text-destructive"
                  >
                    错误摘要
                  </div>
                  <div
                    class="mt-2 whitespace-pre-wrap text-xs leading-6 text-destructive"
                  >
                    {{ docsStatus.embedError || '当前没有错误详情。' }}
                  </div>
                </div>

                <div class="rounded-xl border border-border bg-muted/40 p-3">
                  <div class="space-y-3 text-xs text-muted-foreground">
                    <div>
                      <div
                        class="text-[10px] font-semibold uppercase tracking-[0.18em] text-muted-foreground"
                      >
                        文档 ID
                      </div>
                      <div class="mt-1 break-all text-xs leading-6">
                        {{ docsStatus.docsId || '--' }}
                      </div>
                    </div>
                    <div>
                      <div
                        class="text-[10px] font-semibold uppercase tracking-[0.18em] text-muted-foreground"
                      >
                        开始时间
                      </div>
                      <div class="mt-1">
                        {{ formatDocsTimestamp(docsStatus.embedStartTime) }}
                      </div>
                    </div>
                    <div>
                      <div
                        class="text-[10px] font-semibold uppercase tracking-[0.18em] text-muted-foreground"
                      >
                        结束时间
                      </div>
                      <div class="mt-1">
                        {{ formatDocsTimestamp(docsStatus.embedEndTime) }}
                      </div>
                    </div>
                    <div>
                      <div
                        class="text-[10px] font-semibold uppercase tracking-[0.18em] text-muted-foreground"
                      >
                        索引标记
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
            当前知识库没有失败文档，不需要进入故障恢复流程。
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
                状态
              </div>
              <div class="mt-2 text-base font-semibold text-foreground">
                {{ resolveDocsStatusLabel(statusDetail.embedStatus) }}
              </div>
            </div>
            <div class="rounded-xl border border-border bg-card p-3">
              <div
                class="text-[10px] uppercase tracking-[0.18em] text-muted-foreground"
              >
                耗时
              </div>
              <div class="mt-2 text-base font-semibold text-warning">
                {{ formatDocsDuration(statusDetail.costMs) }}
              </div>
            </div>
            <div class="rounded-xl border border-border bg-card p-3">
              <div
                class="text-[10px] uppercase tracking-[0.18em] text-muted-foreground"
              >
                索引标记
              </div>
              <div class="mt-2 text-base font-semibold text-success">
                {{ statusDetail.indexingStatus ?? '--' }}
              </div>
            </div>
            <div class="rounded-xl border border-border bg-card p-3">
              <div
                class="text-[10px] uppercase tracking-[0.18em] text-muted-foreground"
              >
                更新时间
              </div>
              <div class="mt-2 text-xs font-semibold text-foreground">
                {{ formatDocsTimestamp(statusDetail.updateTime) }}
              </div>
            </div>
          </div>

          <LcCard :hoverable="false">
            <template #header>
              <div class="text-sm font-semibold text-foreground">
                执行时间线
              </div>
            </template>
            <div class="space-y-3 text-sm text-muted-foreground">
              <div class="rounded-xl border border-border bg-card p-3">
                <div
                  class="text-[10px] uppercase tracking-[0.18em] text-muted-foreground"
                >
                  开始时间
                </div>
                <div class="mt-2">
                  {{ formatDocsTimestamp(statusDetail.embedStartTime) }}
                </div>
              </div>
              <div class="rounded-xl border border-border bg-card p-3">
                <div
                  class="text-[10px] uppercase tracking-[0.18em] text-muted-foreground"
                >
                  结束时间
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
              重试当前文档
            </NButton>
          </div>

          <LcCard :hoverable="false">
            <template #header>
              <div class="text-sm font-semibold text-foreground">错误详情</div>
            </template>
            <div
              class="rounded-xl border border-dashed border-border bg-muted/40 p-3"
            >
              <NText
                class="whitespace-pre-wrap text-sm leading-7 text-muted-foreground"
              >
                {{ statusDetail.embedError || '当前没有错误信息。' }}
              </NText>
            </div>
          </LcCard>
        </div>

        <div v-else class="py-10 text-center text-sm text-muted-foreground">
          当前暂无状态详情。
        </div>
      </NDrawerContent>
    </NDrawer>
  </Page>
</template>
