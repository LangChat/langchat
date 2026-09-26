<script setup lang="ts">
import type { ChatComposerSendPayload } from './types';

import { computed, nextTick, onMounted, ref, watch } from 'vue';

import {
  ArrowUp,
  AudioLines,
  Camera,
  Check,
  ChevronDown,
  ImagePlus,
  Mic,
  Paperclip,
  Plus,
  Square,
  X,
} from '@vben/icons';

import { VbenPopover } from '@vben-core/shadcn-ui';

import { getProviderIcon, getProviderLabel } from '#/views/models/model-meta';

import { formatFileSize, useAttachments } from './use-attachments';
import { useModelSelector } from './use-model-selector';
import { formatRecTime, useVoiceInput } from './use-voice-input';

interface Props {
  /** 是否禁用输入与发送 */
  disabled?: boolean;
  /** 输入框占位文案 */
  placeholder?: string;
  /** 是否显示添加附件入口 */
  showAttachment?: boolean;
  /** 是否显示模型选择 */
  showModel?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  placeholder: '有问题,尽管问',
  showAttachment: true,
  showModel: true,
});

const emit = defineEmits<{
  send: [payload: ChatComposerSendPayload];
}>();

const text = defineModel<string>('modelValue', { default: '' });
const modelId = defineModel<string>('modelId', { default: '' });

/** 文本域最大高度(px),超出后内部滚动 */
const TEXTAREA_MAX_HEIGHT = 160;

const textareaRef = ref<HTMLTextAreaElement | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);
const modelMenuOpen = ref(false);
const attachMenuOpen = ref(false);

const {
  items: attachments,
  addFiles,
  clear: clearAttachments,
  remove: removeAttachment,
} = useAttachments();

const {
  currentLabel,
  currentModel,
  loadModels,
  loading: modelsLoading,
  models,
} = useModelSelector(modelId);

const voiceBaseText = ref('');

const {
  cancel: cancelVoice,
  recording,
  seconds: recSeconds,
  start: startVoice,
  stop: stopVoice,
  supported: voiceSupported,
} = useVoiceInput({
  onTranscript: (value) => {
    text.value = value;
  },
});

const hasText = computed(() => text.value.length > 0);
/** 文本域实际换行(高度超过单行)后才切换为双行布局:输入区独占一行,操作按钮换到右下角 */
const isTall = ref(false);
/** 右下角按钮模式:无文字时是语音输入,有文字时变成发送 */
const actionMode = computed(() => (hasText.value ? 'send' : 'voice'));

function resizeTextarea() {
  const el = textareaRef.value;
  if (!el) {
    return;
  }
  el.style.height = 'auto';
  const height = Math.max(36, Math.min(el.scrollHeight, TEXTAREA_MAX_HEIGHT));
  el.style.height = `${height}px`;
  el.style.overflowY = el.scrollHeight > TEXTAREA_MAX_HEIGHT ? 'auto' : 'hidden';
  // 单行高度 36px(行高 22 + 上下内边距 12),超过 40px 说明已换行
  isTall.value = el.scrollHeight > 40;
}

function focusTextarea() {
  textareaRef.value?.focus();
}

watch(text, () => nextTick(resizeTextarea));

onMounted(() => {
  loadModels();
  resizeTextarea();
});

function triggerFilePicker(kind: 'camera' | 'files' | 'photos') {
  const input = fileInputRef.value;
  if (!input) {
    return;
  }
  input.value = '';
  if (kind === 'photos') {
    input.accept = 'image/*';
    input.removeAttribute('capture');
  } else if (kind === 'camera') {
    input.accept = 'image/*';
    input.setAttribute('capture', 'environment');
  } else {
    input.accept = '';
    input.removeAttribute('capture');
  }
  input.click();
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  addFiles(input.files);
  input.value = '';
}

function selectModel(id: string) {
  modelId.value = String(id);
  modelMenuOpen.value = false;
}

function startDictation() {
  if (props.disabled || !voiceSupported || recording.value) {
    return;
  }
  attachMenuOpen.value = false;
  modelMenuOpen.value = false;
  voiceBaseText.value = text.value;
  text.value = '';
  startVoice();
  nextTick(() => {
    resizeTextarea();
    focusTextarea();
  });
}

function stopDictation() {
  stopVoice();
  focusTextarea();
}

