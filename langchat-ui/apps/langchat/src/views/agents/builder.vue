<script lang="ts" setup>
import type {AigcAgent} from '#/api/aigc/agent';
import type {
  AgentChatStreamEvent,
  OpenAiChatCompletionChunk,
  OpenAiDeltaEventPayload,
} from '#/api/aigc/chat';
import type {ChatMessage, ChatRole} from '#/components/chat';

import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {preferences} from '@vben/preferences';
import {useUserStore} from '@vben/stores';

import {NEmpty} from 'naive-ui';

import {message} from '#/adapter/naive';
import {agentApi} from '#/api/aigc/agent';
import {CHAT_STREAM_EVENT, listConversationMessagesApi} from '#/api/aigc/chat';
import {knowledgeApi} from '#/api/aigc/knowledge';
import {mcpApi} from '#/api/aigc/mcp';
import {skillApi} from '#/api/aigc/skill';
import {startAgentChatStream} from '#/components/AgentChatCard/stream-client';
import {
  buildModelConfigJson,
  type ModelConfig,
  parseMetaJson,
  parseModelConfig,
  parseSuggestions,
} from '#/views/agents/builder-utils';
import AgentApiKeyPanel from '#/views/agents/components/agent-api-key-panel.vue';
import AgentBuilderConfigView from '#/views/agents/components/agent-builder-config-view.vue';
import AgentBuilderHeader from '#/views/agents/components/agent-builder-header.vue';
import AgentMessageLogPanel from '#/views/agents/components/agent-message-log-panel.vue';
import AgentMessageStatsPanel from '#/views/agents/components/agent-message-stats-panel.vue';
import {parseIdList, stringifyIdList} from '#/views/shared/aigc/id-list';
import {useAigcLookups} from '#/views/shared/aigc/lookups';
import {AGENT_STATUS_OPTIONS, findOptionLabel,} from '#/views/shared/aigc/options';

type BuilderTab = 'config' | 'keys' | 'logs' | 'stats';

interface AgentFormModel extends Partial<AigcAgent> {
  defaultSuggestionsList: string[];
  knowledgeIdsList: string[];
  mcpIdsList: string[];
  metaExtras: Record<string, unknown>;
  modelConfig: ModelConfig;
  skillIdsList: string[];
}

const DEFAULT_WELCOME = '欢迎使用当前应用，先从一个问题开始。';
const DEFAULT_SUGGESTIONS = [
  '介绍这个应用能做什么',
  '给我一个快速上手示例',
  '推荐下一步操作',
];
const META_KEYS = new Set([
  'defaultSuggestions',
  'enableAutoSuggestion',
  'icon',
  'modelType',
  'welcomeMessage',
]);

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const { loadLookups, lookups } = useAigcLookups({
  knowledges: true,
  models: true,
  skills: true,
});

const loading = ref(false);
const saving = ref(false);
const chatLoading = ref(false);
/** 历史消息加载中(聊天调试区展示 v-loading) */
const historyLoading = ref(false);
const chatConversationId = ref('');
const streamAbortController = ref<AbortController | null>(null);
const configViewRef = ref<InstanceType<typeof AgentBuilderConfigView> | null>(
  null,
);

function chatApi() {
  return configViewRef.value?.layoutRef ?? null;
}

const formModel = ref<AgentFormModel>(buildDefaultModel());
const knowledgeEntities = ref<any[]>([]);
const skillEntities = ref<any[]>([]);
const mcpEntities = ref<any[]>([]);
const activeTab = ref<BuilderTab>('config');

const agentId = computed(() => String(route.params.id || ''));
const isCreateMode = computed(() => !agentId.value || agentId.value === 'new');
const pageTitle = computed(() =>
  isCreateMode.value
    ? '新建 Agent 应用'
    : formModel.value.agentName || 'Agent 应用详情',
);
const statusLabel = computed(() =>
  findOptionLabel(AGENT_STATUS_OPTIONS, formModel.value.status || 'DRAFT'),
);
const formSummary = computed(() => formModel.value.agentName || '未命名应用');
const modelLabel = computed(
  () =>
    lookups.value.models.find(
      (item) =>
        String(item.value) === String(formModel.value.reasoningModelId || ''),
    )?.label || '未配置',
);
const userAvatar = computed(
  () => userStore.userInfo?.avatar || preferences.app.defaultAvatar,
);
const statusType = computed(() => {
  if (formModel.value.status === 'PUBLISHED') {
    return 'success';
  }
  if (formModel.value.status === 'DISABLED') {
    return 'error';
  }
  return 'warning';
});
const builderTabs = computed(() => [
  { key: 'config' as BuilderTab, label: '配置页' },
  { key: 'keys' as BuilderTab, label: 'API 接入' },
  { key: 'logs' as BuilderTab, label: '消息日志' },
  { key: 'stats' as BuilderTab, label: '统计报表' },
]);
const normalizedDefaultSuggestions = computed(() => {
  const values = parseSuggestions(formModel.value.defaultSuggestionsList);
  return values.length > 0 ? values : DEFAULT_SUGGESTIONS;
});

