<script lang="ts" setup>
import type { AigcAgent } from '#/api/aigc/agent';
import type {
  LcChatMessage,
  LcChatSendPayload,
} from '#/components/LcChat/types';

import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { Page } from '@vben/common-ui';
import { Check, Plus, SquarePen, Trash2, X } from '@vben/icons';
import { $t } from '@vben/locales';

import { NButton, NInput } from 'naive-ui';

import { dialog, message } from '#/adapter/naive';
import {
  createConversationApi,
  removeConversationApi,
  updateConversationApi,
} from '#/api/aigc/chat';
import LcChat from '#/components/LcChat/index.vue';
import { useChatRuntime } from '#/views/shared/chat/use-chat-runtime';

const route = useRoute();
const historyKeyword = ref('');
const historyEditingId = ref('');
const historyEditingTitle = ref('');
const chatRef = ref<InstanceType<typeof LcChat> | null>(null);

const {
  conversationSidebarItems,
  draftMessage,
  initializeRuntime,
  loadConversations,
  messageLoading,
  messageItems,
  selectedAgent,
  selectedAgentId,
  selectedConversationId,
  sending,
  sendMessage,
  selectAgent,
  selectConversation,
} = useChatRuntime({
  enableHistory: true,
});

const routeAgentId = computed(() => String(route.params.agentId || ''));

function parseMessageContent(raw: string) {
  const text = String(raw || '').trim();
  if (!text) {
    return '';
  }
  const segments = text
    .split(/\n\s*\n/)
    .map((item) => item.trim())
    .filter(Boolean);
  const parsed: string[] = [];
  for (const segment of segments) {
    try {
      const payload = JSON.parse(segment) as { content?: unknown };
      const content = String(payload?.content ?? '').trim();
      if (content) {
        parsed.push(content);
      }
    } catch {
      parsed.push(segment);
    }
  }
  return parsed.join('\n\n');
}

const marketChatMessages = computed<LcChatMessage[]>(() =>
  messageItems.value
    .filter(
      (
        item,
      ): item is {
        content: string;
        id: string;
        role: 'assistant' | 'user';
      } => item.role === 'assistant' || item.role === 'user',
    )
    .map((item) => ({
      content: parseMessageContent(item.content),
      id: item.id,
      role: item.role,
      status: 'completed' as const,
    })),
);

const marketSuggestions = computed(() =>
  resolveDefaultSuggestions(selectedAgent.value as AigcAgent).map((label) => ({
    label,
  })),
);

const filteredConversationItems = computed(() => {
  const query = historyKeyword.value.trim().toLowerCase();
  if (!query) {
    return conversationSidebarItems.value;
  }
  return conversationSidebarItems.value.filter((item) =>
    String(item.title || '').toLowerCase().includes(query),
  );
});

function resolveAgentPreviewIcon(item: AigcAgent | null) {
  const icon = String(item?.icon || '').trim();
  if (/^https?:\/\//.test(icon)) {
    return icon;
  }
  const avatar = String(item?.avatar || '').trim();
  if (/^https?:\/\//.test(avatar)) {
    return avatar;
  }
  return '';
}

function resolveDefaultSuggestions(item: AigcAgent | null) {
  if (!item?.defaultSuggestions) {
    return [];
  }
  try {
    const parsed = JSON.parse(item.defaultSuggestions);
    if (Array.isArray(parsed)) {
      return parsed
        .map((value) => String(value || '').trim())
        .filter(Boolean)
        .slice(0, 6);
    }
  } catch {
    // noop
  }
  return [];
}

async function handleSubmitMessage(payload: LcChatSendPayload) {
  draftMessage.value = String(payload.text || '').trim();
  await sendMessage(payload);
}

async function syncChatMessages() {
  await nextTick();
  chatRef.value?.setMessages(
    marketChatMessages.value.map((item, index, list) => ({
      ...item,
      status:
        sending.value &&
        item.role === 'assistant' &&
        index === list.length - 1
          ? 'running'
          : 'completed',
    })),
  );
}

async function createNewConversation() {
  const agentId = selectedAgentId.value;
  if (!agentId) {
    message.warning($t('chat.conversation.selectAgentFirst'));
    return;
  }
  const created = await createConversationApi({
    agentId,
    title: $t('chat.conversation.newTitle', {
      time: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
    }),
  });
  await loadConversations(agentId);
  const createdConversationId = String(created?.id || '');
  const targetConversationId =
    createdConversationId || conversationSidebarItems.value[0]?.id || '';
  if (targetConversationId) {
    await selectConversation(targetConversationId);
  }
}

function startEditConversation(id: string, title?: string) {
  historyEditingId.value = id;
  historyEditingTitle.value = String(title || '');
}

function cancelEditConversation() {
  historyEditingId.value = '';
  historyEditingTitle.value = '';
}

async function confirmEditConversation(id: string) {
  const title = historyEditingTitle.value.trim();
  if (!title) {
    message.warning($t('chat.conversation.titleRequired'));
    return;
  }
  await updateConversationApi(id, { title });
  message.success($t('chat.conversation.titleUpdated'));
  await loadConversations(selectedAgentId.value);
  cancelEditConversation();
}

async function handleDeleteConversation(id: string, title?: string) {
  dialog.warning({
    closable: false,
    content: $t('common.messages.deleteConfirmContent', {
      name: title || $t('chat.conversation.untitled'),
    }),
    negativeText: $t('common.actions.cancel'),
    positiveText: $t('common.actions.confirmDelete'),
    title: $t('chat.conversation.deleteTitle'),
    onPositiveClick: async () => {
      await removeConversationApi(id);
      message.success($t('chat.conversation.deleteSuccess'));
      if (selectedConversationId.value === id) {
        await selectConversation('');
      }
      await loadConversations(selectedAgentId.value);
    },
  });
}

