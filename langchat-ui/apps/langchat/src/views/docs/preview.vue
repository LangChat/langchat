<script lang="ts" setup>
import type {
  AigcDocs,
  KnowledgeDocumentIndexStatus,
  KnowledgeDocumentPreview,
} from '#/api/aigc/docs';

import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {ArrowLeft, ChevronsUpDown, FileText, RefreshCcw} from '@vben/icons';
import {$t} from '@vben/locales';

import {NButton, NTag} from 'naive-ui';

import {message} from '#/adapter/naive';
import {
  docsApi,
  getKnowledgeIndexStatusApi,
  indexKnowledgeApi,
  previewKnowledgeApi,
} from '#/api/aigc/docs';
import {
  formatDocsDuration,
  formatDocsTimestamp,
  resolveDocsStatusLabel,
  resolveDocsStatusType,
} from '#/views/shared/aigc/docs-status';
import {useAigcLookups} from '#/views/shared/aigc/lookups';

import {
  buildKnowledgeDocsRouteLocation,
  DOC_EMBED_STATUS,
  formatDocsFileSize,
  normalizeRouteParam,
} from './shared';

const route = useRoute();
const router = useRouter();
const { lookups, loadLookups } = useAigcLookups({
  knowledges: true,
});

const initialized = ref(false);
const docsOptions = ref<AigcDocs[]>([]);
const previewLoading = ref(false);
const previewItem = ref<KnowledgeDocumentPreview | null>(null);
const statusDetail = ref<KnowledgeDocumentIndexStatus | null>(null);
const selectedDoc = ref<AigcDocs | null>(null);
const expandedSections = ref<number[]>([]);

const knowledgeId = computed(() =>
  normalizeRouteParam(route.query.knowledgeId),
);
const docsId = computed(() => normalizeRouteParam(route.params.docsId));

const knowledgeLabel = computed(
  () =>
    lookups.value.knowledges.find((item) => item.value === knowledgeId.value)
      ?.label || $t('knowledge.card.unnamed'),
);

