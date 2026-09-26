<script lang="ts" setup>
import type {AigcAgent} from '#/api/aigc/agent';

import {computed, onMounted, ref, watch} from 'vue';
import {useRoute} from 'vue-router';

import {Page} from "@vben/common-ui";
import {Check, Plus, SquarePen, Trash2, X} from '@vben/icons';

import {NButton, NInput} from 'naive-ui';

import {dialog, message} from '#/adapter/naive';
import {
  createConversationApi,
  removeConversationApi,
  updateConversationApi,
} from '#/api/aigc/chat';
import AgentChatCard from '#/components/AgentChatCard/index.vue';
import {useChatRuntime} from '#/views/shared/chat/use-chat-runtime';

const route = useRoute();
const historyKeyword = ref('');
const historyEditingId = ref('');
const historyEditingTitle = ref('');

const {
  conversationSidebarItems,
  draftMessage,
  initializeRuntime,
  loadConversations,
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

const marketChatMessages = computed(() =>
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

async function handleSubmitMessage(content: string) {
  draftMessage.value = String(content || '').trim();
  await sendMessage();
}

async function createNewConversation() {
  const agentId = selectedAgentId.value;
  if (!agentId) {
    message.warning('当前未选择应用，无法创建会话');
    return;
  }
  const created = await createConversationApi({
    agentId,
    title: `新会话 ${new Date().toLocaleTimeString('zh-CN', {hour12: false})}`,
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
    message.warning('会话标题不能为空');
    return;
  }
  await updateConversationApi(id, { title });
  message.success('会话标题已更新');
  await loadConversations(selectedAgentId.value);
  cancelEditConversation();
}

async function handleDeleteConversation(id: string, title?: string) {
  dialog.warning({
    closable: false,
    content: `确认删除会话「${title || '未命名会话'}」吗？`,
    negativeText: '取消',
    positiveText: '确认删除',
    title: '删除会话',
    onPositiveClick: async () => {
      await removeConversationApi(id);
      message.success('会话已删除');
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
          <div class="mb-2 text-base font-semibold text-foreground">历史会话</div>
          <div class="mb-3">
            <NInput
              v-model:value="historyKeyword"
              clearable
              placeholder="搜索会话标题"
              size="large"
            />
          </div>
          <div class="mb-3">
            <NButton block size="large" type="primary" @click="createNewConversation">
              <template #icon>
                <Plus class="size-4" />
              </template>
              新增会话
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
                  placeholder="请输入会话标题"
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
              暂无历史会话
            </div>
          </div>
        </aside>

        <AgentChatCard
          :app-icon="resolveAgentPreviewIcon(selectedAgent as AigcAgent)"
          :default-suggestions="resolveDefaultSuggestions(selectedAgent as AigcAgent)"
          :loading="sending"
          :messages="marketChatMessages"
          :welcome-message="
          selectedAgent?.welcomeMessage || '欢迎使用当前应用，先从一个问题开始。'
        "
          class="h-full min-h-0 !p-5"
          placeholder="请输入消息内容"
          title="会话"
          @submit="handleSubmitMessage"
        />
      </div>
    </div>
  </Page>
</template>