function cancelDictation() {
  cancelVoice();
  text.value = voiceBaseText.value;
  nextTick(() => {
    resizeTextarea();
    focusTextarea();
  });
}

function handleMicClick() {
  if (props.disabled) {
    return;
  }
  if (recording.value) {
    stopDictation();
  } else {
    startDictation();
  }
}

function handleActionClick() {
  if (props.disabled) {
    return;
  }
  if (actionMode.value === 'voice') {
    startDictation();
  }
}

function submit() {
  if (props.disabled || recording.value) {
    return;
  }
  const trimmed = text.value.trim();
  if (!trimmed) {
    return;
  }
  emit('send', {
    attachments: attachments.value,
    modelId: modelId.value,
    text: trimmed,
  });
  text.value = '';
  clearAttachments();
  nextTick(() => {
    resizeTextarea();
    focusTextarea();
  });
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    if (recording.value) {
      stopDictation();
      return;
    }
    submit();
  }
  if (event.key === 'Escape' && recording.value) {
    cancelDictation();
  }
}
</script>

<template>
  <form
    class="composer"
    :class="{ 'is-recording': recording }"
    novalidate
    @submit.prevent="submit"
  >
    <input
      ref="fileInputRef"
      accept=""
      class="hidden"
      multiple
      type="file"
      @change="handleFileChange"
    />

    <!-- 附件列表 -->
    <div v-show="attachments.length > 0" class="chips-row">
      <TransitionGroup name="chip">
        <div v-for="item in attachments" :key="item.id" class="chip">
          <span class="chip-thumb">
            <img v-if="item.url" :alt="item.name" :src="item.url" />
            <Paperclip v-else class="size-4" />
          </span>
          <span class="chip-meta">
            <span class="chip-name">{{ item.name }}</span>
            <span class="chip-size">{{ formatFileSize(item.size) }}</span>
          </span>
          <button
            aria-label="移除附件"
            class="chip-x"
            type="button"
            @click="removeAttachment(item.id)"
          >
            <X class="size-3" />
          </button>
        </div>
      </TransitionGroup>
    </div>

    <div class="deck" :class="{ 'is-tall': isTall }">
      <!-- 左侧:添加附件 / 录音时变为取消 -->
      <div class="plus-anchor">
        <button
          v-if="recording"
          aria-label="取消听写"
          class="icon-btn"
          type="button"
          @click="cancelDictation"
        >
          <X class="size-[18px]" />
        </button>
        <VbenPopover
          v-else-if="showAttachment"
          :content-props="{ align: 'start', side: 'top', sideOffset: 8 }"
          content-class="w-[216px] rounded-2xl border border-border bg-popover p-1.5 shadow-lg"
          :open="attachMenuOpen"
          @update:open="attachMenuOpen = $event"
        >
          <template #trigger>
            <button aria-label="添加附件" class="icon-btn" type="button">
              <Plus class="size-[18px]" />
            </button>
          </template>
          <div class="flex flex-col gap-0.5">
            <button
              class="flex w-full items-center gap-2.5 rounded-[10px] px-1.5 py-1.5 text-left text-sm text-foreground transition-colors hover:bg-accent"
              type="button"
              @click="triggerFilePicker('photos')"
            >
              <span
                class="flex size-7 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground"
              >
                <ImagePlus class="size-4" />
              </span>
              <span>添加图片</span>
              <span class="ml-auto text-[11px] text-muted-foreground opacity-80">Images</span>
            </button>
            <button
              class="flex w-full items-center gap-2.5 rounded-[10px] px-1.5 py-1.5 text-left text-sm text-foreground transition-colors hover:bg-accent"
              type="button"
              @click="triggerFilePicker('files')"
            >
              <span
                class="flex size-7 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground"
              >
                <Paperclip class="size-4" />
              </span>
              <span>添加文件</span>
              <span class="ml-auto text-[11px] text-muted-foreground opacity-80">Any</span>
            </button>
            <button
              class="flex w-full items-center gap-2.5 rounded-[10px] px-1.5 py-1.5 text-left text-sm text-foreground transition-colors hover:bg-accent"
              type="button"
              @click="triggerFilePicker('camera')"
            >
              <span
                class="flex size-7 shrink-0 items-center justify-center rounded-lg bg-muted text-muted-foreground"
              >
                <Camera class="size-4" />
              </span>
              <span>拍照</span>
              <span class="ml-auto text-[11px] text-muted-foreground opacity-80">Camera</span>
            </button>
          </div>
        </VbenPopover>
      </div>

      <!-- 输入区 -->
      <textarea
        ref="textareaRef"
        v-model="text"
        aria-label="输入消息"
        class="composer-textarea"
        :disabled="disabled"
        :placeholder="placeholder"
        :readonly="recording"
        rows="1"
        @keydown="handleKeydown"
      ></textarea>

      <!-- 右侧操作区 -->
      <div class="controls-anchor">
        <div class="controls">
          <span
            v-if="showModel"
            class="slot"
            :class="{ 'is-collapsed': recording }"
          >
            <VbenPopover
              :content-props="{ align: 'end', side: 'top', sideOffset: 8 }"
              content-class="w-56 rounded-2xl border border-border bg-popover p-1.5 shadow-lg"
              :open="modelMenuOpen"
              @update:open="modelMenuOpen = $event"
            >
              <template #trigger>
                <button aria-haspopup="menu" class="model-btn" type="button">
                  <img
                    v-if="currentModel?.provider"
                    alt=""
                    class="size-4 shrink-0 rounded-full"
                    :src="getProviderIcon(currentModel.provider)"
                  />
                  <Transition mode="out-in" name="label">
                    <span :key="currentLabel" class="whitespace-nowrap">
                      {{ currentLabel }}
                    </span>
                  </Transition>
                  <ChevronDown class="size-4 shrink-0" />
                </button>
              </template>
              <div
                v-if="models.length === 0"
                class="text-muted-foreground px-3 py-6 text-center text-xs"
              >
                {{ modelsLoading ? '模型加载中…' : '暂无可用对话模型' }}
              </div>
              <div v-else class="flex flex-col gap-0.5">
                <button
                  v-for="item in models"
                  :key="item.id"
                  :aria-checked="
                    String(item.id || '') === String(modelId || '')
                      ? 'true'
                      : 'false'
                  "
                  class="relative flex w-full items-center gap-2 rounded-lg px-2.5 py-1.5 text-left text-sm text-foreground transition-colors hover:bg-accent"
                  role="menuitemradio"
                  type="button"
                  @click="selectModel(String(item.id || ''))"
                >
                  <img
                    alt=""
                    class="size-4 shrink-0"
                    :src="getProviderIcon(item.provider)"
                  />
                  <span class="min-w-0 flex-1 truncate">
                    {{ item.name || item.model || '未命名模型' }}
                  </span>
                  <span class="text-[10px] text-muted-foreground">
                    {{ getProviderLabel(item.provider) }}
                  </span>
                  <Check
                    v-if="String(item.id || '') === String(modelId || '')"
                    class="text-primary size-4 shrink-0"
                  />
                </button>
              </div>
            </VbenPopover>
          </span>

          <span
            aria-hidden="true"
            class="slot rec-meta"
            :class="{ 'is-collapsed': !recording }"
          >
            <span class="mini-bars"><i></i><i></i><i></i><i></i></span>
            <span class="rec-timer">{{ formatRecTime(recSeconds) }}</span>
          </span>

          <button
            aria-label="语音输入"
            class="icon-btn mic-btn"
            :class="{ recording }"
            :disabled="disabled || !voiceSupported"
            :title="
              voiceSupported ? '语音输入' : '当前浏览器不支持语音识别'
            "
            type="button"
            @click="handleMicClick"
          >
            <Transition mode="out-in" name="icon">
              <Square v-if="recording" key="stop" class="size-4 fill-current" />
              <Mic v-else key="mic" class="size-[18px]" />
            </Transition>
          </button>

          <span class="slot" :class="{ 'is-collapsed': recording }">
            <button
              :aria-label="actionMode === 'send' ? '发送' : '语音输入'"
              class="action-btn"
              :data-mode="actionMode"
              :disabled="
                disabled || (!voiceSupported && actionMode === 'voice')
              "
              :type="actionMode === 'send' ? 'submit' : 'button'"
              @click="handleActionClick"
            >
              <Transition mode="out-in" name="icon">
                <AudioLines
                  v-if="actionMode === 'voice'"
                  key="voice"
                  class="size-[18px]"
                />
                <ArrowUp v-else key="send" class="size-[18px]" />
              </Transition>
            </button>
          </span>
        </div>
      </div>
    </div>
  </form>
