<script lang="ts" setup>
import type {SelectMixedOption} from 'naive-ui/es/select/src/interface';

import type {AigcDocs} from '#/api/aigc/docs';

import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {Upload} from '@vben/icons';

import {NButton, NInput, NInputNumber, NSelect, NStep, NSteps, NSwitch, NTag,} from 'naive-ui';

import {message} from '#/adapter/naive';
import {indexKnowledgeApi, uploadDocsApi} from '#/api/aigc/docs';
import {useAigcLookups} from '#/views/shared/aigc/lookups';
import {DOC_TYPE_OPTIONS} from '#/views/shared/aigc/options';

import {
  buildDocsIngestionConfig,
  buildKnowledgeDocsRouteLocation,
  buildKnowledgePreviewRouteLocation,
  buildKnowledgeUploadRouteLocation,
  DOC_PARSE_MODE_OPTIONS,
  formatDocsFileSize,
  normalizeRouteParam,
  removeDocsFileExtension,
  resolveDocsTypeByFilename,
} from './shared';

type QueueStatus = 'failed' | 'pending' | 'uploaded' | 'uploading';

interface UploadQueueItem {
  error?: string;
  file: File;
  name: string;
  sourceKey: string;
  status: QueueStatus;
  type: string;
  uid: string;
  uploadedDoc?: AigcDocs;
}

const route = useRoute();
const router = useRouter();
const { lookups, loadLookups } = useAigcLookups({
  knowledges: true,
});
const isCurrentUploadRoute = computed(
  () => route.name === 'KnowledgeUpload',
);
const routeKnowledgeId = computed(() =>
  normalizeRouteParam(route.query.knowledgeId),
);

const fileInputRef = ref<HTMLInputElement | null>(null);
const currentStep = ref(1);
const dragActive = ref(false);
const submitting = ref(false);
const queueItems = ref<UploadQueueItem[]>([]);

const uploadForm = ref({
  autoIndex: true,
  chunkSize: 800,
  enabled: true,
  knowledgeId: '',
  openPreviewAfterUpload: true,
  overlapSize: 120,
  parseMode: 'BUILTIN',
  type: '',
});

const parseModeOptions = computed<SelectMixedOption[]>(() =>
  DOC_PARSE_MODE_OPTIONS.map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

const currentParseModeDescription = computed(
  () =>
    DOC_PARSE_MODE_OPTIONS.find(
      (item) => item.value === uploadForm.value.parseMode,
    )?.description ?? '',
);

const typeOptions = computed<SelectMixedOption[]>(() => [
  { label: '自动识别', value: '' },
  ...DOC_TYPE_OPTIONS.map((item) => ({
    label: item.label,
    value: item.value,
  })),
]);

const knowledgeOptions = computed<SelectMixedOption[]>(
  () => lookups.value.knowledges as SelectMixedOption[],
);

const totalSize = computed(() =>
  queueItems.value.reduce((total, item) => total + item.file.size, 0),
);

const uploadedCount = computed(
  () => queueItems.value.filter((item) => item.status === 'uploaded').length,
);

const failedCount = computed(
  () => queueItems.value.filter((item) => item.status === 'failed').length,
);

const stepDescription = computed(() => {
  switch (currentStep.value) {
    case 1: {
      return '放置拖拽上传框，整理本次入库文档队列。';
    }
    case 2: {
      return '选择知识库，并设置分段器参数与向量化行为。';
    }
    default: {
      return '确认上传范围和参数，执行后台向量化任务。';
    }
  }
});

watch(
  routeKnowledgeId,
  (value) => {
    if (!isCurrentUploadRoute.value) {
      return;
    }
    if (!value) {
      void router.replace('/knowledges');
      return;
    }
    if (value !== uploadForm.value.knowledgeId) {
      uploadForm.value.knowledgeId = value;
    }
  },
  { immediate: true },
);

watch(
  () => lookups.value.knowledges,
  (knowledgeOptions) => {
    if (!uploadForm.value.knowledgeId && knowledgeOptions[0]) {
      uploadForm.value.knowledgeId = String(knowledgeOptions[0].value);
    }
  },
  { immediate: true },
);

watch(
  () => uploadForm.value.knowledgeId,
  (knowledgeId) => {
    if (!isCurrentUploadRoute.value) {
      return;
    }
    if (knowledgeId && knowledgeId !== routeKnowledgeId.value) {
      void router.replace(buildKnowledgeUploadRouteLocation(knowledgeId));
    }
  },
);

onMounted(loadLookups);

function openFileDialog() {
  fileInputRef.value?.click();
}

function handleNativeFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  addFiles([...input.files ?? []]);
  input.value = '';
}