async function prepareRuntime() {
  await initializeRuntime();
  if (routeAgentId.value) {
    await selectAgent(routeAgentId.value);
    const firstConversationId =
      selectedConversationId.value || conversationSidebarItems.value[0]?.id || '';
    if (firstConversationId) {
      await selectConversation(firstConversationId);
    }
  }
}

watch(routeAgentId, async (value) => {
  if (value) {
    await selectAgent(value);
    const firstConversationId =
      selectedConversationId.value || conversationSidebarItems.value[0]?.id || '';
    if (firstConversationId) {
      await selectConversation(firstConversationId);
    }
  }
});

watch(
  filteredConversationItems,
  async (items) => {
    if (!selectedConversationId.value && items.length > 0) {
      await selectConversation(items[0]!.id);
    }
  },
  { immediate: true },
);

watch([marketChatMessages, sending], syncChatMessages, {
  deep: true,
  flush: 'post',
  immediate: true,
});

onMounted(prepareRuntime);
</script>

<template>
  <Page>
    <div
      class="h-full min-h-0 overflow-hidden rounded-xl bg-background p-4 lg:p-5"
    >
      <div class="grid h-full min-h-0 gap-4 lg:grid-cols-[300px_minmax(0,1fr)]">
        <aside
          class="flex min-h-0 flex-col rounded-xl border border-border bg-card/60 px-4 py-4"
        >
          <div class="mb-2 text-base font-semibold text-foreground">
            {{ $t('chat.conversation.history') }}
          </div>
          <div class="mb-3">
            <NInput
              v-model:value="historyKeyword"
              clearable
              :placeholder="$t('chat.conversation.searchPlaceholder')"
              size="large"
            />
          </div>
          <div class="mb-3">
            <NButton block size="large" type="primary" @click="createNewConversation">
              <template #icon>
                <Plus class="size-4" />
              </template>
              {{ $t('chat.conversation.newConversation') }}
            </NButton>
          </div>
          <div class="min-h-0 flex-1 space-y-2 overflow-y-auto pr-1">
            <div
              v-for="item in filteredConversationItems"
              :key="item.id"
              :class="
              item.id === selectedConversationId
                ? 'border-primary/50 bg-primary/8'
                : 'border-border bg-card hover:border-primary/35 hover:bg-primary/5'
            "
              class="rounded-md border px-3 py-2.5 transition-colors"
            >
              <div v-if="historyEditingId !== item.id" class="flex items-center gap-1.5">
                <button class="min-w-0 flex-1 text-left" type="button" @click="selectConversation(item.id)">
                  <div class="truncate text-sm font-normal text-foreground">{{ item.title }}</div>
                  <div class="mt-1 truncate text-[10px] text-muted-foreground">
                    {{ item.meta || '--' }}
                  </div>
                </button>
                <NButton
                  circle
                  quaternary
                  size="tiny"
                  @click="startEditConversation(item.id, item.title)"
                >
                  <SquarePen class="size-3" />
                </NButton>
                <NButton
                  circle
                  quaternary
                  size="tiny"
                  type="error"
                  @click="handleDeleteConversation(item.id, item.title)"
                >
                  <Trash2 class="size-3" />
                </NButton>
              </div>
              <div v-else class="flex items-center gap-1">
                <NInput
                  v-model:value="historyEditingTitle"
                  :placeholder="$t('chat.conversation.titlePlaceholder')"
                  size="small"
                />
                <NButton circle size="tiny" type="primary" @click="confirmEditConversation(item.id)">
                  <Check class="size-3" />
                </NButton>
                <NButton circle quaternary size="tiny" @click="cancelEditConversation">
                  <X class="size-3" />
                </NButton>
              </div>
            </div>
            <div
              v-if="filteredConversationItems.length === 0"
              class="rounded-md border border-dashed border-border px-3 py-5 text-center text-xs text-muted-foreground"
            >
              {{ $t('market.sessionEmpty') }}
            </div>
          </div>
        </aside>

        <section
          class="flex h-full min-h-0 min-w-0 flex-col rounded-xl border border-border bg-card p-3"
        >
          <div class="mb-2 shrink-0 border-b border-border pb-2">
            <div class="text-sm font-semibold text-foreground">
              {{ $t('market.sessionHeading') }}
            </div>
            <div class="mt-0.5 truncate text-xs text-muted-foreground">
              {{
                selectedAgent?.agentName ||
                $t('chat.conversation.defaultAgentName')
              }}
            </div>
          </div>
          <div class="min-h-0 flex-1">
            <LcChat
              ref="chatRef"
              :assistant-icon="
                resolveAgentPreviewIcon(selectedAgent as AigcAgent)
              "
              :disabled="sending"
              :empty-title="
                selectedAgent?.welcomeMessage || $t('chat.empty.welcome')
              "
              :loading="messageLoading"
              :placeholder="$t('chat.composer.inputMessage')"
              :show-attachment="true"
              :show-model="false"
              :suggestions="marketSuggestions"
              @send="handleSubmitMessage"
            >
              <template #empty-head>
                <div class="flex flex-col items-center gap-3 text-center">
                  <img
                    v-if="resolveAgentPreviewIcon(selectedAgent as AigcAgent)"
                    alt=""
                    class="size-12 rounded-xl border border-border object-cover"
                    :src="resolveAgentPreviewIcon(selectedAgent as AigcAgent)"
                  />
                  <div class="text-base font-semibold text-foreground">
                    {{
                      selectedAgent?.welcomeMessage || $t('chat.empty.welcome')
                    }}
                  </div>
                </div>
              </template>
            </LcChat>
          </div>
        </section>
      </div>
    </div>
  </Page>
</template>