</template>

<style scoped>
.composer {
  --composer-voice: hsl(var(--primary));
  --composer-ease: cubic-bezier(0.22, 1, 0.36, 1);

  position: relative;
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 6px;
  background: hsl(var(--background));
  border: 1px solid hsl(var(--border));
  border-radius: 24px;
  box-shadow:
    0 1px 3px hsl(var(--foreground) / 4%),
    0 4px 12px hsl(var(--foreground) / 5%);
  transition:
    background-color 0.2s ease-out,
    border-color 0.2s ease-out,
    box-shadow 0.2s ease-out;
}

/* 悬停:边框切换为 Primary 主题色,阴影略微加深 */
.composer:hover {
  border-color: hsl(var(--primary));
  box-shadow:
    0 1px 4px hsl(var(--foreground) / 5%),
    0 6px 18px hsl(var(--foreground) / 8%);
}

/* 聚焦:保持 Primary 边框 + 淡主色光环 */
.composer:focus-within {
  border-color: hsl(var(--primary));
  box-shadow:
    0 0 0 3px hsl(var(--primary) / 20%),
    0 4px 12px hsl(var(--foreground) / 6%);
}

.composer.is-recording {
  background: color-mix(
    in oklab,
    hsl(var(--muted)) 85%,
    var(--composer-voice)
  );
}