const knowledgeRelationOptions = computed(() =>
  knowledgeEntities.value
    .map((item) => {
      const vectorModelLabel =
        lookups.value.models.find(
          (option) =>
            String(option.value) === String(item.vectorModelId || item.modelId),
        )?.label || '--';
      return {
        description:
          item.description ||
          `补充 ${item.name || '当前知识库'} 的业务知识上下文。`,
        label: item.name || '未命名知识库',
        metrics: [
          `向量模型 ${vectorModelLabel}`,
          `TopK ${item.maxResults ?? '--'}`,
          '文档 --',
        ],
        tags: [item.rerank ? '重排开启' : '无重排', '知识库'],
        value: String(item.id || ''),
      };
    })
    .filter((item) => item.value),
);

const skillRelationOptions = computed(() =>
  skillEntities.value
    .map((item) => ({
      description: item.description || '该技能暂未填写描述。',
      label: item.title || item.name || '未命名技能',
      metrics: [
        `版本 ${item.version || '--'}`,
        `文件数 ${item.fileCount ?? '--'}`,
      ],
      tags: [item.enabled ? '启用' : '停用'],
      value: String(item.id || ''),
    }))
    .filter((item) => item.value),
);

const mcpRelationOptions = computed(() =>
  mcpEntities.value
    .map((item) => ({
      description: item.description || '该 MCP 服务暂无描述信息。',
      label: item.name || '未命名 MCP 服务',
      metrics: [`协议 ${item.transport || '--'}`],
      tags: [item.authorized ? '已授权' : '待授权', 'MCP'],
      value: String(item.id || ''),
    }))
    .filter((item) => item.value),
);

function buildDefaultModel(source: Partial<AigcAgent> = {}): AgentFormModel {
  const meta = parseMetaJson(source.metaJson);
  const metaExtras = { ...meta };
  META_KEYS.forEach((key) => {
    delete metaExtras[key];
  });

  const defaultSuggestionsSource =
    source.defaultSuggestions ?? meta.defaultSuggestions;
  const defaultSuggestions = parseSuggestions(defaultSuggestionsSource);

  return {
    ...source,
    defaultSuggestionsList:
      defaultSuggestions.length > 0
        ? defaultSuggestions
        : [...DEFAULT_SUGGESTIONS],
    enableAutoSuggestion:
      source.enableAutoSuggestion ?? Boolean(meta.enableAutoSuggestion),
    knowledgeIdsList: parseIdList(source.knowledgeIds),
    mcpIdsList: parseIdList(source.mcpIds),
    metaExtras,
    modelConfig: parseModelConfig(source.modelConfigJson),
    skillIdsList: parseIdList(source.skillIds),
    status: source.status || 'DRAFT',
    icon: source.icon || String(meta.icon || ''),
    welcomeMessage:
      source.welcomeMessage || String(meta.welcomeMessage || DEFAULT_WELCOME),
  };
}

function buildMetaJson() {
  const merged = {
    ...formModel.value.metaExtras,
    defaultSuggestions: normalizedDefaultSuggestions.value,
    enableAutoSuggestion: Boolean(formModel.value.enableAutoSuggestion),
    icon: String(formModel.value.icon || ''),
    modelType: 'REASONING',
    welcomeMessage: (formModel.value.welcomeMessage || DEFAULT_WELCOME).trim(),
  };
  return JSON.stringify(merged);
}

function resolveBuilderConversationId(id: string) {
  return id;
}

