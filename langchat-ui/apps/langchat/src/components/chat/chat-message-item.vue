<script lang="ts" setup>
import type { ChatMessage } from './types';

import { computed, ref } from 'vue';

import {
  Check,
  Copy,
  RefreshCw,
  Sparkles,
  Square,
  User,
  Volume2,
} from '@vben/icons';

import MarkdownRender from 'markstream-vue';
import 'markstream-vue/index.css';

// 直接按文件路径引用本组件的页面不会经过 ../index.ts，样式需在此就地引入
import './ui/chat-effects.css';

interface Props {
  /** 助手头像图片地址 */
  assistantIcon?: string;
  /** 是否为最后一条助手消息(决定是否展示重新生成) */
  isLast?: boolean;
  message: ChatMessage;
  /** 用户头像图片地址 */
  userAvatar?: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  regenerate: [];
}>();

const isUser = computed(() => props.message.role === 'user');
const running = computed(() => props.message.status === 'running');
const failed = computed(() => props.message.status === 'error');
/** 内容为空且仍在生成:展示"正在思考"状态行 */
const showThinking = computed(() => running.value && !props.message.content);
/** 流式进行中:交由 markstream 做增量渲染,否则按最终态渲染 */
const streamDone = computed(() => props.message.status !== 'running');

const copied = ref(false);
const speaking = ref(false);

async function handleCopy() {
  try {
    await navigator.clipboard.writeText(props.message.content);
    copied.value = true;
    setTimeout(() => {
      copied.value = false;
    }, 1500);
  } catch {
    // 剪贴板不可用时静默降级
  }
}

/** 浏览器语音朗读(纯前端能力,不依赖后端) */
function handleSpeak() {
  if (!('speechSynthesis' in window)) {
    return;
  }
  if (speaking.value) {
    window.speechSynthesis.cancel();
    speaking.value = false;
    return;
  }
  const utterance = new SpeechSynthesisUtterance(props.message.content);
  utterance.lang = 'zh-CN';
  utterance.addEventListener('end', () => {
    speaking.value = false;
  });
  utterance.addEventListener('error', () => {
    speaking.value = false;
  });
  window.speechSynthesis.speak(utterance);
  speaking.value = true;
}
</script>

<template>
  <div
    class="group flex gap-2.5"
    :class="isUser ? 'flex-row-reverse' : 'flex-row'"
  >
    <!-- 头像:与消息首行文字对齐 -->
    <div class="shrink-0 self-start">
      <img
        v-if="isUser && userAvatar"
        alt=""
        class="size-10 rounded-full object-cover"
        :src="userAvatar"
      />
      <img
        v-else-if="!isUser && assistantIcon"
        alt=""
        class="size-10 rounded-full object-cover"
        :src="assistantIcon"
      />
      <span
        v-else-if="!isUser"
        class="flex size-10 items-center justify-center rounded-full bg-primary/10 text-primary"
      >
        <Sparkles class="size-5" />
      </span>
      <span
        v-else
        class="flex size-10 items-center justify-center rounded-full bg-muted text-muted-foreground"
      >
        <User class="size-5" />
      </span>
    </div>

    <!-- 内容列 -->
    <div
      class="flex min-w-0 max-w-[86%] flex-col gap-1"
      :class="isUser ? 'items-end' : 'items-start'"
    >
      <!-- 用户气泡 -->
      <div
        v-if="isUser"
        class="whitespace-pre-wrap break-words rounded-[14px] bg-primary px-3.5 py-2 text-sm leading-6 text-primary-foreground"
      >
        {{ message.content }}
      </div>

      <!-- 助手正文(浅灰卡片,Markdown 渲染) -->
      <div v-else class="w-full min-w-0 text-sm leading-7">
        <div
          class="w-fit max-w-full break-words rounded-[14px] px-3.5 py-1.5"
          :class="
            failed
              ? 'bg-destructive/10 text-destructive'
              : 'bg-muted/60 text-foreground'
          "
        >
          <span
            v-if="showThinking"
            class="inline-flex items-center gap-2 py-0.5"
          >
            <span class="chat-spinner"></span>
            <span class="chat-shimmer">正在思考…</span>
          </span>
          <template v-else>
            <div class="chat-markdown">
              <MarkdownRender
                :content="message.content"
                :final="streamDone"
                mode="chat"
              />
            </div>
            <span v-if="running" class="chat-cursor">▍</span>
          </template>
        </div>
        <slot name="append"></slot>
      </div>

      <!-- 悬停操作条:复制 / 语音朗读 / 重新生成 -->
      <div
        class="flex items-center gap-0.5 opacity-0 transition-opacity duration-150 group-hover:opacity-100"
        :class="isUser ? 'flex-row-reverse' : ''"
      >
        <button
          :aria-label="copied ? '已复制' : '复制'"
          class="flex size-6 cursor-pointer items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          type="button"
          @click="handleCopy"
        >
          <Check v-if="copied" class="size-3.5" />
          <Copy v-else class="size-3.5" />
        </button>
        <button
          v-if="!isUser && message.content"
          :aria-label="speaking ? '停止朗读' : '语音朗读'"
          class="flex size-6 cursor-pointer items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          type="button"
          @click="handleSpeak"
        >
          <Square v-if="speaking" class="size-3 fill-current" />
          <Volume2 v-else class="size-3.5" />
        </button>
        <button
          v-if="!isUser && isLast && !running"
          aria-label="重新生成"
          class="flex size-6 cursor-pointer items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          type="button"
          @click="emit('regenerate')"
        >
          <RefreshCw class="size-3.5" />
        </button>
      </div>
    </div>
  </div>
</template>