/* ---------------- 布局 ---------------- */

.deck {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr auto;
  column-gap: 4px;
  align-items: end;
}

.deck.is-tall {
  row-gap: 6px;
}

.plus-anchor {
  display: inline-flex;
  grid-row: 1;
  grid-column: 1;
}

.deck.is-tall .plus-anchor {
  grid-row: 2;
}

.deck.is-tall .composer-textarea {
  grid-column: 1 / -1;
  padding-right: 9px;
  padding-left: 9px;
}

.controls-anchor {
  display: inline-flex;
  grid-row: 1;
  grid-column: 3;
}

.deck.is-tall .controls-anchor {
  grid-row: 2;
}

.controls {
  display: flex;
  gap: 2px;
  align-items: center;
}

/* ---------------- 图标按钮 ---------------- */

.icon-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: hsl(var(--muted-foreground));
  border-radius: 9999px;
  transition:
    color 0.15s ease-out,
    background-color 0.15s ease-out,
    scale 0.15s ease-out;
}

.icon-btn:hover {
  color: hsl(var(--foreground));
  background: color-mix(in oklab, hsl(var(--foreground)) 10%, transparent);
}

.icon-btn:active {
  scale: 0.96;
}

.icon-btn:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

/* 录音中:麦克风变主色 + 呼吸圆环 */
.mic-btn.recording {
  color: hsl(var(--primary-foreground));
  background: var(--composer-voice);
}

.mic-btn.recording:hover {
  color: hsl(var(--primary-foreground));
  background: var(--composer-voice);
}

.mic-btn.recording::after {
  position: absolute;
  inset: 0;
  content: '';
  border-radius: 9999px;
  animation: rec-ring 1.6s ease-out infinite;
}

@keyframes rec-ring {
  from {
    box-shadow: 0 0 0 0
      color-mix(in oklab, var(--composer-voice) 45%, transparent);
  }

  to {
    box-shadow: 0 0 0 14px transparent;
  }
}

/* ---------------- 文本域 ---------------- */

.composer-textarea {
  grid-row: 1;
  grid-column: 2;
  min-height: 36px;
  max-height: 160px;
  padding: 6px 8px 6px 4px;
  overflow-y: hidden;
  font-size: 14px;
  line-height: 22px;
  color: hsl(var(--foreground));
  caret-color: hsl(var(--foreground));
  resize: none;
  outline: none;
  background: transparent;
  border: none;
}

.composer-textarea::placeholder {
  color: hsl(var(--muted-foreground));
}

/* ---------------- 折叠槽位 ---------------- */

.slot {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  max-width: 260px;
  overflow: hidden;
  transition:
    max-width 0.2s var(--composer-ease),
    opacity 0.16s ease-out,
    filter 0.16s ease-out;
}

.slot.is-collapsed {
  max-width: 0;
  opacity: 0;
  filter: blur(4px);
}