function handleDrop(event: DragEvent) {
  event.preventDefault();
  dragActive.value = false;
  addFiles([...event.dataTransfer?.files ?? []]);
}

function addFiles(files: File[]) {
  const exists = new Set(queueItems.value.map((item) => item.sourceKey));
  const nextItems = files
    .filter((file) => file.size > 0)
    .map((file) => {
      const sourceKey = buildSourceKey(file);
      return {
        file,
        name: removeDocsFileExtension(file.name),
        sourceKey,
        status: 'pending' as QueueStatus,
        type: resolveDocsTypeByFilename(file.name),
        uid: buildUid(sourceKey),
      };
    })
    .filter((item) => !exists.has(item.sourceKey));

  if (nextItems.length === 0 && files.length > 0) {
    message.warning('待上传列表中已存在相同文件');
    return;
  }

  queueItems.value = [...queueItems.value, ...nextItems];
}

function removeQueueItem(uid: string) {
  queueItems.value = queueItems.value.filter((item) => item.uid !== uid);
}

function resetFailedItem(item: UploadQueueItem) {
  item.error = undefined;
  item.status = 'pending';
}

function validateCurrentStep(step = currentStep.value) {
  if (step >= 1 && queueItems.value.length === 0) {
    message.error('请先添加至少一个上传文件');
    return false;
  }
  if (step >= 2) {
    if (!uploadForm.value.knowledgeId) {
      message.error('请选择所属知识库');
      return false;
    }
    if (!uploadForm.value.chunkSize || uploadForm.value.chunkSize < 100) {
      message.error('切片大小至少为 100');
      return false;
    }
    if (uploadForm.value.overlapSize < 0) {
      message.error('重叠大小不能小于 0');
      return false;
    }
  }
  return true;
}

function goNextStep() {
  if (!validateCurrentStep()) {
    return;
  }
  currentStep.value = Math.min(3, currentStep.value + 1);
}

function goPrevStep() {
  currentStep.value = Math.max(1, currentStep.value - 1);
}

async function submitUploadQueue() {
  if (!validateCurrentStep(3)) {
    return;
  }

  submitting.value = true;
  const uploadedDocs: AigcDocs[] = [];
  try {
    for (const item of queueItems.value) {
      if (item.uploadedDoc?.id) {
        uploadedDocs.push(item.uploadedDoc);
        continue;
      }

      resetFailedItem(item);
      item.status = 'uploading';
      try {
        const formData = new FormData();
        formData.append('file', item.file);
        formData.append('knowledgeId', uploadForm.value.knowledgeId);
        formData.append(
          'name',
          item.name || removeDocsFileExtension(item.file.name),
        );
        formData.append('enabled', String(uploadForm.value.enabled));
        formData.append(
          'ingestionConfig',
          buildDocsIngestionConfig(
            uploadForm.value.chunkSize,
            uploadForm.value.overlapSize,
            uploadForm.value.parseMode,
          ),
        );
        if (uploadForm.value.type) {
          formData.append('type', uploadForm.value.type);
        }

        const uploaded = await uploadDocsApi(formData);
        item.status = 'uploaded';
        item.uploadedDoc = uploaded;
        uploadedDocs.push(uploaded);
      } catch (error) {
        item.status = 'failed';
        item.error = error instanceof Error ? error.message : '上传失败';
      }
    }

    if (uploadedDocs.length === 0) {
      message.error('没有文档上传成功，无法继续执行向量化');
      return;
    }

    if (uploadForm.value.autoIndex) {
      await indexKnowledgeApi(uploadForm.value.knowledgeId, {
        chunkSize: uploadForm.value.chunkSize,
        docsIds: uploadedDocs
          .map((item) => item.id)
          .filter(Boolean),
        overlapSize: uploadForm.value.overlapSize,
      });
    }

    const successText = uploadForm.value.autoIndex
      ? `已上传 ${uploadedDocs.length} 个文档，并提交后台向量化任务`
      : `已上传 ${uploadedDocs.length} 个文档`;

    if (failedCount.value > 0) {
      message.warning(
        `${successText}，另有 ${failedCount.value} 个文档上传失败`,
      );
      return;
    }

    message.success(successText);

    if (uploadForm.value.openPreviewAfterUpload && uploadedDocs[0]?.id) {
      void router.push(
        buildKnowledgePreviewRouteLocation({
          docsId: uploadedDocs[0].id,
          knowledgeId: uploadForm.value.knowledgeId,
        }),
      );
      return;
    }

    void router.push(
      buildKnowledgeDocsRouteLocation(uploadForm.value.knowledgeId),
    );
  } finally {
    submitting.value = false;
  }
}