async function loadBuilderChatHistory() {
  await nextTick();
  chatApi()?.reset();
  if (isCreateMode.value || !agentId.value) {
    chatConversationId.value = '';
    return;
  }

  const conversationId = resolveBuilderConversationId(agentId.value);
  chatConversationId.value = conversationId;
  historyLoading.value = true;
  try {
    const messages = await listConversationMessagesApi(conversationId);
    const history: ChatMessage[] = messages
      .filter((item) => {
        const role = String(item.role || '').toLowerCase();
        return role === 'user' || role === 'assistant';
      })
      .map((item) => ({
        content: String(item.message || ''),
        id: String(item.id || item.chatId || `${conversationId}-${Date.now()}`),
        role: String(item.role || '').toLowerCase() as ChatRole,
        status: 'completed' as const,
      }));
    chatApi()?.setMessages(history);
  } catch {
    // 首次调试时会话尚未创建，发送第一条消息时由后端创建。
  } finally {
    historyLoading.value = false;
  }
}

async function loadCurrentAgent() {
  if (isCreateMode.value) {
    formModel.value = buildDefaultModel();
    return;
  }

  loading.value = true;
  try {
    const detail = await agentApi.detail(agentId.value);
    formModel.value = buildDefaultModel(detail);
  } finally {
    loading.value = false;
  }
}

async function initializePage() {
  await Promise.all([
    loadLookups(),
    loadCurrentAgent(),
    knowledgeApi.list().then((items) => {
      knowledgeEntities.value = items;
    }),
    skillApi.list().then((items) => {
      skillEntities.value = items;
    }),
    mcpApi.list().then((items) => {
      mcpEntities.value = items;
    }),
  ]);
  await loadBuilderChatHistory();
}

function goBack() {
  void router.push('/agents');
}

async function handleSave(nextStatus?: 'DISABLED' | 'PUBLISHED') {
  saving.value = true;
  try {
    const { agentName, avatar, description, systemPrompt } = formModel.value;
    const targetStatus = nextStatus || formModel.value.status || 'DRAFT';
    const payload: Partial<AigcAgent> = {
      agentName,
      avatar,
      description,
      defaultSuggestions: JSON.stringify(normalizedDefaultSuggestions.value),
      enableAutoSuggestion: Boolean(formModel.value.enableAutoSuggestion),
      icon: String(formModel.value.icon || ''),
      knowledgeIds: stringifyIdList(formModel.value.knowledgeIdsList),
      mcpIds: stringifyIdList(formModel.value.mcpIdsList),
      metaJson: buildMetaJson(),
      modelConfigJson: buildModelConfigJson(formModel.value.modelConfig),
      reasoningModelId: formModel.value.reasoningModelId,
      skillIds: stringifyIdList(formModel.value.skillIdsList),
      status: targetStatus,
      systemPrompt: (systemPrompt || '').trim(),
      welcomeMessage: (
        formModel.value.welcomeMessage || DEFAULT_WELCOME
      ).trim(),
    };

    if (isCreateMode.value) {
      await agentApi.create(payload);
      message.success('Agent 应用已创建');
      void router.push('/agents');
      return;
    }

    await agentApi.update(agentId.value, payload);
    formModel.value.status = targetStatus;
    message.success('Agent 应用已保存');
  } finally {
    saving.value = false;
  }
}

async function handleChatSubmit(content: string) {
  if (isCreateMode.value || !agentId.value) {
    message.warning('请先保存当前 Agent，再进行聊天调试');
    return;
  }

  const chat = chatApi();
  if (!chat) {
    return;
  }

  const assistantId = chat.beginTurn(content);

  chatLoading.value = true;
  try {
    streamAbortController.value?.abort();
    const controller = new AbortController();
    streamAbortController.value = controller;

    await startAgentChatStream(
      {
        agentId: agentId.value,
        conversationId: chatConversationId.value || undefined,
        messages: [
          {
            content,
            role: 'user',
          },
        ],
        stream: true,
      },
      {
        onEvent: (event) => {
          if (event.conversationId) {
            chatConversationId.value = String(event.conversationId);
          }
          applyChatStreamEvent(chat, assistantId, event);
        },
        signal: controller.signal,
      },
    );
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      return;
    }
    const descriptionText =
      error instanceof Error ? error.message : '聊天请求失败';
    chat.failTurn(assistantId, descriptionText);
    message.error(descriptionText);
  } finally {
    streamAbortController.value = null;
    chatLoading.value = false;
  }
}

