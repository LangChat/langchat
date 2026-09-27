<script lang="ts" setup>
import type {
  AigcDocs,
  KnowledgeDocumentIndexStatus,
} from '#/api/aigc/docs';
import type {AigcSegment} from '#/api/aigc/segment';

import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {
  ArrowLeft,
  ChevronsUpDown,
  FileText,
  RefreshCcw,
  RotateCcw,
  SquarePen,
  Trash2,
} from '@vben/icons';
import {$t} from '@vben/locales';

import {NButton, NCheckbox, NInput, NSwitch, NTag} from 'naive-ui';

import {dialog, message} from '#/adapter/naive';
import {
  docsApi,
  getKnowledgeIndexStatusApi,
  indexKnowledgeApi,
} from '#/api/aigc/docs';
import {
  deleteSegmentApi,
  deleteSegmentsApi,
  listKnowledgeSegmentsApi,
  reindexSegmentApi,
  updateSegmentContentApi,
  updateSegmentEnabledApi,
} from '#/api/aigc/segment';
import {
  formatDocsDuration,
  formatDocsTimestamp,
  resolveDocsStatusLabel,
  resolveDocsStatusType,
} from '#/views/shared/aigc/docs-status';
import {useAigcLookups} from '#/views/shared/aigc/lookups';

import {
  buildKnowledgeDocsRouteLocation,
  docParseModeOptions,
  formatDocsCharCount,
  formatDocsFileSize,
  normalizeRouteParam,
  parseDocsIngestionConfig,
} from './shared';

const route = useRoute();
const router = useRouter();
const { lookups, loadLookups } = useAigcLookups({
  knowledges: true,
});

const initialized = ref(false);
const docsOptions = ref<AigcDocs[]>([]);
const segments = ref<AigcSegment[]>([]);
const segmentsLoading = ref(false);
const statusDetail = ref<KnowledgeDocumentIndexStatus | null>(null);
const selectedDoc = ref<AigcDocs | null>(null);
const expandedIds = ref<string[]>([]);
const selectedIds = ref<string[]>([]);
const editingId = ref('');
const editingContent = ref('');
const pendingSegmentId = ref('');

const knowledgeId = computed(() =>
  normalizeRouteParam(route.query.knowledgeId),
);
const docsId = computed(() => normalizeRouteParam(route.params.docsId));

const knowledgeLabel = computed(
  () =>
    lookups.value.knowledges.find((item) => item.value === knowledgeId.value)
      ?.label || $t('knowledge.card.unnamed'),
);

const segmentCount = computed(() => segments.value.length);
const charCount = computed(() =>
  segments.value.reduce(
    (total, item) => total + (item.content?.length ?? 0),
    0,
  ),
);
const segmentConfig = computed(() =>
  parseDocsIngestionConfig(selectedDoc.value?.ingestionConfig),
);
const parseModeLabel = computed(
  () =>
    docParseModeOptions().find(
      (item) => item.value === segmentConfig.value.parseMode,
    )?.label ?? $t('docs.upload.parserSummaryDefault'),
);
const selectedCount = computed(() => selectedIds.value.length);
const allSelected = computed(
  () => segmentCount.value > 0 && selectedCount.value === segmentCount.value,
);
const allExpanded = computed(
  () => segmentCount.value > 0 && expandedIds.value.length === segmentCount.value,
);

watch(
  [knowledgeId, docsId],
  async () => {
    if (!initialized.value) {
      return;
    }
    await bootstrapPage();
  },
  { immediate: true },
);

onMounted(async () => {
  await loadLookups();
  initialized.value = true;
  await bootstrapPage();
});