/* ---------------- 模型按钮 ---------------- */

.model-btn {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  height: 36px;
  padding: 0 8px 0 12px;
  font-size: 14px;
  color: hsl(var(--muted-foreground));
  border-radius: 9999px;
  transition:
    color 0.15s ease-out,
    background-color 0.15s ease-out;
}

.model-btn:hover {
  color: hsl(var(--foreground));
  background: color-mix(in oklab, hsl(var(--foreground)) 10%, transparent);
}

/* ---------------- 语音 / 发送按钮 ---------------- */

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  color: hsl(var(--primary-foreground));
  background: hsl(var(--primary));
  border-radius: 9999px;
  transition:
    background-color 0.2s ease-out,
    scale 0.15s ease-out;
}

.action-btn:hover {
  background: hsl(var(--primary) / 85%);
}

.action-btn:active {
  scale: 0.96;
}

.action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

/* ---------------- 录音计时 ---------------- */

.rec-meta {
  gap: 10px;
}

.mini-bars {
  display: flex;
  gap: 3px;
  align-items: center;
  height: 16px;
  margin-left: 10px;
}

.mini-bars i {
  width: 3px;
  height: 16px;
  background: var(--composer-voice);
  border-radius: 2px;
  transform-origin: center;
  animation: eq 760ms ease-in-out infinite alternate;
}

.mini-bars i:nth-child(2) {
  animation-duration: 620ms;
  animation-delay: -190ms;
}

.mini-bars i:nth-child(3) {
  animation-duration: 880ms;
  animation-delay: -380ms;
}

.mini-bars i:nth-child(4) {
  animation-duration: 700ms;
  animation-delay: -570ms;
}

@keyframes eq {
  from {
    transform: scaleY(0.18);
  }

  to {
    transform: scaleY(1);
  }
}

.rec-timer {
  margin-right: 8px;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  color: hsl(var(--muted-foreground));
  white-space: nowrap;
}

/* ---------------- 附件 chips ---------------- */

.chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 9px;
  margin-bottom: 6px;
}

.chip {
  display: flex;
  gap: 8px;
  align-items: center;
  height: 40px;
  padding: 4px 6px 4px 4px;
  outline: 1px solid hsl(var(--border));
  background: color-mix(in oklab, hsl(var(--foreground)) 8%, transparent);
  border-radius: 10px;
}

.chip-thumb {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  overflow: hidden;
  color: hsl(var(--muted-foreground));
  background: color-mix(in oklab, hsl(var(--foreground)) 10%, transparent);
  border-radius: 7px;
}

.chip-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.chip-meta {
  min-width: 0;
  max-width: 150px;
}

.chip-name {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
  line-height: 16px;
  color: hsl(var(--foreground));
  white-space: nowrap;
}

.chip-size {
  display: block;
  font-size: 11px;
  line-height: 14px;
  color: hsl(var(--muted-foreground));
}

.chip-x {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: hsl(var(--muted-foreground));
  border-radius: 9999px;
  transition:
    color 0.12s ease-out,
    background-color 0.12s ease-out;
}

.chip-x:hover {
  color: hsl(var(--foreground));
  background: color-mix(in oklab, hsl(var(--foreground)) 10%, transparent);
}

/* ---------------- 过渡动画 ---------------- */

.chip-enter-active {
  transition: all 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

.chip-enter-from {
  opacity: 0;
  filter: blur(4px);
  transform: scale(0.8);
}

.chip-leave-active {
  transition: all 0.18s ease-out;
}

.chip-leave-to {
  opacity: 0;
  filter: blur(4px);
  transform: scale(0.8);
}

.label-enter-active,
.label-leave-active,
.icon-enter-active,
.icon-leave-active {
  transition:
    opacity 0.18s ease-out,
    transform 0.18s var(--composer-ease),
    filter 0.18s ease-out;
}

.label-enter-from,
.icon-enter-from {
  opacity: 0;
  filter: blur(4px);
  transform: scale(0.5);
}

.label-leave-to,
.icon-leave-to {
  opacity: 0;
  filter: blur(4px);
  transform: scale(0.5);
}

@media (prefers-reduced-motion: reduce) {
  .composer *,
  .composer *::before,
  .composer *::after {
    transition: none !important;
    animation: none !important;
  }
}
</style>
