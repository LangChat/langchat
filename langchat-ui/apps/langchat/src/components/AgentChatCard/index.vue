<script lang="ts" setup>
import type {AgentChatEventEntry} from './types';

import {computed} from 'vue';

import {Bot} from '@vben/icons';

import {Sender} from 'ant-design-x-vue';

import {message} from '#/adapter/naive';

import ChatAssistantChainBubble from './chat-assistant-chain-bubble.vue';
import ChatMessageItem from './chat-message-item.vue';

interface ChatMessage {
  content: string;
  id: number | string;
  meta?: string;
  role: 'assistant' | 'user';
  time?: number;
}

interface UserBlock {
  content: string;
  id: string;
  timeText?: string;
  type: 'user';
}

interface AssistantChainBlock {
  id: string;
  items: AgentChatEventEntry[];
  type: 'assistant-chain';
}

type RenderBlock = AssistantChainBlock | UserBlock;

interface Props {
  appIcon?: string;
  autoSuggestionsEnabled?: boolean;
  defaultSuggestions?: string[];
  entries?: AgentChatEventEntry[];
  loading?: boolean;
  messageSuggestions?: Record<string, string[]>;
  messages?: ChatMessage[];
  placeholder?: string;
  title?: string;
  userIcon?: string;
  welcomeMessage?: string;
}

const props = withDefaults(defineProps<Props>(), {
  appIcon: '',
  autoSuggestionsEnabled: false,
  defaultSuggestions: () => [],
  entries: () => [],
  loading: false,
  messageSuggestions: () => ({}),
  messages: () => [],
  placeholder: '请输入消息',
  title: '聊天调试',
  userIcon: '',
  welcomeMessage: '欢迎使用当前应用，先从一个问题开始。',
});

const emit = defineEmits<{
  submit: [message: string];
}>();

function handleSubmit(content: string) {
  const text = String(content ?? '').trim();
  if (!text) {
    message.warning('请输入消息后再发送');
    return;
  }
  emit('submit', text);
}

function submitSuggestion(value: string) {
  const text = String(value || '').trim();
  if (!text) {
    return;
  }
  emit('submit', text);
}

function resolveSuggestions(messageId: number | string) {
  return props.messageSuggestions[String(messageId)] || [];
}

function normalizeLegacyMessages() {
  return props.messages.map((item) => ({
    content: item.content,
    id: String(item.id),
    meta: item.meta,
    role: item.role,
    time: item.time,
    type: 'message' as const,
  }));
}

function formatTime(time?: number) {
  if (!time) {
    return '';
  }
  return new Date(time).toLocaleTimeString('zh-CN', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function resolveEntries() {
  if (props.entries.length > 0) {
    return props.entries;
  }
  return normalizeLegacyMessages();
}

const resolvedEntries = computed(() => resolveEntries());

const renderBlocks = computed<RenderBlock[]>(() => {
  const blocks: RenderBlock[] = [];
  resolvedEntries.value.forEach((item) => {
    if (item.type === 'message' && item.role === 'user') {
      blocks.push({
        content: item.content,
        id: String(item.id),
        timeText: formatTime(item.time),
        type: 'user',
      });
      return;
    }
    const lastBlock = blocks[blocks.length - 1];
    if (lastBlock && lastBlock.type === 'assistant-chain') {
      lastBlock.items.push(item);
      return;
    }
    blocks.push({
      id: `assistant-chain-${item.id}`,
      items: [item],
      type: 'assistant-chain',
    });
  });
  return blocks;
});

const lastAssistantChainIndex = computed(() => {
  for (let index = renderBlocks.value.length - 1; index >= 0; index -= 1) {
    if (renderBlocks.value[index]?.type === 'assistant-chain') {
      return index;
    }
  }
  return -1;
});

function isAssistantMessage(item: AgentChatEventEntry) {
  return item.type === 'message' && item.role === 'assistant';
}

function resolveChainSuggestionMessageId(items: AgentChatEventEntry[]) {
  const target = [...items].reverse().find((item) => isAssistantMessage(item));
  return String(target?.id || '');
}

function isPendingAssistantChain(index: number) {
  return props.loading && index === lastAssistantChainIndex.value;
}
</script>

<template>
  <div
    class="flex h-full min-h-0 flex-col rounded-lg border border-border/70 bg-card p-3"
  >
    <div class="mb-2 text-sm font-semibold text-foreground">
      {{ props.title }}
    </div>

    <div class="flex min-h-0 flex-1 flex-col">
      <div class="min-h-0 flex-1 overflow-y-auto p-3">
        <div
          v-if="renderBlocks.length === 0"
          class="flex h-full min-h-full flex-col items-center justify-center px-4 text-center"
        >
          <div
            class="inline-flex size-14 items-center justify-center rounded-xl border border-border bg-background"
          >
            <img
              v-if="props.appIcon"
              :src="props.appIcon"
              alt="app icon"
              class="size-10 rounded-lg object-cover"
            />
            <Bot v-else class="size-7 text-primary" />
          </div>
          <div class="mt-4 text-sm leading-6 text-foreground">
            {{ props.welcomeMessage }}
          </div>

          <div
            v-if="props.defaultSuggestions.length > 0"
            class="mt-4 grid w-full max-w-lg gap-2 sm:grid-cols-2"
          >
            <button
              v-for="suggestion in props.defaultSuggestions"
              :key="`default-${suggestion}`"
              class="rounded-lg border border-border bg-background px-3 py-2 text-left text-xs text-muted-foreground transition-colors hover:border-primary/35 hover:bg-primary/5 hover:text-primary"
              type="button"
              @click="submitSuggestion(suggestion)"
            >
              {{ suggestion }}
            </button>
          </div>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="(block, index) in renderBlocks"
            :key="block.id"
            class="space-y-3"
          >
            <ChatMessageItem
              v-if="block.type === 'user'"
              :app-icon="props.appIcon"
              :content="block.content"
              :time-text="block.timeText"
              :user-icon="props.userIcon"
              role="user"
            />
            <ChatAssistantChainBubble
              v-else
              :app-icon="props.appIcon"
              :entries="block.items"
              :pending="isPendingAssistantChain(index)"
            />

            <div
              v-if="
                block.type === 'assistant-chain' &&
                props.autoSuggestionsEnabled &&
                resolveSuggestions(resolveChainSuggestionMessageId(block.items))
                  .length > 0
              "
              class="ml-1 grid max-w-[92%] gap-2 sm:grid-cols-2"
            >
              <button
                v-for="suggestion in resolveSuggestions(
                  resolveChainSuggestionMessageId(block.items),
                )"
                :key="`${resolveChainSuggestionMessageId(block.items)}-${suggestion}`"
                class="rounded-lg border border-border bg-background px-3 py-2 text-left text-xs text-muted-foreground transition-colors hover:border-primary/35 hover:bg-primary/5 hover:text-primary"
                type="button"
                @click="submitSuggestion(suggestion)"
              >
                {{ suggestion }}
              </button>
            </div>
          </div>

          <ChatAssistantChainBubble
            v-if="props.loading && lastAssistantChainIndex === -1"
            :app-icon="props.appIcon"
            :entries="[]"
            pending
          />
        </div>
      </div>

      <div class="mt-2 pt-2">
        <Sender
          :allow-speech="true"
          :disabled="props.loading"
          :loading="props.loading"
          :on-submit="handleSubmit"
          :placeholder="props.placeholder"
        />
      </div>
    </div>
  </div>
</template>
