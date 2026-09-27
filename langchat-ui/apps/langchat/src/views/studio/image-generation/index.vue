<script lang="ts" setup>
import type {AigcModel} from '#/api/aigc/model';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Download, ImagePlus, Sparkles } from '@vben/icons';
import { $t } from '@vben/locales';

import {NButton, NForm, NFormItem, NInput, NSelect, NSlider, NSpin,} from 'naive-ui';

import {ResizableHandle, ResizablePanel, ResizablePanelGroup,} from '@vben-core/shadcn-ui';

import {message} from '#/adapter/naive';
import {generateImage} from '#/api/aigc/image';
import {modelApi} from '#/api/aigc/model';
import ModelSelector from '#/components/ModelSelector/index.vue';

const loading = ref(false);
const generatedUrl = ref('');
const models = ref<AigcModel[]>([]);

const form = ref({
  modelId: '',
  n: 1,
  prompt: '',
  quality: 'standard',
  ratio: '1:1',
  size: '1024x1024',
});

const ratioOptions = [
  { label: '1 : 1', value: '1:1' },
  { label: '16 : 9', value: '16:9' },
  { label: '9 : 16', value: '9:16' },
  { label: '4 : 3', value: '4:3' },
  { label: '3 : 4', value: '3:4' },
];

const qualityOptions = computed(() => [
  { label: $t('studio.imageGeneration.qualityStandard'), value: 'standard' },
  { label: $t('studio.imageGeneration.qualityHigh'), value: 'high' },
]);

const sizeOptions = [
  { label: '1024 x 1024', value: '1024x1024' },
  { label: '1536 x 1024', value: '1536x1024' },
  { label: '1024 x 1536', value: '1024x1536' },
  { label: '1792 x 1024', value: '1792x1024' },
  { label: '1024 x 1792', value: '1024x1792' },
];

function fillSizeByRatio() {
  const ratioMap: Record<string, string> = {
    '1:1': '1024x1024',
    '16:9': '1536x1024',
    '9:16': '1024x1536',
    '4:3': '1536x1024',
    '3:4': '1024x1536',
  };
  form.value.size = ratioMap[form.value.ratio] || '1024x1024';
}

async function loadModels() {
  models.value = await modelApi.list();
}

/**
 * 统一返回内容：模型可能回传图片链接，也可能回传 base64 数据，
 * 这里按内容自动补全 data URL，无需用户选择返回格式。
 */
function buildImageUrl() {
  const value = generatedUrl.value;
  if (!value) {
    return '';
  }
  if (
    value.startsWith('data:') ||
    value.startsWith('http://') ||
    value.startsWith('https://')
  ) {
    return value;
  }
  return `data:image/png;base64,${value}`;
}

function downloadImage() {
  const url = buildImageUrl();
  if (!url || !generatedUrl.value) {
    message.warning($t('studio.imageGeneration.generateFirst'));
    return;
  }
  const link = document.createElement('a');
  link.href = url;
  link.download = `langchat-image-${Date.now()}.png`;
  link.click();
}

async function handleGenerate() {
  if (!form.value.modelId) {
    message.warning($t('studio.imageGeneration.selectModel'));
    return;
  }
  if (!form.value.prompt.trim()) {
    message.warning($t('studio.imageGeneration.promptRequired'));
    return;
  }
  loading.value = true;
  generatedUrl.value = '';
  try {
    const result = await generateImage({
      modelId: form.value.modelId,
      n: form.value.n,
      prompt: form.value.prompt,
      quality: form.value.quality,
      size: form.value.size,
    });
    const data = result?.url || result?.base64Data || '';
    if (!data) {
      message.warning($t('studio.imageGeneration.noImageData'));
      return;
    }
    generatedUrl.value = data;
  } catch (error) {
    message.error(
      $t('studio.imageGeneration.generateFailed', {
        message: (error as Error)?.message || $t('errors.unknown'),
      }),
    );
  } finally {
    loading.value = false;
  }
}

onMounted(loadModels);
</script>

