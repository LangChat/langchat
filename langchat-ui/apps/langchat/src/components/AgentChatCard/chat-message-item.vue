<script lang="ts" setup>
import {computed, onBeforeUnmount, ref} from 'vue';

import {Bot, User} from '@vben/icons';

import {Bubble} from 'ant-design-x-vue';

import {message} from '#/adapter/naive';

import ChatMessageActions from './chat-message-actions.vue';

interface Props {
  appIcon?: string;
  content: string;
  role: 'assistant' | 'user';
  timeText?: string;
  userIcon?: string;
}

const props = withDefaults(defineProps<Props>(), {
  appIcon: '',
  timeText: '',
  userIcon: '',
});

const speaking = ref(false);

const isUser = computed(() => props.role === 'user');
const avatar = computed(() => (isUser.value ? props.userIcon : props.appIcon));

function copyMessage() {
  const text = String(props.content || '').trim();
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
  const text = String(props.content || '').trim();
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
  <div :class="isUser ? 'items-end' : 'items-start'" class="group flex flex-col">
    <div
      :class="isUser ? 'flex-row-reverse' : 'flex-row'"
      class="flex max-w-[95%] items-start gap-2"
    >
      <div
        class="inline-flex size-8 shrink-0 items-center justify-center rounded-full border border-border bg-background"
      >
        <img
          v-if="avatar"
          :src="avatar"
          alt="avatar"
          class="size-7 rounded-full object-cover"
        />
        <User v-if="isUser" class="size-4 text-primary" />
        <Bot v-else class="size-4 text-primary" />
      </div>

      <Bubble :content="content" :placement="isUser ? 'end' : 'start'" />
    </div>

    <div
      :class="isUser ? 'mr-10 justify-end' : 'ml-10 justify-start'"
      class="mt-1 flex items-center gap-2"
    >
      <div class="flex items-center gap-2.5 opacity-0 transition-opacity duration-150 group-hover:opacity-100">
        <span class="text-[10px] text-muted-foreground">
          {{ timeText }}
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
