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
      ?.label || '未命名知识库',
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
    message.warning('未找到当前文档，已返回文档列表');
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
    message.error('当前文档缺少必要参数');
    return;
  }
  await indexKnowledgeApi(knowledgeId.value, {
    docsIds: [docsId.value],
  });
  message.success('已提交当前文档向量化任务');
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
              返回
            </NButton>
            <div class="min-w-0">
              <div class="flex items-center gap-2">
                <FileText class="size-5 text-primary" />
                <div class="truncate text-lg font-semibold text-foreground">
                  {{ selectedDoc?.name || '分段预览' }}
                </div>
              </div>
              <div class="mt-2 text-sm leading-6 text-muted-foreground">
                当前知识库「{{ knowledgeLabel }}」下共展示
                {{
                  visibleSectionCount
                }}
                条分段，默认折叠到两行文本，可按需展开查看全文。
              </div>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <NButton secondary @click="loadPreview">
              <RefreshCcw class="size-4" />
              刷新预览
            </NButton>
          </div>
        </div>

        <div class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_280px]">
          <div class="min-w-0">
            <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
              <div class="text-sm font-semibold text-foreground">文本分段</div>
              <NButton bordered size="small" @click="toggleAllSections">
                <ChevronsUpDown class="size-4" />
                {{ allExpanded ? '收起全部' : '展开全部' }}
              </NButton>
            </div>

            <div
              v-if="previewLoading"
              class="rounded-lg border border-dashed border-border px-6 py-12 text-center text-sm text-muted-foreground"
            >
              正在生成分段预览...
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
                      分段 {{ index + 1 }}
                    </NTag>
                    <span class="text-xs text-muted-foreground">{{ section.length }} 字符</span>
                  </div>
                  <button
                    class="text-xs font-medium text-primary transition-opacity hover:opacity-80"
                    type="button"
                    @click="toggleSection(index)"
                  >
                    {{ isExpanded(index) ? '收起' : '展开' }}
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
              当前文档暂无可展示的分段内容。
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
                  文档元数据
                </div>
                <div
                  class="mt-3 space-y-2 text-xs leading-6 text-muted-foreground"
                >
                  <div>知识库：{{ knowledgeLabel }}</div>
                  <div>文档名称：{{ selectedDoc?.name || '--' }}</div>
                  <div>文档 ID：{{ selectedDoc?.id || '--' }}</div>
                  <div>文件后缀：{{ selectedDoc?.ext || '--' }}</div>
                  <div>
                    文件大小：{{ formatDocsFileSize(selectedDoc?.size) }}
                  </div>
                  <div>
                    更新时间：{{ formatDocsTimestamp(selectedDoc?.updateTime) }}
                  </div>
                </div>
              </div>

              <div>
                <div
                  class="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground"
                >
                  解析信息
                </div>
                <div
                  class="mt-3 space-y-2 text-xs leading-6 text-muted-foreground"
                >
                  <div>解析器：{{ previewItem?.parserName || '--' }}</div>
                  <div>分段数：{{ previewItem?.sectionCount || 0 }}</div>
                  <div>切片数：{{ previewItem?.chunkCount || 0 }}</div>
                  <div>内容长度：{{ previewItem?.contentLength || 0 }}</div>
                </div>
              </div>

              <div>
                <div
                  class="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground"
                >
                  向量化状态
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
                    开始时间：{{
                      formatDocsTimestamp(statusDetail?.embedStartTime)
                    }}
                  </div>
                  <div>
                    结束时间：{{
                      formatDocsTimestamp(statusDetail?.embedEndTime)
                    }}
                  </div>
                  <div>
                    耗时：{{ formatDocsDuration(statusDetail?.costMs) }}
                  </div>
                  <div>
                    索引标记：{{ statusDetail?.indexingStatus ?? '--' }}
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
                    重试当前文档
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