/** 将后端流事件映射到布局的 OpenAI 风格 turn 生命周期 */
function applyChatStreamEvent(
  chat: NonNullable<ReturnType<typeof chatApi>>,
  assistantId: string,
  event: AgentChatStreamEvent,
) {
  switch (event.eventType) {
    case CHAT_STREAM_EVENT.DONE:
    case CHAT_STREAM_EVENT.MESSAGE_COMPLETED:
    case CHAT_STREAM_EVENT.MESSAGE_STOP: {
      chat.completeTurn(assistantId);
      break;
    }
    case CHAT_STREAM_EVENT.ERROR: {
      const nested = extractNestedEvent(event);
      chat.failTurn(assistantId, String(nested?.message || '对话生成失败。'));
      break;
    }
    case CHAT_STREAM_EVENT.MESSAGE_DELTA: {
      const delta = extractDeltaContent(event);
      if (delta) {
        chat.appendDelta(assistantId, delta);
      }
      break;
    }
    case CHAT_STREAM_EVENT.RAG_RETRIEVED: {
      chat.setTurnMeta(assistantId, '知识库检索完成');
      break;
    }
    case CHAT_STREAM_EVENT.TIMEOUT: {
      chat.failTurn(assistantId, '对话超时，请稍后重试。');
      break;
    }
    case CHAT_STREAM_EVENT.TOOL_BEFORE: {
      const nested = extractNestedEvent(event);
      chat.setTurnMeta(
        assistantId,
        nested?.tool_name ? `正在调用技能 ${nested.tool_name}` : '正在调用技能',
      );
      break;
    }
    case CHAT_STREAM_EVENT.TOOL_EXECUTED: {
      const nested = extractNestedEvent(event);
      const failed = Boolean(nested?.failed || nested?.status === 'failed');
      chat.setTurnMeta(assistantId, failed ? '技能执行失败' : '技能执行完成');
      break;
    }
    default: {
      break;
    }
  }
}

function extractDeltaContent(event: AgentChatStreamEvent): string {
  const chunk = event.payload as OpenAiChatCompletionChunk | undefined;
  return chunk?.choices?.[0]?.delta?.content || '';
}

function extractNestedEvent(
  event: AgentChatStreamEvent,
): OpenAiDeltaEventPayload | undefined {
  const chunk = event.payload as OpenAiChatCompletionChunk | undefined;
  return chunk?.choices?.[0]?.delta?.event as
    | OpenAiDeltaEventPayload
    | undefined;
}

watch(agentId, async () => {
  streamAbortController.value?.abort();
  chatConversationId.value = '';
  activeTab.value = 'config';
  await initializePage();
});

onMounted(initializePage);
onBeforeUnmount(() => {
  streamAbortController.value?.abort();
});
</script>

<template>
  <Page auto-content-height>
    <div class="flex h-full flex-col gap-2">
      <AgentBuilderHeader
        v-model:tab="activeTab"
        :app-icon="formModel.icon || formModel.avatar || ''"
        :create-time="formModel.createTime || 0"
        :creator="formModel.creator || ''"
        :model-label="modelLabel"
        :page-title="pageTitle"
        :saving="saving"
        :status-label="statusLabel"
        :status-type="statusType"
        :summary="formSummary"
        :tabs="builderTabs"
        :update-time="formModel.updateTime || 0"
        :updater="formModel.updater || ''"
        @back="goBack"
        @save-draft="handleSave()"
        @save-with-status="handleSave"
      />

      <div v-if="!loading" class="min-h-0 flex-1 overflow-hidden rounded-xl">
        <AgentBuilderConfigView
          v-if="activeTab === 'config'"
          ref="configViewRef"
          v-model="formModel"
          :app-icon="formModel.icon || formModel.avatar || ''"
          :chat-loading="chatLoading"
          :default-suggestions="normalizedDefaultSuggestions"
          :history-loading="historyLoading"
          :knowledge-relation-options="knowledgeRelationOptions"
          :mcp-relation-options="mcpRelationOptions"
          :model-entities="lookups.modelEntities"
          :skill-relation-options="skillRelationOptions"
          :user-icon="userAvatar"
          :welcome-message="formModel.welcomeMessage || DEFAULT_WELCOME"
          @submit="handleChatSubmit"
        />

        <AgentApiKeyPanel
          v-else-if="activeTab === 'keys'"
          :agent-id="isCreateMode ? '' : agentId"
        />

        <AgentMessageLogPanel
          v-else-if="activeTab === 'logs'"
          :agent-id="isCreateMode ? '' : agentId"
        />

        <AgentMessageStatsPanel
          v-else
          :agent-id="isCreateMode ? '' : agentId"
        />
      </div>

      <div
        v-else
        class="rounded-xl border border-dashed border-border bg-card px-6 py-16"
      >
        <NEmpty description="正在加载 Agent 应用详情..." />
      </div>
    </div>
  </Page>
</template>