<template>
  <Page>
    <div class="flex h-full min-h-full min-w-0 w-full flex-col gap-2">
      <div
        class="flex flex-col gap-3 rounded-xl border border-border bg-card p-4 lg:flex-row lg:items-center lg:justify-between"
      >
        <div>
          <div class="text-lg font-semibold text-foreground">
            {{ $t('studio.imageGeneration.title') }}
          </div>
          <div class="mt-1 text-sm text-muted-foreground">
            {{ $t('studio.imageGeneration.description') }}
          </div>
        </div>
      </div>

      <ResizablePanelGroup class="min-h-0 flex-1" direction="horizontal">
        <!-- 左侧配置 -->
        <ResizablePanel :default-size="30" :min-size="20">
          <div
            class="flex h-full min-h-0 flex-col gap-3 overflow-y-auto rounded-xl border border-border bg-card p-4"
          >
            <NForm :show-label="true" label-placement="top">
            <NFormItem :label="$t('studio.imageGeneration.model')">
              <ModelSelector
                :allowed-types="['TEXT2IMAGE']"
                :config="{}"
                :model-entities="models"
                :model-id="form.modelId"
                @update:model-id="form.modelId = $event"
              />
            </NFormItem>

            <NFormItem :label="$t('studio.imageGeneration.prompt')">
              <NInput
                v-model:value="form.prompt"
                :autosize="{minRows: 4, maxRows: 8}"
                :placeholder="$t('studio.imageGeneration.promptPlaceholder')"
                type="textarea"
              />
            </NFormItem>

            <NFormItem :label="$t('studio.imageGeneration.aspectRatio')">
              <NSelect
                v-model:value="form.ratio"
                :options="ratioOptions"
                @update:value="fillSizeByRatio"
              />
            </NFormItem>

            <NFormItem :label="$t('studio.imageGeneration.resolution')">
              <NSelect
                v-model:value="form.size"
                :options="sizeOptions"
                :placeholder="$t('studio.imageGeneration.resolutionPlaceholder')"
              />
            </NFormItem>

            <NFormItem :label="$t('studio.imageGeneration.quality')">
              <NSelect v-model:value="form.quality" :options="qualityOptions" />
            </NFormItem>

            <NFormItem :label="$t('studio.imageGeneration.count')">
              <NSlider
                v-model:value="form.n"
                :marks="{1: '1', 2: '2', 3: '3', 4: '4'}"
                :max="4"
                :min="1"
                :step="1"
              />
            </NFormItem>

            <NButton
              :loading="loading"
              block
              type="primary"
              @click="handleGenerate"
            >
              <template #icon>
                <Sparkles class="size-4" />
              </template>
              {{ $t('studio.imageGeneration.generate') }}
            </NButton>
          </NForm>
          </div>
        </ResizablePanel>

        <ResizableHandle
          class="mx-px transition-colors hover:bg-primary/60"
          with-handle
        />

        <!-- 右侧结果 -->
        <ResizablePanel :default-size="70" :min-size="40">
          <div
            class="flex h-full min-h-[420px] flex-col gap-3 rounded-xl border border-border bg-card p-4"
          >
          <div class="flex items-center justify-between">
            <div class="text-sm font-semibold text-foreground">
              {{ $t('studio.imageGeneration.resultTitle') }}
            </div>
            <NButton
              v-if="generatedUrl"
              size="small"
              type="primary"
              @click="downloadImage"
            >
              <template #icon>
                <Download class="size-3.5" />
              </template>
              {{ $t('studio.imageGeneration.download') }}
            </NButton>
          </div>

          <div class="flex flex-1 items-center justify-center rounded-lg bg-muted/30">
            <NSpin :show="loading">
              <img
                v-if="generatedUrl"
                :src="buildImageUrl()"
                :alt="$t('studio.imageGeneration.resultAlt')"
                class="max-h-[560px] w-full rounded-lg object-contain"
              />
              <div
                v-else
                class="flex flex-col items-center gap-2 p-12 text-muted-foreground"
              >
                <ImagePlus class="size-12" />
                <span class="text-sm">{{
                  $t('studio.imageGeneration.emptyResult')
                }}</span>
              </div>
            </NSpin>
          </div>
          </div>
        </ResizablePanel>
      </ResizablePanelGroup>
    </div>
  </Page>
</template>
