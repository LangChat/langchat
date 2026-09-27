<script lang="ts" setup>
import type {SelectMixedOption} from 'naive-ui/es/select/src/interface';

import type {AigcDocs} from '#/api/aigc/docs';

import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {Upload} from '@vben/icons';
import {$t} from '@vben/locales';

import {NButton, NInput, NInputNumber, NSelect, NStep, NSteps, NSwitch, NTag,} from 'naive-ui';

import {message} from '#/adapter/naive';
import {indexKnowledgeApi, uploadDocsApi} from '#/api/aigc/docs';
import {useAigcLookups} from '#/views/shared/aigc/lookups';
import {docTypeOptions} from '#/views/shared/aigc/options';

import {
  buildDocsIngestionConfig,
  buildKnowledgeDocsRouteLocation,
  buildKnowledgePreviewRouteLocation,
  buildKnowledgeUploadRouteLocation,
  docParseModeOptions,
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
  docParseModeOptions().map((item) => ({
    label: item.label,
    value: item.value,
  })),
);

const currentParseModeDescription = computed(
  () =>
    docParseModeOptions().find(
      (item) => item.value === uploadForm.value.parseMode,
    )?.description ?? '',
);

const typeOptions = computed<SelectMixedOption[]>(() => [
  { label: $t('docs.upload.typeAuto'), value: '' },
  ...docTypeOptions().map((item) => ({
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
      return $t('docs.upload.stepHint.upload');
    }
    case 2: {
      return $t('docs.upload.stepHint.segment');
    }
    default: {
      return $t('docs.upload.stepHint.execute');
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
    message.warning($t('docs.upload.duplicateFile'));
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
    message.error($t('docs.upload.noFiles'));
    return false;
  }
  if (step >= 2) {
    if (!uploadForm.value.knowledgeId) {
      message.error($t('docs.upload.selectKnowledge'));
      return false;
    }
    if (!uploadForm.value.chunkSize || uploadForm.value.chunkSize < 100) {
      message.error($t('docs.upload.chunkSizeTooSmall'));
      return false;
    }
    if (uploadForm.value.overlapSize < 0) {
      message.error($t('docs.upload.overlapInvalid'));
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
        item.error = error instanceof Error ? error.message : $t('docs.upload.uploadFailed');
      }
    }

    if (uploadedDocs.length === 0) {
      message.error($t('docs.upload.noSuccessVectorize'));
      return;
    }

    if (uploadForm.value.autoIndex) {
      await indexKnowledgeApi(uploadForm.value.knowledgeId, {
        chunkSize: uploadForm.value.chunkSize,
        docsIds: uploadedDocs
          .map((item) => item.id)
          .filter((id): id is string => Boolean(id)),
        overlapSize: uploadForm.value.overlapSize,
      });
    }

    const successText = uploadForm.value.autoIndex
      ? $t('docs.upload.uploadedWithVectorize', {
          count: uploadedDocs.length,
        })
      : $t('docs.upload.uploadedOnly', { count: uploadedDocs.length });

    if (failedCount.value > 0) {
      message.warning(
        $t('docs.upload.partialFailed', {
          success: successText,
          failed: failedCount.value,
        }),
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
            {{ $t('docs.title.upload') }}
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
            {{ $t('docs.actions.backToList') }}
          </NButton>
          <NButton secondary @click="openFileDialog">
            <Upload class="size-4" />
            {{ $t('docs.upload.addMore') }}
          </NButton>
        </div>
      </div>

      <div class="mt-4 border-t border-border pt-4">
        <NSteps :current="currentStep">
          <NStep
            :description="$t('docs.upload.stepUploadDescription')"
            :title="$t('docs.upload.stepUpload')"
          />
          <NStep
            :description="$t('docs.upload.stepSegmentDescription')"
            :title="$t('docs.upload.stepSegment')"
          />
          <NStep
            :description="$t('docs.upload.stepExecuteDescription')"
            :title="$t('docs.upload.stepExecute')"
          />
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
              {{ $t('docs.upload.dragHere') }}
            </div>
            <div class="mt-3 max-w-lg text-sm leading-6 text-muted-foreground">
              {{ $t('docs.upload.supportedFormats') }}
              {{ $t('docs.upload.supportedFormatsSuffix') }}
            </div>
            <div class="mt-5 flex flex-wrap items-center justify-center gap-2">
              <NTag :bordered="false" round type="info">
                {{ $t('docs.upload.selectedCount', { count: queueItems.length }) }}
              </NTag>
              <NTag :bordered="false" round type="success">
                {{
                  $t('docs.upload.totalSize', {
                    size: formatDocsFileSize(totalSize),
                  })
                }}
              </NTag>
            </div>
          </div>

          <div
            class="rounded-lg border border-dashed border-border bg-muted/30 p-3 text-xs leading-5 text-muted-foreground"
          >
            <div class="mb-1 text-xs font-semibold text-foreground">
              {{ $t('docs.upload.guideTitle') }}
            </div>
            <div>{{ $t('docs.upload.guideFirst') }}</div>
            <div>{{ $t('docs.upload.guideSecond') }}</div>
            <div>{{ $t('docs.upload.guideThird') }}</div>
          </div>
        </div>

        <div class="min-w-0">
          <div class="text-sm font-semibold text-foreground">
            {{ $t('docs.upload.queueTitle') }}
          </div>
          <div class="mt-1 text-xs text-muted-foreground">
            {{ $t('docs.upload.queueDescription', { count: queueItems.length }) }}
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
                  :placeholder="$t('docs.upload.namePlaceholder')"
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
                        ? $t('docs.upload.statusUploaded')
                        : item.status === 'uploading'
                          ? $t('docs.upload.statusUploading')
                          : item.status === 'failed'
                            ? $t('docs.upload.statusFailed')
                            : $t('docs.upload.statusPending')
                    }}
                  </NTag>
                  <NButton
                    quaternary
                    size="tiny"
                    type="error"
                    @click="removeQueueItem(item.uid)"
                  >
                    {{ $t('docs.upload.remove') }}
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
            {{ $t('docs.upload.queueEmpty') }}
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
                {{ $t('docs.upload.knowledge') }}
              </div>
              <NSelect
                v-model:value="uploadForm.knowledgeId"
                :options="knowledgeOptions"
                :placeholder="$t('docs.upload.knowledgePlaceholder')"
              />
            </div>
            <div>
              <div class="mb-2 text-sm font-medium text-foreground">
                {{ $t('docs.upload.typeOverride') }}
              </div>
              <NSelect
                v-model:value="uploadForm.type"
                :options="typeOptions"
                :placeholder="$t('docs.upload.typePlaceholder')"
              />
            </div>
            <div>
              <div class="mb-2 text-sm font-medium text-foreground">
                {{ $t('docs.upload.parserMode') }}
              </div>
              <NSelect
                v-model:value="uploadForm.parseMode"
                :options="parseModeOptions"
                :placeholder="$t('docs.upload.parserPlaceholder')"
              />
              <div class="mt-1.5 text-xs text-muted-foreground">
                {{
                  currentParseModeDescription
                }}
              </div>
            </div>
            <div>
              <div class="mb-2 text-sm font-medium text-foreground">
                {{ $t('docs.upload.chunkSize') }}
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
                {{ $t('docs.upload.overlapSize') }}
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
                {{ $t('docs.upload.autoVectorize') }}
              </div>
              <div
                class="flex h-10 items-center rounded-xl border border-border bg-muted/20 px-3"
              >
                <NSwitch v-model:value="uploadForm.autoIndex" />
                <span class="ml-3 text-sm text-muted-foreground">
                  {{
                    uploadForm.autoIndex
                      ? $t('docs.upload.autoVectorizeOn')
                      : $t('docs.upload.autoVectorizeOff')
                  }}
                </span>
              </div>
            </div>
            <div>
              <div class="mb-2 text-sm font-medium text-foreground">
                {{ $t('docs.upload.docEnabled') }}
              </div>
              <div
                class="flex h-10 items-center rounded-xl border border-border bg-muted/20 px-3"
              >
                <NSwitch v-model:value="uploadForm.enabled" />
                <span class="ml-3 text-sm text-muted-foreground">
                  {{
                    uploadForm.enabled
                      ? $t('docs.upload.docEnabledOn')
                      : $t('docs.upload.docEnabledOff')
                  }}
                </span>
              </div>
            </div>
          </div>

          <div
            class="rounded-lg border border-dashed border-border bg-muted/30 p-3 text-xs leading-5 text-muted-foreground"
          >
            <div class="mb-1.5 text-xs font-semibold text-foreground">
              {{ $t('docs.upload.segmenterGuide') }}
            </div>
            <div class="space-y-1.5">
              <div>
                {{ $t('docs.upload.chunkSizeHint') }}
              </div>
              <div>
                {{ $t('docs.upload.overlapSizeHint') }}
              </div>
              <div>
                {{ $t('docs.upload.parserHint') }} docling-serve{{
                  $t('docs.upload.parserHintSuffix')
                }}
              </div>
              <div>
                {{ $t('docs.upload.configPersistHint') }}
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
                {{ $t('docs.upload.summaryDocCount') }}
              </div>
              <div class="mt-3 text-3xl font-semibold text-foreground">
                {{ queueItems.length }}
              </div>
            </div>
            <div class="rounded-lg border border-border bg-muted/30 p-4">
              <div
                class="text-[11px] uppercase tracking-[0.16em] text-muted-foreground"
              >
                {{ $t('docs.upload.summaryTotalSize') }}
              </div>
              <div class="mt-3 text-3xl font-semibold text-primary">
                {{ formatDocsFileSize(totalSize) }}
              </div>
            </div>
            <div class="rounded-lg border border-border bg-muted/30 p-4">
              <div
                class="text-[11px] uppercase tracking-[0.16em] text-muted-foreground"
              >
                {{ $t('docs.upload.summaryTargetKnowledge') }}
              </div>
              <div class="mt-3 text-lg font-semibold text-foreground">
                {{
                  lookups.knowledges.find(
                    (item) => item.value === uploadForm.knowledgeId,
                  )?.label || $t('docs.upload.summaryNotSelected')
                }}
              </div>
            </div>
          </div>

          <div class="mt-4 grid gap-4 lg:grid-cols-2">
            <div class="rounded-lg border border-border bg-muted/30 p-4">
              <div class="text-sm font-semibold text-foreground">
                {{ $t('docs.upload.segmentConfig') }}
              </div>
              <div class="mt-3 space-y-2 text-sm text-muted-foreground">
                <div>
                  {{
                    $t('docs.upload.chunkSizeSummary', {
                      size: uploadForm.chunkSize,
                    })
                  }}
                </div>
                <div>
                  {{
                    $t('docs.upload.overlapSizeSummary', {
                      size: uploadForm.overlapSize,
                    })
                  }}
                </div>
                <div>
                  {{
                    $t('docs.upload.typeSummary', {
                      type: uploadForm.type || $t('docs.upload.typeSummaryDefault'),
                    })
                  }}
                </div>
                <div>
                  {{
                    $t('docs.upload.parserSummary', {
                      parser:
                        docParseModeOptions().find(
                          (item) => item.value === uploadForm.parseMode,
                        )?.label || $t('docs.upload.parserSummaryDefault'),
                    })
                  }}
                </div>
              </div>
            </div>
            <div class="rounded-lg border border-border bg-muted/30 p-4">
              <div class="text-sm font-semibold text-foreground">
                {{ $t('docs.upload.executeStrategy') }}
              </div>
              <div class="mt-3 space-y-2 text-sm text-muted-foreground">
                <div>
                  {{
                    $t('docs.upload.autoVectorizeSummary', {
                      value: uploadForm.autoIndex
                        ? $t('common.status.yes')
                        : $t('common.status.no'),
                    })
                  }}
                </div>
                <div>
                  {{
                    $t('docs.upload.docEnabledSummary', {
                      value: uploadForm.enabled
                        ? $t('common.status.yes')
                        : $t('common.status.no'),
                    })
                  }}
                </div>
                <div>
                  {{
                    $t('docs.upload.openPreviewSummary', {
                      value: uploadForm.openPreviewAfterUpload
                        ? $t('common.status.yes')
                        : $t('common.status.no'),
                    })
                  }}
                </div>
              </div>
            </div>
          </div>

          <div class="mt-4 rounded-lg border border-border bg-muted/20 p-4">
            <div class="mb-3 flex items-center justify-between gap-3">
              <div class="text-sm font-semibold text-foreground">
                {{ $t('docs.upload.queueConfirm') }}
              </div>
              <div class="flex items-center gap-2">
                <NTag :bordered="false" round type="success">
                  {{ $t('docs.upload.uploadedCount', { count: uploadedCount }) }}
                </NTag>
                <NTag :bordered="false" round type="error">
                  {{ $t('docs.upload.failedCount', { count: failedCount }) }}
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
                      ? $t('docs.upload.statusUploaded')
                      : item.status === 'uploading'
                        ? $t('docs.upload.statusUploading')
                        : item.status === 'failed'
                          ? $t('docs.upload.statusFailed')
                          : $t('docs.upload.statusPending')
                  }}
                </NTag>
              </div>
            </div>
          </div>
        </div>

          <div
            class="rounded-lg border border-dashed border-border bg-muted/30 p-3"
          >
            <div class="text-sm font-semibold text-foreground">
              {{ $t('docs.upload.afterUpload') }}
            </div>
          <div
            class="mt-3 flex h-12 items-center rounded-lg border border-border bg-muted/20 px-3"
          >
            <NSwitch v-model:value="uploadForm.openPreviewAfterUpload" />
            <span class="ml-3 text-sm text-muted-foreground">
              {{ $t('docs.upload.openPreviewHint') }}
            </span>
          </div>

          <div class="mt-4 space-y-3 text-sm leading-6 text-muted-foreground">
            <div>
              {{ $t('docs.upload.stayHint') }}
            </div>
            <div>
              {{ $t('docs.upload.asyncHint') }}
            </div>
          </div>
        </div>
      </div>

      <div
        class="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-border pt-3"
      >
        <div class="text-sm text-muted-foreground">
          {{
            $t('docs.upload.stepIndicator', { current: currentStep, total: 3 })
          }}
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <NButton
            :disabled="currentStep === 1 || submitting"
            @click="goPrevStep"
          >
            {{ $t('docs.upload.prev') }}
          </NButton>
          <NButton
            v-if="currentStep < 3"
            :disabled="submitting"
            type="primary"
            @click="goNextStep"
          >
            {{ $t('docs.upload.next') }}
          </NButton>
          <NButton
            v-else
            :loading="submitting"
            type="primary"
            @click="submitUploadQueue"
          >
            {{ $t('docs.upload.confirmUpload') }}
          </NButton>
        </div>
      </div>
    </div>
  </Page>
</template>