function buildSourceKey(file: File) {
  return `${file.name}-${file.size}-${file.lastModified}`;
}

function buildUid(sourceKey: string) {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}-${sourceKey}`;
}
</script>

<template>
  <Page>
    <input
      ref="fileInputRef"
      class="hidden"
      multiple
      type="file"
      @change="handleNativeFileChange"
    />

    <div class="rounded-lg border border-border bg-card p-4">
      <div
        class="flex flex-col gap-2 xl:flex-row xl:items-start xl:justify-between"
      >
        <div class="min-w-0">
          <div class="text-lg font-semibold text-foreground">
            文档上传向量化
          </div>
          <div class="mt-1 max-w-3xl text-sm leading-6 text-muted-foreground">
            {{ stepDescription }}
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <NButton
            secondary
            @click="
              router.push(
                buildKnowledgeDocsRouteLocation(uploadForm.knowledgeId),
              )
            "
          >
            返回文档列表
          </NButton>
          <NButton secondary @click="openFileDialog">
            <Upload class="size-4" />
            继续添加文件
          </NButton>
        </div>
      </div>

      <div class="mt-4 border-t border-border pt-4">
        <NSteps :current="currentStep">
          <NStep description="拖拽上传框 + 文档列表" title="上传文件" />
          <NStep description="知识库、切片大小、重叠大小" title="分段设置" />
          <NStep description="确认上传并执行后台逻辑" title="执行向量化" />
        </NSteps>
      </div>

      <div
        v-if="currentStep === 1"
        class="mt-4 grid gap-4 xl:grid-cols-[minmax(0,1.25fr)_minmax(0,1fr)]"
      >
        <div class="flex min-w-0 flex-col gap-3">
          <div
            :class="[
              dragActive
                ? 'border-primary bg-primary/5'
                : 'border-border bg-muted/30 hover:border-primary/50',
            ]" class="flex min-h-[240px] cursor-pointer flex-col items-center justify-center rounded-lg border border-dashed px-6 text-center transition-colors"
            @click="openFileDialog"
            @drop="handleDrop"
            @dragenter.prevent="dragActive = true"
            @dragleave.prevent="dragActive = false"
            @dragover.prevent
          >
            <div class="text-lg font-semibold text-foreground">
              拖拽文件到这里，或点击选择本地文档
            </div>
            <div class="mt-3 max-w-lg text-sm leading-6 text-muted-foreground">
              支持 PDF、Office、Markdown、TXT
              等常见知识文档格式。当前上传接口按文件顺序依次入库，适合批量整理一个知识库的文档队列。
            </div>
            <div class="mt-5 flex flex-wrap items-center justify-center gap-2">
              <NTag :bordered="false" round type="info">
                已选 {{ queueItems.length }} 个文件
              </NTag>
              <NTag :bordered="false" round type="success">
                总大小 {{ formatDocsFileSize(totalSize) }}
              </NTag>
            </div>
          </div>

          <div
            class="rounded-lg border border-dashed border-border bg-muted/30 p-3 text-xs leading-5 text-muted-foreground"
          >
            <div class="mb-1 text-xs font-semibold text-foreground">
              上传说明
            </div>
            <div>1. 这里先整理本次要入库的文档列表，可反复拖拽追加。</div>
            <div>2. 每个文件默认使用文件名作为文档名称，可在列表里调整。</div>
            <div>3. 下一步统一选择知识库、切片大小和向量化策略。</div>
          </div>
        </div>

        <div class="min-w-0">
          <div class="text-sm font-semibold text-foreground">
            待上传文档列表
          </div>
          <div class="mt-1 text-xs text-muted-foreground">
            当前共 {{ queueItems.length }} 个文件，支持逐个修改文档名称。
          </div>

          <div v-if="queueItems.length > 0" class="mt-3 space-y-3">
            <div
              v-for="item in queueItems"
              :key="item.uid"
              class="rounded-lg border border-border bg-muted/30 p-3"
            >
              <div
                class="grid gap-3 md:grid-cols-[minmax(0,1fr)_92px_92px] md:items-center"
              >
                <NInput
                  v-model:value="item.name"
                  placeholder="请输入文档名称"
                />
                <div
                  class="flex h-8 items-center rounded-lg border border-border bg-card px-2 text-xs text-muted-foreground"
                >
                  {{ item.type }}
                </div>
                <div
                  class="flex h-8 items-center rounded-lg border border-border bg-card px-2 text-xs text-muted-foreground"
                >
                  {{ formatDocsFileSize(item.file.size) }}
                </div>
              </div>
              <div
                class="mt-2 flex items-center justify-between gap-2 whitespace-nowrap"
              >
                <div class="min-w-0 truncate text-xs text-muted-foreground">
                  {{ item.file.name }}
                </div>
                <div class="flex shrink-0 items-center gap-2">
                  <NTag
                    :bordered="false"
                    :type="
                      item.status === 'failed'
                        ? 'error'
                        : item.status === 'uploaded'
                          ? 'success'
                          : 'default'
                    "
                    round
                    size="small"
                  >
                    {{
                      item.status === 'uploaded'
                        ? '已上传'
                        : item.status === 'uploading'
                          ? '上传中'
                          : item.status === 'failed'
                            ? '失败'
                            : '待处理'
                    }}
                  </NTag>
                  <NButton
                    quaternary
                    size="tiny"
                    type="error"
                    @click="removeQueueItem(item.uid)"
                  >
                    移除
                  </NButton>
                </div>
              </div>
              <div v-if="item.error" class="mt-1 text-xs text-destructive">
                {{ item.error }}
              </div>
            </div>
          </div>

          <div
            v-else
            class="mt-3 rounded-lg border border-dashed border-border px-6 py-12 text-center text-sm text-muted-foreground"
          >
            还没有加入任何文件，先把文档拖进来。
          </div>
        </div>
      </div>

      <div
        v-if="currentStep === 2"
        class="mt-4 grid gap-4 xl:grid-cols-[minmax(0,1.2fr)_320px]"
      >
        <div class="grid gap-4 md:grid-cols-2">
            <div>
              <div class="mb-2 text-sm font-medium text-foreground">
                所属知识库
              </div>
              <NSelect
                v-model:value="uploadForm.knowledgeId"
                :options="knowledgeOptions"
                placeholder="请选择知识库"
              />
            </div>
            <div>
              <div class="mb-2 text-sm font-medium text-foreground">
                文档类型覆盖
              </div>
              <NSelect
                v-model:value="uploadForm.type"
                :options="typeOptions"
                placeholder="默认自动识别"
              />
            </div>
            <div>
              <div class="mb-2 text-sm font-medium text-foreground">
                解析模式
              </div>
              <NSelect
                v-model:value="uploadForm.parseMode"
                :options="parseModeOptions"
                placeholder="默认内置解析"
              />
              <div class="mt-1.5 text-xs text-muted-foreground">
                {{
                  currentParseModeDescription
                }}
              </div>
            </div>
            <div>
              <div class="mb-2 text-sm font-medium text-foreground">
                切片大小
              </div>
              <NInputNumber
                v-model:value="uploadForm.chunkSize"
                :min="100"
                :step="50"
                class="w-full"
              />
            </div>
            <div>
              <div class="mb-2 text-sm font-medium text-foreground">
                重叠大小
              </div>
              <NInputNumber
                v-model:value="uploadForm.overlapSize"
                :min="0"
                :step="20"
                class="w-full"
              />
            </div>
            <div>
              <div class="mb-2 text-sm font-medium text-foreground">
                上传后自动向量化
              </div>
              <div
                class="flex h-10 items-center rounded-xl border border-border bg-muted/20 px-3"
              >
                <NSwitch v-model:value="uploadForm.autoIndex" />
                <span class="ml-3 text-sm text-muted-foreground">
                  {{
                    uploadForm.autoIndex
                      ? '上传完成后立刻提交后台索引任务'
                      : '仅入库，不自动向量化'
                  }}
                </span>
              </div>
            </div>
            <div>
              <div class="mb-2 text-sm font-medium text-foreground">
                文档可用状态
              </div>
              <div
                class="flex h-10 items-center rounded-xl border border-border bg-muted/20 px-3"
              >
                <NSwitch v-model:value="uploadForm.enabled" />
                <span class="ml-3 text-sm text-muted-foreground">
                  {{
                    uploadForm.enabled
                      ? '新上传文档默认启用'
                      : '新上传文档先禁用'
                  }}
                </span>
              </div>
            </div>
          </div>

          <div
            class="rounded-lg border border-dashed border-border bg-muted/30 p-3 text-xs leading-5 text-muted-foreground"
          >
            <div class="mb-1.5 text-xs font-semibold text-foreground">
              分段器说明
            </div>
            <div class="space-y-1.5">
              <div>
                切片大小决定单个 chunk 的文本长度，值越大，召回上下文越完整。
              </div>
              <div>
                重叠大小用于维持上下文连续性，通常设置为切片大小的 10% 到 20%。
              </div>
              <div>
                解析模式决定正文抽取方式：内置解析开箱可用；Docling 需要后端单独部署
                docling-serve，对复杂 PDF 的版面与表格还原更好。
              </div>
              <div>
                这里的参数会写入每个文档的 ingestionConfig，并在后续预览页复用。
              </div>
            </div>
          </div>
        </div>

      <div
        v-if="currentStep === 3"
        class="mt-4 grid gap-4 xl:grid-cols-[minmax(0,1.2fr)_320px]"
      >
        <div class="min-w-0">
          <div class="grid gap-3 md:grid-cols-3">
            <div class="rounded-lg border border-border bg-muted/30 p-4">
              <div
                class="text-[11px] uppercase tracking-[0.16em] text-muted-foreground"
              >
                文档数量
              </div>
              <div class="mt-3 text-3xl font-semibold text-foreground">
                {{ queueItems.length }}
              </div>
            </div>
            <div class="rounded-lg border border-border bg-muted/30 p-4">
              <div
                class="text-[11px] uppercase tracking-[0.16em] text-muted-foreground"
              >
                总大小
              </div>
              <div class="mt-3 text-3xl font-semibold text-primary">
                {{ formatDocsFileSize(totalSize) }}
              </div>
            </div>
            <div class="rounded-lg border border-border bg-muted/30 p-4">
              <div
                class="text-[11px] uppercase tracking-[0.16em] text-muted-foreground"
              >
                目标知识库
              </div>
              <div class="mt-3 text-lg font-semibold text-foreground">
                {{
                  lookups.knowledges.find(
                    (item) => item.value === uploadForm.knowledgeId,
                  )?.label || '未选择'
                }}
              </div>
            </div>
          </div>

          <div class="mt-4 grid gap-4 lg:grid-cols-2">
            <div class="rounded-lg border border-border bg-muted/30 p-4">
              <div class="text-sm font-semibold text-foreground">分段配置</div>
              <div class="mt-3 space-y-2 text-sm text-muted-foreground">
                <div>切片大小：{{ uploadForm.chunkSize }}</div>
                <div>重叠大小：{{ uploadForm.overlapSize }}</div>
                <div>类型覆盖：{{ uploadForm.type || '自动识别' }}</div>
                <div>
                  解析模式：{{
                    DOC_PARSE_MODE_OPTIONS.find(
                      (item) => item.value === uploadForm.parseMode,
                    )?.label || '内置解析'
                  }}
                </div>
              </div>
            </div>
            <div class="rounded-lg border border-border bg-muted/30 p-4">
              <div class="text-sm font-semibold text-foreground">执行策略</div>
              <div class="mt-3 space-y-2 text-sm text-muted-foreground">
                <div>
                  上传后自动向量化：{{ uploadForm.autoIndex ? '是' : '否' }}
                </div>
                <div>文档默认启用：{{ uploadForm.enabled ? '是' : '否' }}</div>
                <div>
                  完成后跳转预览：{{
                    uploadForm.openPreviewAfterUpload ? '是' : '否'
                  }}
                </div>
              </div>
            </div>
          </div>

          <div class="mt-4 rounded-lg border border-border bg-muted/20 p-4">
            <div class="mb-3 flex items-center justify-between gap-3">
              <div class="text-sm font-semibold text-foreground">
                上传队列确认
              </div>
              <div class="flex items-center gap-2">
                <NTag :bordered="false" round type="success">
                  已上传 {{ uploadedCount }}
                </NTag>
                <NTag :bordered="false" round type="error">
                  失败 {{ failedCount }}
                </NTag>
              </div>
            </div>
            <div class="space-y-2">
              <div
                v-for="item in queueItems"
                :key="item.uid"
                class="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-card px-3 py-2"
              >
                <div class="min-w-0">
                  <div class="truncate text-sm font-medium text-foreground">
                    {{ item.name || removeDocsFileExtension(item.file.name) }}
                  </div>
                  <div class="mt-1 text-xs text-muted-foreground">
                    {{ item.file.name }} ·
                    {{ formatDocsFileSize(item.file.size) }}
                  </div>
                  <div v-if="item.error" class="mt-1 text-xs text-destructive">
                    {{ item.error }}
                  </div>
                </div>
                <NTag
                  :bordered="false"
                  :type="
                    item.status === 'uploaded'
                      ? 'success'
                      : item.status === 'uploading'
                        ? 'warning'
                        : item.status === 'failed'
                          ? 'error'
                          : 'default'
                  "
                  round
                >
                  {{
                    item.status === 'uploaded'
                      ? '已上传'
                      : item.status === 'uploading'
                        ? '上传中'
                        : item.status === 'failed'
                          ? '失败'
                          : '待处理'
                  }}
                </NTag>
              </div>
            </div>
          </div>
        </div>

          <div
            class="rounded-lg border border-dashed border-border bg-muted/30 p-3"
          >
            <div class="text-sm font-semibold text-foreground">完成后动作</div>
          <div
            class="mt-3 flex h-12 items-center rounded-lg border border-border bg-muted/20 px-3"
          >
            <NSwitch v-model:value="uploadForm.openPreviewAfterUpload" />
            <span class="ml-3 text-sm text-muted-foreground">
              上传成功后自动进入首个文档的分段预览页
            </span>
          </div>

          <div class="mt-4 space-y-3 text-sm leading-6 text-muted-foreground">
            <div>
              如果队列里有上传失败的文档，页面会停留在当前步骤，方便你修正后再次提交。
            </div>
            <div>
              后台向量化接口仍然是异步执行，提交成功后可在文档列表或状态详情中继续观察进度。
            </div>
          </div>
        </div>
      </div>

      <div
        class="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-border pt-3"
      >
        <div class="text-sm text-muted-foreground">
          第 {{ currentStep }} 步，共 3 步
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <NButton
            :disabled="currentStep === 1 || submitting"
            @click="goPrevStep"
          >
            上一步
          </NButton>
          <NButton
            v-if="currentStep < 3"
            :disabled="submitting"
            type="primary"
            @click="goNextStep"
          >
            下一步
          </NButton>
          <NButton
            v-else
            :loading="submitting"
            type="primary"
            @click="submitUploadQueue"
          >
            确认上传并执行向量化
          </NButton>
        </div>
      </div>
    </div>
  </Page>
</template>
