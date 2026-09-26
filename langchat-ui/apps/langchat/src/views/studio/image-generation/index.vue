<script lang="ts" setup>
import type {AigcModel} from '#/api/aigc/model';

import {onMounted, ref} from 'vue';

import {Page} from '@vben/common-ui';
import {Download, ImagePlus, Sparkles} from '@vben/icons';

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
  responseFormat: 'url',
  size: '1024x1024',
});

const ratioOptions = [
  { label: '1 : 1', value: '1:1' },
  { label: '16 : 9', value: '16:9' },
  { label: '9 : 16', value: '9:16' },
  { label: '4 : 3', value: '4:3' },
  { label: '3 : 4', value: '3:4' },
];

const qualityOptions = [
  { label: '标准', value: 'standard' },
  { label: '高清', value: 'high' },
];

const formatOptions = [
  { label: '图片链接 (url)', value: 'url' },
  { label: 'Base64 (b64_json)', value: 'b64_json' },
];

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

function buildImageUrl() {
  if (form.value.responseFormat === 'b64_json') {
    return `data:image/png;base64,${generatedUrl.value}`;
  }
  return generatedUrl.value;
}

function downloadImage() {
  const url = buildImageUrl();
  if (!url || !generatedUrl.value) {
    message.warning('请先生成图片');
    return;
  }
  const link = document.createElement('a');
  link.href = url;
  link.download = `langchat-image-${Date.now()}.png`;
  link.click();
}

async function handleGenerate() {
  if (!form.value.modelId) {
    message.warning('请选择模型');
    return;
  }
  if (!form.value.prompt.trim()) {
    message.warning('请输入提示词');
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
      responseFormat: form.value.responseFormat,
      size: form.value.size,
    });
    const data = result?.url || result?.base64Data || '';
    if (!data) {
      message.warning('未返回图片数据');
      return;
    }
    generatedUrl.value = data;
  } catch (error) {
    message.error(`生成失败：${(error as Error)?.message || '未知错误'}`);
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
          <div class="text-lg font-semibold text-foreground">图片生成</div>
          <div class="mt-1 text-sm text-muted-foreground">
            选择文生图模型，配置提示词与参数，一键生成图片。
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
            <NFormItem label="生成模型">
              <ModelSelector
                :allowed-types="['TEXT2IMAGE']"
                :config="{}"
                :model-entities="models"
                :model-id="form.modelId"
                @update:model-id="form.modelId = $event"
              />
            </NFormItem>

            <NFormItem label="提示词">
              <NInput
                v-model:value="form.prompt"
                :autosize="{minRows: 4, maxRows: 8}"
                placeholder="描述你想要生成的图片，例如：一只在星空下奔跑的白色狐狸，赛博朋克风格"
                type="textarea"
              />
            </NFormItem>

            <NFormItem label="宽高比例">
              <NSelect
                v-model:value="form.ratio"
                :options="ratioOptions"
                @update:value="fillSizeByRatio"
              />
            </NFormItem>

            <NFormItem label="像素分辨率">
              <NSelect
                v-model:value="form.size"
                :options="sizeOptions"
                placeholder="选择图片分辨率"
              />
            </NFormItem>

            <NFormItem label="图片质量">
              <NSelect v-model:value="form.quality" :options="qualityOptions" />
            </NFormItem>

            <NFormItem label="返回格式">
              <NSelect
                v-model:value="form.responseFormat"
                :options="formatOptions"
              />
            </NFormItem>

            <NFormItem label="生成数量">
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
              生成图片
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
            <div class="text-sm font-semibold text-foreground">生成结果</div>
            <NButton
              v-if="generatedUrl"
              size="small"
              type="primary"
              @click="downloadImage"
            >
              <template #icon>
                <Download class="size-3.5" />
              </template>
              下载图片
            </NButton>
          </div>

          <div class="flex flex-1 items-center justify-center rounded-lg bg-muted/30">
            <NSpin :show="loading">
              <img
                v-if="generatedUrl"
                :src="buildImageUrl()"
                alt="生成结果"
                class="max-h-[560px] w-full rounded-lg object-contain"
              />
              <div
                v-else
                class="flex flex-col items-center gap-2 p-12 text-muted-foreground"
              >
                <ImagePlus class="size-12" />
                <span class="text-sm">生成的图片将展示在这里</span>
              </div>
            </NSpin>
          </div>
          </div>
        </ResizablePanel>
      </ResizablePanelGroup>
    </div>
  </Page>
</template>
