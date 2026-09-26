<script lang="ts" setup>
import type { ChatMessage, ChatSendPayload, ChatSuggestion } from './types';

import { computed, ref } from 'vue';

import { ArrowDown } from '@vben/icons';

import ChatMessageItem from './chat-message-item.vue';
import ChatComposer from './composer/index.vue';
import { useChatScroll } from './use-chat-scroll';

// 直接按文件路径引用本组件的页面不会经过 ../index.ts，滚动渐隐样式需在此就地引入
import './ui/chat-scroll-fade.css';

interface Props {
  /** 助手头像图片地址 */
  assistantIcon?: string;
  /** 禁用输入与发送(例如请求进行中) */
  disabled?: boolean;
  /** 空状态标题 */
  emptyTitle?: string;
  /** 历史消息加载中(消息区展示 v-loading) */
  loading?: boolean;
  /** 加载中提示文字 */
  loadingText?: string;
  placeholder?: string;
  /** 是否显示添加附件入口 */
  showAttachment?: boolean;
  /** 是否显示模型选择 */
  showModel?: boolean;
  /** 空状态建议问题 */
  suggestions?: ChatSuggestion[];
  /** 用户头像图片地址 */
  userAvatar?: string;
}

withDefaults(defineProps<Props>(), {
  assistantIcon: '',
  disabled: false,
  emptyTitle: '随时可以开始',
  loading: false,
  loadingText: '正在加载消息...',
  placeholder: '输入你的问题',
  showAttachment: false,
  showModel: true,
  suggestions: () => [],
  userAvatar: '',
});

const emit = defineEmits<{
  regenerate: [];
  send: [payload: ChatSendPayload];
}>();

const messages = ref<ChatMessage[]>([]);
const composerText = ref('');
const scrollRef = ref<HTMLElement | null>(null);

/** 流式跟滚的依赖源:消息条数 + 全部文本 */
const scrollSource = computed(() =>
  messages.value.map((item) => item.content).join('') +
  messages.value.length,
);

const { nearBottom, scrollToBottom, updateNearBottom } = useChatScroll(
  () => scrollRef.value,
  scrollSource,
);

function handleSend(payload: ChatSendPayload) {
  emit('send', payload);
}

/** 建议问题点击后回填输入框(不直接发送) */
function handleSuggestion(item: ChatSuggestion) {
  composerText.value = item.value ?? item.label;
}

let seed = 0;

function nextId(prefix: string) {
  seed += 1;
  return `${prefix}-${Date.now()}-${seed}`;
}

function patch(id: string, updater: (message: ChatMessage) => void) {
  const item = messages.value.find((current) => current.id === id);
  if (item) {
    updater(item);
  }
}

/**
 * 开启一轮对话:追加用户消息与助手占位消息(OpenAI 协议风格),
 * 返回助手消息 ID,上层用该 ID 驱动 appendDelta / completeTurn / failTurn。
 */
function beginTurn(userText: string): string {
  messages.value.push({
    content: userText,
    id: nextId('user'),
    role: 'user',
    status: 'completed',
  });
  const assistantId = nextId('assistant');
  messages.value.push({
    content: '',
    id: assistantId,
    role: 'assistant',
    status: 'running',
  });
  return assistantId;
}

function appendDelta(id: string, chunk: string) {
  patch(id, (message) => {
    message.content += chunk;
  });
}

function setTurnMeta(id: string, meta: string) {
  patch(id, (message) => {
    message.meta = meta;
  });
}

function setTurnExtra(id: string, extra: Record<string, unknown>) {
  patch(id, (message) => {
    message.extra = { ...message.extra, ...extra };
  });
}

function completeTurn(id: string, finalContent?: string) {
  patch(id, (message) => {
    if (finalContent !== undefined) {
      message.content = finalContent;
    }
    message.status = 'completed';
  });
}

