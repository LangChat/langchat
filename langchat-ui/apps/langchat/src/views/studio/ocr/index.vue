<script lang="ts" setup>
import type {UploadFileInfo} from 'naive-ui';

import type {AigcModel} from '#/api/aigc/model';

import {onMounted, ref} from 'vue';

import {Page} from '@vben/common-ui';
import {Copy, FileSearch, ScanText} from '@vben/icons';

import {NButton, NForm, NFormItem, NInput, NSpin, NUpload, NUploadDragger,} from 'naive-ui';

import {ResizableHandle, ResizablePanel, ResizablePanelGroup,} from '@vben-core/shadcn-ui';

import {message} from '#/adapter/naive';
import {recognizeImage} from '#/api/aigc/image';
import {modelApi} from '#/api/aigc/model';
import ModelSelector from '#/components/ModelSelector/index.vue';

const loading = ref(false);
const recognizeText = ref('');
const previewUrl = ref('');
const models = ref<AigcModel[]>([]);
const fileList = ref<UploadFileInfo[]>([]);

const form = ref({
  modelId: '',
  prompt: '请识别图片中的文字内容。',
});

async function loadModels() {
  models.value = await modelApi.list();
}

function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.addEventListener('load', () => resolve(String(reader.result || '')));
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

async function handleFileChange(options: {file: UploadFileInfo}) {
  const file = options.file.file;
  if (!file) {
    previewUrl.value = '';
    return;
  }
  previewUrl.value = await readFileAsDataUrl(file);
}

function handleRemove() {
  fileList.value = [];
  previewUrl.value = '';
  recognizeText.value = '';
}

async function handleRecognize() {
  if (!form.value.modelId) {
    message.warning('请选择视觉模型');
    return;
  }
  if (!previewUrl.value) {
    message.warning('请先上传图片');
    return;
  }
  loading.value = true;
  recognizeText.value = '';
  try {
    const result = await recognizeImage({
      image: previewUrl.value,
      modelId: form.value.modelId,
      prompt: form.value.prompt,
    });
    recognizeText.value = result?.text || '';
    if (!recognizeText.value) {
      message.warning('未识别到文字内容');
    }
  } catch (error) {
    message.error(`识别失败：${(error as Error)?.message || '未知错误'}`);
  } finally {
    loading.value = false;
  }
}

async function handleCopy() {
  if (!recognizeText.value) {
    return;
  }
  try {
    await navigator.clipboard.writeText(recognizeText.value);
    message.success('已复制到剪贴板');
  } catch {
    message.error('复制失败');
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
          <div class="text-lg font-semibold text-foreground">图片识别</div>
          <div class="mt-1 text-sm text-muted-foreground">
            选择视觉模型，上传图片进行 OCR 文字识别与结构化抽取。
          </div>
        </div>
      </div>

      <ResizablePanelGroup class="min-h-0 flex-1" direction="horizontal">
        <!-- 左侧配置 -->
        <ResizablePanel :default-size="30" :min-size="20">
          <div
            class="flex h-full min-h-0 flex-col gap-3 overflow-y-auto rounded-xl border border-border bg-card p-4"
          >
          <NForm label-placement="top">
            <NFormItem label="视觉模型">
              <ModelSelector
                :allowed-types="['IMAGE2TEXT', 'OCR', 'VISION']"
                :config="{}"
                :model-entities="models"
                :model-id="form.modelId"
                @update:model-id="form.modelId = $event"
              />
            </NFormItem>

            <NFormItem label="上传图片">
              <NUpload
                v-model:file-list="fileList"
                :max="1"
                :on-remove="handleRemove"
                :show-file-list="false"
                accept="image/*"
                @change="handleFileChange"
              >
                <NUploadDragger>
                  <div class="flex flex-col items-center gap-2 py-6 text-muted-foreground">
                    <FileSearch class="size-10" />
                    <span class="text-sm">点击或拖拽图片到此处上传</span>
                    <span class="text-xs">支持 png / jpg / jpeg / webp</span>
                  </div>
                </NUploadDragger>
              </NUpload>
              <img
                v-if="previewUrl"
                :src="previewUrl"
                alt="预览"
                class="mt-2 max-h-56 w-full rounded-lg object-contain"
              />
            </NFormItem>

            <NFormItem label="识别提示">
              <NInput
                v-model:value="form.prompt"
                :autosize="{minRows: 2, maxRows: 5}"
                type="textarea"
              />
            </NFormItem>

            <NButton
              :loading="loading"
              block
              type="primary"
              @click="handleRecognize"
            >
              <template #icon>
                <ScanText class="size-4" />
              </template>
              开始识别
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
            <div class="text-sm font-semibold text-foreground">识别结果</div>
            <NButton
              v-if="recognizeText"
              size="small"
              @click="handleCopy"
            >
              <template #icon>
                <Copy class="size-3.5" />
              </template>
              复制文本
            </NButton>
          </div>

          <div class="flex flex-1 rounded-lg bg-muted/30 p-4">
            <NSpin :show="loading" class="w-full">
              <div
                v-if="recognizeText"
                class="whitespace-pre-wrap text-sm leading-relaxed text-foreground"
              >
                {{ recognizeText }}
              </div>
              <div
                v-else
                class="flex h-full flex-col items-center justify-center gap-2 text-muted-foreground"
              >
                <ScanText class="size-12" />
                <span class="text-sm">识别出的文字将展示在这里</span>
              </div>
            </NSpin>
          </div>
          </div>
        </ResizablePanel>
      </ResizablePanelGroup>
    </div>
  </Page>
</template>
