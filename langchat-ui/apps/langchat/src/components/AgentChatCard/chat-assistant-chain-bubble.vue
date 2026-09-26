<script setup lang="ts">
import type { AgentChatEventEntry } from './types';

import { computed, onBeforeUnmount, ref } from 'vue';
import { Bot, LoaderCircle } from '@vben/icons';
import { Bubble } from 'ant-design-x-vue';

import { message } from '#/adapter/naive';
import ChatMessageActions from './chat-message-actions.vue';
import ChatRagEventCard from './chat-rag-event-card.vue';
import Shimmer from './Shimmer.vue';
import ChatToolEventCard from './chat-tool-event-card.vue';

interface Props {
  appIcon?: string;
  entries: AgentChatEventEntry[];
  pending?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  appIcon: '',
  pending: false,
});

const speaking = ref(false);

const assistantText = computed(() =>
  props.entries
    .filter(
      (item): item is Extract<AgentChatEventEntry, { type: 'message' }> =>
        item.type === 'message' && item.role === 'assistant',
    )
    .map((item) => item.content || '')
    .join('\n')
    .trim(),
);

const assistantTimeText = computed(() => {
  const target = [...props.entries]
    .reverse()
    .find(
      (item): item is Extract<AgentChatEventEntry, { type: 'message' }> =>
        item.type === 'message' && item.role === 'assistant',
    );
  if (!target?.time) {
    return '--';
  }
  return new Date(target.time).toLocaleTimeString('zh-CN', {
    hour12: false,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
});

function copyMessage() {
  const text = assistantText.value;
  if (!text) {
    return;
  }
  navigator.clipboard
    .writeText(text)
    .then(() => {
      message.success('消息已复制');
    })
    .catch(() => {
      message.error('复制失败');
    });
}

function toggleSpeech() {
  const text = assistantText.value;
  if (!text || typeof window === 'undefined') {
    return;
  }
  if (!('speechSynthesis' in window)) {
    message.warning('当前浏览器不支持语音朗读');
    return;
  }
  if (speaking.value) {
    window.speechSynthesis.cancel();
    speaking.value = false;
    return;
  }
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'zh-CN';
  utterance.rate = 1;
  utterance.onend = () => {
    speaking.value = false;
  };
  utterance.onerror = () => {
    speaking.value = false;
  };
  window.speechSynthesis.cancel();
  speaking.value = true;
  window.speechSynthesis.speak(utterance);
}

onBeforeUnmount(() => {
  if (
    speaking.value &&
    typeof window !== 'undefined' &&
    'speechSynthesis' in window
  ) {
    window.speechSynthesis.cancel();
  }
});
</script>

<template>
  <div class="group flex flex-col items-start">
    <div class="flex max-w-[95%] items-start gap-2">
      <div
        class="inline-flex size-8 shrink-0 items-center justify-center rounded-full border border-border bg-background"
      >
        <img
          v-if="appIcon"
          :src="appIcon"
          alt="avatar"
          class="size-7 rounded-full object-cover"
        />
        <Bot v-else class="size-4 text-primary" />
      </div>

      <Bubble content="" placement="start">
        <template #message>
          <div class="space-y-1.5">
            <template v-for="item in entries" :key="item.id">
              <div
                v-if="item.type === 'message' && item.role === 'assistant'"
                class="whitespace-pre-wrap break-words text-sm leading-6 text-foreground"
              >
                {{ item.content }}
              </div>
              <ChatToolEventCard
                v-else-if="item.type === 'tool'"
                :item="item"
              />
              <ChatRagEventCard v-else-if="item.type === 'rag'" :item="item" />
            </template>
          </div>
        </template>
      </Bubble>
    </div>

    <div
      v-if="pending"
      class="ml-10 mt-1 inline-flex items-center gap-1.5 text-xs text-muted-foreground"
    >
      <LoaderCircle class="size-3.5 animate-spin text-primary" />
      <Shimmer class="text-xs">AI 生成中...</Shimmer>
    </div>

    <div class="ml-10 mt-1 flex items-center gap-2">
      <div class="flex items-center gap-2 opacity-0 transition-opacity duration-150 group-hover:opacity-100">
        <span class="text-[10px] text-muted-foreground">
          {{ assistantTimeText }}
        </span>
        <ChatMessageActions
          :speaking="speaking"
          @copy="copyMessage"
          @speak="toggleSpeech"
        />
      </div>
    </div>
  </div>
</template>