function failTurn(id: string, errorMessage: string) {
  patch(id, (message) => {
    message.content = errorMessage;
    message.status = 'error';
  });
}

function reset() {
  messages.value = [];
}

function setMessages(list: ChatMessage[]) {
  messages.value = [...list];
}

defineExpose({
  appendDelta,
  beginTurn,
  completeTurn,
  failTurn,
  messages,
  reset,
  scrollToBottom,
  setMessages,
  setTurnExtra,
  setTurnMeta,
});
</script>

<template>
  <div class="flex h-full min-h-0 flex-col">
    <!-- 消息区(会话列表 + 空状态):历史消息加载期间用 v-loading 覆盖 -->
    <div
      v-loading="{ spinning: loading, text: loadingText }"
      class="flex min-h-0 flex-1 flex-col"
    >
      <!-- 会话态:消息滚动区 -->
      <div
        v-if="messages.length > 0"
        ref="scrollRef"
        class="chat-scroll-fade min-h-0 flex-1 overflow-y-auto"
        @scroll="updateNearBottom"
      >
      <div
        class="mx-auto flex w-full max-w-3xl flex-col gap-5 px-4 pb-4 pt-4"
      >
        <ChatMessageItem
          v-for="(item, index) in messages"
          :key="item.id"
          :assistant-icon="assistantIcon"
          :is-last="
            item.role === 'assistant' && index === messages.length - 1
          "
          :message="item"
          :user-avatar="userAvatar"
          @regenerate="emit('regenerate')"
        >
          <template #append>
            <div
              v-if="item.meta"
              class="mt-1 text-[11px] text-muted-foreground"
            >
              {{ item.meta }}
            </div>
            <slot :message="item" name="message-append"></slot>
          </template>
        </ChatMessageItem>
      </div>

      <!-- 不贴底时显示"回到底部"悬浮按钮 -->
      <div
        v-if="!nearBottom"
        class="pointer-events-none sticky bottom-2 flex justify-center"
      >
        <button
          aria-label="回到底部"
          class="pointer-events-auto flex size-8 items-center justify-center rounded-full border border-border bg-background text-muted-foreground shadow-md transition-colors hover:text-foreground"
          type="button"
          @click="scrollToBottom()"
        >
          <ArrowDown class="size-4" />
        </button>
      </div>
    </div>

    <!-- 空状态:标题区(输入框上方) -->
    <div
      v-if="messages.length === 0"
      class="flex min-h-0 flex-1 flex-col items-center justify-end px-4 pb-6 text-center"
    >
      <slot name="empty-head">
        <div class="text-xl font-semibold text-foreground">
          {{ emptyTitle }}
        </div>
      </slot>
    </div>
    </div>

    <!-- 输入区:空状态时位于两组占位之间实现垂直居中,会话态时贴底 -->
    <div class="w-full px-4 pb-3">
      <div class="mx-auto w-full max-w-3xl">
        <ChatComposer
          v-model="composerText"
          :disabled="disabled"
          :placeholder="placeholder"
          :show-attachment="showAttachment"
          :show-model="showModel"
          @send="handleSend"
        />
      </div>
    </div>

    <!-- 空状态:建议列表(输入框下方) -->
    <div
      v-if="messages.length === 0 && suggestions.length > 0"
      class="flex min-h-0 flex-1 flex-col items-stretch justify-start px-4 pt-5"
    >
      <div class="mx-auto flex w-full max-w-3xl flex-col gap-1">
        <button
          v-for="item in suggestions"
          :key="item.label"
          class="flex items-center gap-3 rounded-lg px-3 py-2 text-left text-sm text-muted-foreground transition-colors hover:bg-muted/60 hover:text-foreground"
          type="button"
          @click="handleSuggestion(item)"
        >
          <component
            :is="item.icon"
            v-if="item.icon"
            class="size-4 shrink-0"
          />
          <span>{{ item.label }}</span>
        </button>
      </div>
    </div>
  </div>
</template>
