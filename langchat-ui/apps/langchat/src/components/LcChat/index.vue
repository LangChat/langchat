<script lang="ts" setup>
import type {
  LcChatMessage,
  LcChatSendPayload,
  LcChatSuggestion,
} from './types';

import { computed, ref } from 'vue';

import { ArrowDown } from '@vben/icons';

import LcChatMessageItem from './chat-message-item.vue';
import LcChatComposer from './composer/index.vue';
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
  /** 组件重建时用于恢复消息的初始快照 */
  initialMessages?: LcChatMessage[];
  placeholder?: string;
  /** 是否显示添加附件入口 */
  showAttachment?: boolean;
  /** 是否显示模型选择 */
  showModel?: boolean;
  /** 空状态建议问题 */
  suggestions?: LcChatSuggestion[];
  /** 用户头像图片地址 */
  userAvatar?: string;
}

const props = withDefaults(defineProps<Props>(), {
  assistantIcon: '',
  disabled: false,
  emptyTitle: '随时可以开始',
  initialMessages: () => [],
  loading: false,
  loadingText: '正在加载消息...',
  placeholder: '输入你的问题',
  showAttachment: false,
  showModel: true,
  suggestions: () => [],
  userAvatar: '',
});

const emit = defineEmits<{
  messagesChange: [messages: LcChatMessage[]];
  regenerate: [];
  send: [payload: LcChatSendPayload];
}>();

const messages = ref<LcChatMessage[]>(cloneMessages(props.initialMessages));
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

function handleSend(payload: LcChatSendPayload) {
  emit('send', payload);
}

/** 建议问题点击后回填输入框(不直接发送) */
function handleSuggestion(item: LcChatSuggestion) {
  composerText.value = item.value ?? item.label;
}

let seed = 0;

function nextId(prefix: string) {
  seed += 1;
  return `${prefix}-${Date.now()}-${seed}`;
}

function cloneMessages(list: LcChatMessage[]) {
  return list.map((item) => ({
    ...item,
    extra: item.extra ? { ...item.extra } : undefined,
  }));
}

function emitMessagesChange() {
  emit('messagesChange', cloneMessages(messages.value));
}

function patch(id: string, updater: (message: LcChatMessage) => void) {
  const item = messages.value.find((current) => current.id === id);
  if (item) {
    updater(item);
    emitMessagesChange();
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
  emitMessagesChange();
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
  emitMessagesChange();
}

function getMessages() {
  return cloneMessages(messages.value);
}

function setMessages(list: LcChatMessage[]) {
  messages.value = cloneMessages(list);
  emitMessagesChange();
}

defineExpose({
  appendDelta,
  beginTurn,
  completeTurn,
  failTurn,
  getMessages,
  messages,
  reset,
  scrollToBottom,
  setMessages,
  setTurnExtra,
  setTurnMeta,
});
</script>

<template>
  <div class="chat-layout flex h-full min-h-0 flex-col">
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
        <div class="chat-layout-content flex flex-col gap-4 pb-4 pt-4">
          <LcChatMessageItem
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
              <slot :item="item" :message="item" name="message-append"></slot>
            </template>
          </LcChatMessageItem>
        </div>

        <!-- 不贴底时显示"回到底部"悬浮按钮 -->
        <div
          v-if="!nearBottom"
          class="pointer-events-none sticky bottom-2 flex justify-center"
        >
          <button
            aria-label="回到底部"
            class="pointer-events-auto flex size-8 items-center justify-center rounded-full border border-border bg-background text-muted-foreground transition-colors hover:text-foreground"
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
        class="flex min-h-0 flex-1 flex-col items-center justify-end pb-6 text-center"
      >
        <div class="chat-layout-content">
          <slot name="empty-head">
            <div class="text-xl font-semibold text-foreground">
              {{ emptyTitle }}
            </div>
          </slot>
        </div>
      </div>
    </div>

    <!-- 输入区:空状态时位于两组占位之间实现垂直居中,会话态时贴底 -->
    <div class="w-full pb-3">
      <div class="chat-layout-content">
        <LcChatComposer
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
      class="flex min-h-0 flex-1 flex-col items-stretch justify-start pt-5"
    >
      <div class="chat-layout-content flex flex-col gap-2">
        <button
          v-for="(item, index) in suggestions"
          :key="item.label"
          class="group flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left text-sm text-muted-foreground transition-colors hover:bg-muted/60 hover:text-foreground active:bg-muted"
          type="button"
          @click="handleSuggestion(item)"
        >
          <span class="flex size-5 shrink-0 items-center justify-center text-base">
            <component
              :is="item.icon"
              v-if="item.icon"
              class="size-4 text-muted-foreground transition-colors group-hover:text-primary"
            />
            <span v-else aria-hidden="true">
              {{ ['✨', '💡', '🚀'][index % 3] }}
            </span>
          </span>
          <span class="min-w-0 flex-1 leading-5">{{ item.label }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-layout {
  --chat-layout-gutter: clamp(1rem, 3vw, 2.5rem);
  --chat-layout-content-width: 60rem;

  container-type: inline-size;
}

@supports (width: 1cqi) {
  .chat-layout {
    --chat-layout-gutter: clamp(1rem, 3.5cqi, 2.5rem);
  }
}

.chat-layout-content {
  box-sizing: border-box;
  width: min(100%, var(--chat-layout-content-width));
  padding-inline: var(--chat-layout-gutter);
  margin-inline: auto;
}
</style>