const visibleSectionCount = computed(
  () => previewItem.value?.sections?.length ?? 0,
);
const allExpanded = computed(
  () =>
    (previewItem.value?.sections?.length ?? 0) > 0 &&
    expandedSections.value.length ===
      (previewItem.value?.sections?.length ?? 0),
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

  await Promise.all([loadStatusDetail(), loadPreview()]);
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

async function loadPreview() {
  if (!knowledgeId.value || !docsId.value) {
    previewItem.value = null;
    return;
  }
  previewLoading.value = true;
  try {
    const result = await previewKnowledgeApi(knowledgeId.value, {
      chunkLimit: 200,
      docsIds: [docsId.value],
      sectionLimit: 200,
    });
    previewItem.value = result.docs[0] ?? null;
    expandedSections.value = [];
  } finally {
    previewLoading.value = false;
  }
}

function isExpanded(index: number) {
  return expandedSections.value.includes(index);
}

function toggleSection(index: number) {
  if (isExpanded(index)) {
    expandedSections.value = expandedSections.value.filter(
      (item) => item !== index,
    );
    return;
  }
  expandedSections.value = [...expandedSections.value, index];
}

function toggleAllSections() {
  if (allExpanded.value) {
    expandedSections.value = [];
    return;
  }
  expandedSections.value = (previewItem.value?.sections ?? []).map(
    (_item, index) => index,
  );
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
    <div class="rounded-lg border border-border bg-card p-4">
      <div class="flex flex-col gap-2">
        <div
          class="flex flex-col gap-2 border-b border-border pb-4 xl:flex-row xl:items-start xl:justify-between"
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
              <div class="mt-2 text-sm leading-6 text-muted-foreground">
                {{
                  $t('docs.preview.description', {
                    knowledge: knowledgeLabel,
                    total: visibleSectionCount,
                  })
                }}
              </div>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <NButton secondary @click="loadPreview">
              <RefreshCcw class="size-4" />
              {{ $t('docs.actions.refreshPreview') }}
            </NButton>
          </div>
        </div>

        <div class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_280px]">
          <div class="min-w-0">
            <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
              <div class="text-sm font-semibold text-foreground">
                {{ $t('docs.preview.sections') }}
              </div>
              <NButton bordered size="small" @click="toggleAllSections">
                <ChevronsUpDown class="size-4" />
                {{
                  allExpanded
                    ? $t('common.actions.collapseAll')
                    : $t('common.actions.expandAll')
                }}
              </NButton>
            </div>

            <div
              v-if="previewLoading"
              class="rounded-lg border border-dashed border-border px-6 py-12 text-center text-sm text-muted-foreground"
            >
              {{ $t('docs.preview.generating') }}
            </div>

            <div
              v-else-if="(previewItem?.sections?.length ?? 0) > 0"
              class="space-y-3"
            >
              <div
                v-for="(section, index) in previewItem?.sections || []"
                :key="`section-${index}`"
                class="rounded-lg border border-dashed border-border bg-muted/10 p-4"
              >
                <div class="flex flex-wrap items-center justify-between gap-3">
                  <div class="flex items-center gap-2">
                    <NTag :bordered="false" round size="small" type="info">
                      {{ $t('docs.preview.segment', { index: index + 1 }) }}
                    </NTag>
                    <span class="text-xs text-muted-foreground">
                      {{ $t('docs.preview.charCount', { count: section.length }) }}
                    </span>
                  </div>
                  <button
                    class="text-xs font-medium text-primary transition-opacity hover:opacity-80"
                    type="button"
                    @click="toggleSection(index)"
                  >
                    {{
                      isExpanded(index)
                        ? $t('common.actions.collapse')
                        : $t('common.actions.expand')
                    }}
                  </button>
                </div>

                <div
                  :class="[
                    isExpanded(index) ? '' : 'line-clamp-2',
                  ]" class="mt-3 whitespace-pre-wrap text-sm leading-7 text-muted-foreground"
                >
                  {{ section }}
                </div>
              </div>
            </div>

            <div
              v-else
              class="rounded-lg border border-dashed border-border px-6 py-12 text-center text-sm text-muted-foreground"
            >
              {{ $t('docs.preview.empty') }}
            </div>
          </div>

          <aside
            class="border-t border-border pt-4 xl:border-l xl:border-t-0 xl:pl-6 xl:pt-0"
          >
            <div class="space-y-5">
              <div>
                <div
                  class="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground"
                >
                  {{ $t('docs.preview.metadata') }}
                </div>
                <div
                  class="mt-3 space-y-2 text-xs leading-6 text-muted-foreground"
                >
                  <div>
                    {{ $t('docs.search.knowledgeName', { name: knowledgeLabel }) }}
                  </div>
                  <div>
                    {{
                      $t('docs.search.docName', {
                        name: selectedDoc?.name || '--',
                      })
                    }}
                  </div>
                  <div>
                    {{
                      $t('docs.search.docId', { id: selectedDoc?.id || '--' })
                    }}
                  </div>
                  <div>
                    {{
                      $t('docs.search.ext', { ext: selectedDoc?.ext || '--' })
                    }}
                  </div>
                  <div>
                    {{
                      $t('docs.search.size', {
                        size: formatDocsFileSize(selectedDoc?.size),
                      })
                    }}
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

              <div>
                <div
                  class="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground"
                >
                  {{ $t('docs.preview.parserInfo') }}
                </div>
                <div
                  class="mt-3 space-y-2 text-xs leading-6 text-muted-foreground"
                >
                  <div>
                    {{
                      $t('docs.preview.parserName', {
                        name: previewItem?.parserName || '--',
                      })
                    }}
                  </div>
                  <div>
                    {{
                      $t('docs.preview.sectionCount', {
                        count: previewItem?.sectionCount || 0,
                      })
                    }}
                  </div>
                  <div>
                    {{
                      $t('docs.preview.chunkCount', {
                        count: previewItem?.chunkCount || 0,
                      })
                    }}
                  </div>
                  <div>
                    {{
                      $t('docs.preview.contentLength', {
                        count: previewItem?.contentLength || 0,
                      })
                    }}
                  </div>
                </div>
              </div>

              <div>
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
                  class="mt-3 space-y-2 text-xs leading-6 text-muted-foreground"
                >
                  <div>
                    {{
                      $t('docs.preview.startTime', {
                        time: formatDocsTimestamp(statusDetail?.embedStartTime),
                      })
                    }}
                  </div>
                  <div>
                    {{
                      $t('docs.preview.endTime', {
                        time: formatDocsTimestamp(statusDetail?.embedEndTime),
                      })
                    }}
                  </div>
                  <div>
                    {{
                      $t('docs.preview.duration', {
                        time: formatDocsDuration(statusDetail?.costMs),
                      })
                    }}
                  </div>
                  <div>
                    {{
                      $t('docs.preview.indexStatus', {
                        status: statusDetail?.indexingStatus ?? '--',
                      })
                    }}
                  </div>
                </div>
                <div
                  v-if="statusDetail?.embedError"
                  class="mt-3 rounded-xl border border-dashed border-border px-3 py-2 text-xs leading-6 text-destructive"
                >
                  {{ statusDetail.embedError }}
                </div>
                <div class="mt-3">
                  <NButton
                    v-if="statusDetail?.embedStatus === DOC_EMBED_STATUS.FAILED"
                    quaternary
                    size="small"
                    type="error"
                    @click="submitCurrentIndex"
                  >
                    {{ $t('docs.actions.retryCurrent') }}
                  </NButton>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
  </Page>
</template>