async function bootstrapPage() {
  if (!knowledgeId.value || !docsId.value) {
    void router.replace('/knowledges');
    return;
  }

  const docs = await docsApi.list({
    knowledgeId: knowledgeId.value,
  });
  docsOptions.value = docs;
  selectedDoc.value = docs.find((item) => item.id === docsId.value) ?? null;

  if (!selectedDoc.value) {
    message.warning($t('docs.messages.docNotFound'));
    void router.replace(buildKnowledgeDocsRouteLocation(knowledgeId.value));
    return;
  }

  await Promise.all([loadStatusDetail(), loadSegments()]);
}

async function loadStatusDetail() {
  if (!knowledgeId.value || !docsId.value) {
    statusDetail.value = null;
    return;
  }
  const result = await getKnowledgeIndexStatusApi(knowledgeId.value, [
    docsId.value,
  ]);
  statusDetail.value = result.docs[0] ?? null;
}

async function loadSegments() {
  if (!knowledgeId.value || !docsId.value) {
    segments.value = [];
    return;
  }
  segmentsLoading.value = true;
  try {
    segments.value = await listKnowledgeSegmentsApi(
      knowledgeId.value,
      docsId.value,
    );
    expandedIds.value = [];
    selectedIds.value = [];
    cancelEdit();
  } finally {
    segmentsLoading.value = false;
  }
}

function segmentKey(segment: AigcSegment) {
  return segment.id ?? '';
}

function isExpanded(segment: AigcSegment) {
  return expandedIds.value.includes(segmentKey(segment));
}

function isSelected(segment: AigcSegment) {
  return selectedIds.value.includes(segmentKey(segment));
}

function isDisabled(segment: AigcSegment) {
  return segment.enabled === false;
}

function isEditing(segment: AigcSegment) {
  return editingId.value === segmentKey(segment);
}

function toggleSegment(segment: AigcSegment) {
  const key = segmentKey(segment);
  if (editingId.value === key) {
    return;
  }
  expandedIds.value = isExpanded(segment)
    ? expandedIds.value.filter((item) => item !== key)
    : [...expandedIds.value, key];
}

function toggleExpandedAll() {
  expandedIds.value = allExpanded.value
    ? []
    : segments.value.map((segment) => segmentKey(segment));
}

function toggleSelected(segment: AigcSegment, checked: boolean) {
  const key = segmentKey(segment);
  selectedIds.value = checked
    ? [...selectedIds.value, key]
    : selectedIds.value.filter((item) => item !== key);
}

function toggleSelectedAll(checked: boolean) {
  selectedIds.value = checked
    ? segments.value.map((segment) => segmentKey(segment))
    : [];
}

function startEdit(segment: AigcSegment) {
  editingId.value = segmentKey(segment);
  editingContent.value = segment.content ?? '';
  expandedIds.value = [...expandedIds.value, segmentKey(segment)];
}

function cancelEdit() {
  editingId.value = '';
  editingContent.value = '';
}

async function saveSegment(segment: AigcSegment, reindex = false) {
  const content = editingContent.value.trim();
  if (!content) {
    message.warning($t('docs.preview.segmentContentEmpty'));
    return;
  }
  const key = segmentKey(segment);
  pendingSegmentId.value = key;
  try {
    await updateSegmentContentApi(knowledgeId.value, key, { content });
    if (reindex) {
      await reindexSegmentApi(knowledgeId.value, key);
      message.success($t('docs.preview.segmentReindexed'));
    } else {
      message.success($t('docs.preview.segmentSaved'));
    }
    cancelEdit();
    await Promise.all([loadSegments(), loadStatusDetail()]);
  } finally {
    pendingSegmentId.value = '';
  }
}

async function handleReindex(segment: AigcSegment) {
  const key = segmentKey(segment);
  pendingSegmentId.value = key;
  try {
    await reindexSegmentApi(knowledgeId.value, key);
    message.success($t('docs.preview.segmentReindexed'));
    await loadStatusDetail();
  } finally {
    pendingSegmentId.value = '';
  }
}

async function handleToggleEnabled(segment: AigcSegment, enabled: boolean) {
  const key = segmentKey(segment);
  pendingSegmentId.value = key;
  try {
    await updateSegmentEnabledApi(knowledgeId.value, key, { enabled });
    message.success(
      enabled
        ? $t('docs.preview.segmentEnabled')
        : $t('docs.preview.segmentDisabled'),
    );
    await Promise.all([loadSegments(), loadStatusDetail()]);
  } finally {
    pendingSegmentId.value = '';
  }
}

function confirmDeleteSegment(segment: AigcSegment) {
  dialog.warning({
    closable: false,
    content: $t('docs.preview.deleteSegmentConfirm', {
      index: (segment.position ?? 0) + 1,
    }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('docs.preview.deleteSegmentTitle'),
    onPositiveClick: async () => {
      await deleteSegmentApi(knowledgeId.value, segmentKey(segment));
      message.success($t('docs.preview.segmentDeleted'));
      await Promise.all([loadSegments(), loadStatusDetail()]);
    },
  });
}

function confirmBatchDelete() {
  const ids = [...selectedIds.value];
  if (ids.length === 0) {
    message.warning($t('docs.preview.selectSegmentsFirst'));
    return;
  }
  dialog.warning({
    closable: false,
    content: $t('docs.preview.batchDeleteConfirm', { count: ids.length }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('docs.preview.batchDeleteTitle'),
    onPositiveClick: async () => {
      await deleteSegmentsApi(knowledgeId.value, { segmentIds: ids });
      message.success($t('docs.preview.segmentsDeleted', { count: ids.length }));
      await Promise.all([loadSegments(), loadStatusDetail()]);
    },
  });
}

async function submitCurrentIndex() {
  if (!knowledgeId.value || !docsId.value) {
    message.error($t('docs.messages.missingKnowledgeOrDocId'));
    return;
  }
  await indexKnowledgeApi(knowledgeId.value, {
    docsIds: [docsId.value],
  });
  message.success($t('docs.messages.vectorizeSubmittedCurrent'));
  await loadStatusDetail();
}
</script>

<template>
  <Page>
    <div class="flex h-full min-h-0 flex-col gap-3">
      <div
        class="flex flex-col gap-3 rounded-lg border border-border bg-card p-4 xl:flex-row xl:items-start xl:justify-between"
      >
        <div class="flex min-w-0 items-start gap-3">
          <NButton
            secondary
            @click="router.push(buildKnowledgeDocsRouteLocation(knowledgeId))"
          >
            <ArrowLeft class="size-4" />
            {{ $t('common.actions.back') }}
          </NButton>
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <FileText class="size-5 text-primary" />
              <div class="truncate text-lg font-semibold text-foreground">
                {{ selectedDoc?.name || $t('docs.title.preview') }}
              </div>
            </div>
            <div class="mt-2 flex flex-wrap items-center gap-2">
              <NTag :bordered="false" round size="small">
                <span class="text-muted-foreground">
                  {{ $t('docs.preview.tagCreateTime') }}
                </span>
                <span class="ml-1 font-medium tabular-nums text-foreground">
                  {{ formatDocsTimestamp(selectedDoc?.createTime) }}
                </span>
              </NTag>
              <NTag :bordered="false" round size="small">
                <span class="text-muted-foreground">
                  {{ $t('docs.preview.tagSegmentCount') }}
                </span>
                <span class="ml-1 font-medium tabular-nums text-foreground">
                  {{ segmentCount }}
                </span>
              </NTag>
              <NTag :bordered="false" round size="small">
                <span class="text-muted-foreground">
                  {{ $t('docs.preview.tagCharCount') }}
                </span>
                <span class="ml-1 font-medium tabular-nums text-foreground">
                  {{ formatDocsCharCount(charCount) }}
                </span>
              </NTag>
              <NTag :bordered="false" round size="small" type="info">
                <span class="text-muted-foreground">
                  {{ $t('docs.preview.tagParseMode') }}
                </span>
                <span class="ml-1 font-medium text-foreground">
                  {{ parseModeLabel }}
                </span>
              </NTag>
            </div>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <NButton secondary @click="loadSegments">
            <RefreshCcw class="size-4" />
            {{ $t('docs.actions.refreshPreview') }}
          </NButton>
          <NButton type="primary" @click="submitCurrentIndex">
            <RotateCcw class="size-4" />
            {{ $t('docs.actions.reVectorize') }}
          </NButton>
        </div>
      </div>

      <div
        class="grid min-h-0 flex-1 gap-3 xl:grid-cols-[minmax(0,1fr)_300px] xl:grid-rows-[minmax(0,1fr)]"
      >
        <section class="flex min-h-0 flex-col rounded-lg border border-border bg-card">
          <div
            class="flex flex-wrap items-center justify-between gap-3 border-b border-border px-4 py-3"
          >
            <div class="flex items-center gap-3">
              <NCheckbox
                :checked="allSelected"
                :disabled="segmentCount === 0"
                size="small"
                @update:checked="toggleSelectedAll"
              />
              <div class="text-sm font-semibold text-foreground">
                {{ $t('docs.preview.sections') }}
                <span class="ml-1 text-xs font-normal text-muted-foreground">
                  {{
                    $t('docs.preview.selectedCount', { count: selectedCount })
                  }}
                </span>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <NButton
                v-if="selectedCount > 0"
                ghost
                size="small"
                type="error"
                @click="confirmBatchDelete"
              >
                <Trash2 class="size-3.5" />
                {{ $t('docs.actions.batchDelete') }} ({{ selectedCount }})
              </NButton>
              <NButton bordered size="small" @click="toggleExpandedAll">
                <ChevronsUpDown class="size-4" />
                {{
                  allExpanded
                    ? $t('common.actions.collapseAll')
                    : $t('common.actions.expandAll')
                }}
              </NButton>
            </div>
          </div>

          <div class="min-h-0 flex-1 overflow-y-auto p-4">
            <div
              v-if="segmentsLoading"
              class="rounded-lg border border-dashed border-border px-6 py-12 text-center text-sm text-muted-foreground"
            >
              {{ $t('docs.preview.generating') }}
            </div>

            <div
              v-else-if="segmentCount === 0"
              class="flex min-h-full flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-border px-6 py-12 text-center"
            >
              <div class="text-sm font-semibold text-foreground">
                {{ $t('docs.preview.segmentEmpty') }}
              </div>
              <div class="max-w-md text-sm leading-6 text-muted-foreground">
                {{ $t('docs.preview.segmentEmptyHint') }}
              </div>
              <NButton type="primary" @click="submitCurrentIndex">
                <RotateCcw class="size-4" />
                {{ $t('docs.actions.reVectorize') }}
              </NButton>
            </div>

            <div v-else class="space-y-3">
              <div
                v-for="(segment, index) in segments"
                :key="segmentKey(segment)"
                class="cursor-pointer rounded-lg border bg-card p-3 transition-all hover:border-primary/50 hover:bg-primary/5"
                :class="[
                  isExpanded(segment)
                    ? 'border-primary/60 bg-primary/5'
                    : 'border-border',
                  isDisabled(segment) ? 'opacity-70' : '',
                ]"
                @click="toggleSegment(segment)"
              >
                <div class="flex items-start gap-3">
                  <NCheckbox
                    :checked="isSelected(segment)"
                    size="small"
                    @click.stop
                    @update:checked="(value) => toggleSelected(segment, value)"
                  />

                  <div class="min-w-0 flex-1">
                    <div class="flex flex-wrap items-center gap-2">
                      <NTag :bordered="false" round size="small" type="info">
                        {{ $t('docs.preview.segment', { index: index + 1 }) }}
                      </NTag>
                      <span class="text-xs text-muted-foreground tabular-nums">
                        {{
                          $t('docs.preview.charCount', {
                            count: segment.content?.length ?? 0,
                          })
                        }}
                      </span>
                      <NTag
                        v-if="isDisabled(segment)"
                        :bordered="false"
                        round
                        size="small"
                        type="warning"
                      >
                        {{ $t('docs.preview.disabled') }}
                      </NTag>
                    </div>

                    <div v-if="isEditing(segment)" class="mt-3 space-y-2" @click.stop>
                      <NInput
                        v-model:value="editingContent"
                        :autosize="{ minRows: 3, maxRows: 12 }"
                        type="textarea"
                      />
                      <div class="flex flex-wrap items-center gap-2">
                        <NButton
                          :loading="pendingSegmentId === segmentKey(segment)"
                          size="small"
                          type="primary"
                          @click="saveSegment(segment)"
                        >
                          {{ $t('common.actions.save') }}
                        </NButton>
                        <NButton
                          :loading="pendingSegmentId === segmentKey(segment)"
                          ghost
                          size="small"
                          type="primary"
                          @click="saveSegment(segment, true)"
                        >
                          {{ $t('docs.preview.saveAndReindex') }}
                        </NButton>
                        <NButton size="small" @click="cancelEdit">
                          {{ $t('common.actions.cancel') }}
                        </NButton>
                      </div>
                    </div>

                    <div
                      v-else
                      :class="[isExpanded(segment) ? '' : 'line-clamp-2']"
                      class="mt-2 whitespace-pre-wrap text-sm leading-7 text-muted-foreground"
                    >
                      {{ segment.content }}
                    </div>
                  </div>

                  <div
                    v-if="!isEditing(segment)"
                    class="flex shrink-0 items-center gap-1"
                    @click.stop
                  >
                    <NSwitch
                      :value="!isDisabled(segment)"
                      :loading="pendingSegmentId === segmentKey(segment)"
                      size="small"
                      @update:value="
                        (value) => handleToggleEnabled(segment, value)
                      "
                    />
                    <NButton
                      v-tippy="$t('docs.preview.editSegment')"
                      quaternary
                      size="small"
                      @click="startEdit(segment)"
                    >
                      <SquarePen class="size-3.5" />
                    </NButton>
                    <NButton
                      v-tippy="$t('docs.actions.reVectorize')"
                      quaternary
                      size="small"
                      type="primary"
                      @click="handleReindex(segment)"
                    >
                      <RotateCcw class="size-3.5" />
                    </NButton>
                    <NButton
                      v-tippy="$t('docs.preview.deleteSegment')"
                      quaternary
                      size="small"
                      type="error"
                      @click="confirmDeleteSegment(segment)"
                    >
                      <Trash2 class="size-3.5" />
                    </NButton>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <aside
          class="min-h-0 overflow-y-auto rounded-lg border border-border bg-card"
        >
          <div class="divide-y divide-border">
            <div class="p-4">
              <div
                class="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground"
              >
                {{ $t('docs.preview.metadata') }}
              </div>
              <div class="mt-3 space-y-3 text-xs leading-6">
                <div>
                  <div class="text-muted-foreground">
                    {{ $t('docs.search.knowledgeName') }}
                  </div>
                  <div class="mt-1">
                    <NTag :bordered="false" round size="small" type="info">
                      {{ knowledgeLabel }}
                    </NTag>
                  </div>
                </div>
                <div>
                  <div class="text-muted-foreground">
                    {{ $t('docs.search.docName') }}
                  </div>
                  <div class="mt-1 break-all font-medium text-foreground">
                    {{ selectedDoc?.name || '--' }}
                  </div>
                </div>
                <div class="flex flex-wrap items-center gap-2">
                  <NTag :bordered="false" round size="small">
                    {{
                      $t('docs.search.ext', { ext: selectedDoc?.ext || '--' })
                    }}
                  </NTag>
                  <NTag :bordered="false" round size="small">
                    {{
                      $t('docs.search.size', {
                        size: formatDocsFileSize(selectedDoc?.size),
                      })
                    }}
                  </NTag>
                </div>
                <div class="space-y-1 text-muted-foreground">
                  <div class="break-all">
                    {{ $t('docs.labels.docId') }}: {{ selectedDoc?.id || '--' }}
                  </div>
                  <div>
                    {{
                      $t('docs.search.updateTime', {
                        time: formatDocsTimestamp(selectedDoc?.updateTime),
                      })
                    }}
                  </div>
                </div>
              </div>
            </div>

            <div class="p-4">
              <div
                class="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground"
              >
                {{ $t('docs.preview.parserInfo') }}
              </div>
              <div class="mt-3 space-y-3 text-xs leading-6">
                <div>
                  <div class="text-muted-foreground">
                    {{ $t('docs.upload.parserMode') }}
                  </div>
                  <div class="mt-1">
                    <NTag :bordered="false" round size="small" type="info">
                      {{ parseModeLabel }}
                    </NTag>
                  </div>
                </div>
                <div class="flex items-center justify-between gap-3">
                  <span class="text-muted-foreground">
                    {{ $t('docs.upload.chunkSize') }}
                  </span>
                  <span class="font-medium tabular-nums text-foreground">
                    {{
                      segmentConfig.chunkSize ?? $t('docs.list.segmentDefault')
                    }}
                  </span>
                </div>
                <div class="flex items-center justify-between gap-3">
                  <span class="text-muted-foreground">
                    {{ $t('docs.upload.overlapSize') }}
                  </span>
                  <span class="font-medium tabular-nums text-foreground">
                    {{
                      segmentConfig.overlapSize ?? $t('docs.list.segmentDefault')
                    }}
                  </span>
                </div>
              </div>
            </div>

            <div class="p-4">
              <div
                class="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground"
              >
                {{ $t('docs.preview.vectorStatus') }}
              </div>
              <div class="mt-3 flex items-center gap-2">
                <NTag
                  :bordered="false"
                  :type="
                    resolveDocsStatusType(statusDetail?.embedStatus) as any
                  "
                  round
                  size="small"
                >
                  {{ resolveDocsStatusLabel(statusDetail?.embedStatus) }}
                </NTag>
              </div>
              <div
                class="mt-3 space-y-1.5 text-xs leading-6 text-muted-foreground"
              >
                <div class="flex items-center justify-between gap-3">
                  <span>{{ $t('docs.preview.tagSegmentCount') }}</span>
                  <span class="tabular-nums text-foreground">
                    {{ statusDetail?.segmentCount ?? segmentCount }}
                  </span>
                </div>
                <div class="flex items-center justify-between gap-3">
                  <span>{{ $t('docs.preview.tagCharCount') }}</span>
                  <span class="tabular-nums text-foreground">
                    {{ formatDocsCharCount(statusDetail?.charCount ?? charCount) }}
                  </span>
                </div>
                <div class="flex items-center justify-between gap-3">
                  <span>{{ $t('docs.preview.startTime') }}</span>
                  <span class="tabular-nums text-foreground">
                    {{ formatDocsTimestamp(statusDetail?.embedStartTime) }}
                  </span>
                </div>
                <div class="flex items-center justify-between gap-3">
                  <span>{{ $t('docs.preview.endTime') }}</span>
                  <span class="tabular-nums text-foreground">
                    {{ formatDocsTimestamp(statusDetail?.embedEndTime) }}
                  </span>
                </div>
                <div class="flex items-center justify-between gap-3">
                  <span>{{ $t('docs.preview.duration') }}</span>
                  <span class="tabular-nums text-foreground">
                    {{ formatDocsDuration(statusDetail?.costMs) }}
                  </span>
                </div>
              </div>
              <div
                v-if="statusDetail?.embedError"
                class="mt-3 rounded-xl border border-dashed border-border px-3 py-2 text-xs leading-6 text-destructive"
              >
                {{ statusDetail.embedError }}
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </Page>
</template>
