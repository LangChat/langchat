<script lang="ts" setup>
import type {AigcAgent} from '#/api/aigc/agent';
import type {
  AgentChatStreamEvent,
  OpenAiChatCompletionChunk,
  OpenAiDeltaEventPayload,
} from '#/api/aigc/chat';
import type {
  LcChatMessage,
  LcChatRole,
  LcChatSendPayload,
} from '#/components/LcChat/types';

import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';

import {Page} from '@vben/common-ui';
import {$t} from '@vben/locales';
import {preferences} from '@vben/preferences';
import {useUserStore} from '@vben/stores';

import {NEmpty} from 'naive-ui';

import {message} from '#/adapter/naive';
import {agentApi} from '#/api/aigc/agent';
import {CHAT_STREAM_EVENT, listConversationMessagesApi} from '#/api/aigc/chat';
import {knowledgeApi} from '#/api/aigc/knowledge';
import {mcpApi} from '#/api/aigc/mcp';
import {skillApi} from '#/api/aigc/skill';
import { startAgentChatStream } from '#/components/LcChat/stream-client';
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
import {agentStatusOptions, findOptionLabel,} from '#/views/shared/aigc/options';

type BuilderTab = 'config' | 'keys' | 'logs' | 'stats';

interface AgentFormModel extends Partial<AigcAgent> {
  defaultSuggestionsList: string[];
  knowledgeIdsList: string[];
  mcpIdsList: string[];
  metaExtras: Record<string, unknown>;
  modelConfig: ModelConfig;
  skillIdsList: string[];
}

const DEFAULT_WELCOME = computed(() => $t('agents.configView.welcomeFallback'));
const DEFAULT_SUGGESTIONS = computed(() => [
  $t('agents.configView.suggestionDefaults.first'),
  $t('agents.configView.suggestionDefaults.second'),
  $t('agents.configView.suggestionDefaults.third'),
]);
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
    ? $t('agents.title.builderNew')
    : formModel.value.agentName || $t('agents.title.builderDetail'),
);
const statusLabel = computed(() =>
  findOptionLabel(agentStatusOptions(), formModel.value.status || 'DRAFT'),
);
const formSummary = computed(() =>
  formModel.value.agentName || $t('agents.header.untitled'),
);
const modelLabel = computed(
  () =>
    lookups.value.models.find(
      (item) =>
        String(item.value) === String(formModel.value.reasoningModelId || ''),
    )?.label || $t('common.status.notConfigured'),
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
  { key: 'config' as BuilderTab, label: $t('agents.builder.tabs.config') },
  { key: 'keys' as BuilderTab, label: $t('agents.builder.tabs.apiKeys') },
  { key: 'logs' as BuilderTab, label: $t('agents.builder.tabs.logs') },
  { key: 'stats' as BuilderTab, label: $t('agents.builder.tabs.stats') },
]);
const normalizedDefaultSuggestions = computed(() => {
  const values = parseSuggestions(formModel.value.defaultSuggestionsList);
  return values.length > 0 ? values : DEFAULT_SUGGESTIONS.value;
});

const knowledgeRelationOptions = computed(() =>
  knowledgeEntities.value
    .map((item) => {
      const vectorModelLabel =
        lookups.value.models.find(
          (option) => String(option.value) === String(item.vectorModelId),
        )?.label || '--';
      return {
        description:
          item.description ||
          $t('agents.builder.knowledgeCard.description', {
            name: item.name || $t('agents.builder.knowledgeCard.untitled'),
          }),
        label: item.name || $t('agents.builder.knowledgeCard.untitled'),
        metrics: [
          $t('agents.builder.knowledgeCard.vectorModel', {
            name: vectorModelLabel,
          }),
          $t('agents.builder.knowledgeCard.topK', {
            count: item.maxResults ?? '--',
          }),
          $t('agents.builder.knowledgeCard.docsCount', { count: '--' }),
        ],
        tags: [$t('common.labels.knowledgeBase')],
        value: String(item.id || ''),
      };
    })
    .filter((item) => item.value),
);

const skillRelationOptions = computed(() =>
  skillEntities.value
    .map((item) => ({
      description:
        item.description || $t('agents.builder.skillCard.descriptionFallback'),
      label: item.title || item.name || $t('agents.builder.skillCard.untitled'),
      metrics: [
        $t('agents.builder.skillCard.version', {
          version: item.version || '--',
        }),
        $t('agents.builder.skillCard.filesCount', {
          count: item.fileCount ?? '--',
        }),
      ],
      tags: [
        item.enabled
          ? $t('common.status.enabled')
          : $t('common.status.disabled'),
      ],
      value: String(item.id || ''),
    }))
    .filter((item) => item.value),
);

const mcpRelationOptions = computed(() =>
  mcpEntities.value
    .map((item) => ({
      description:
        item.description || $t('agents.builder.mcpCard.descriptionFallback'),
      label: item.name || $t('agents.builder.mcpCard.untitled'),
      metrics: [
        $t('agents.builder.mcpCard.protocol', {
          value: item.transport || '--',
        }),
      ],
      tags: [
        item.authorized
          ? $t('agents.builder.authorized')
          : $t('agents.builder.pendingAuthorized'),
        'MCP',
      ],
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
        : [...DEFAULT_SUGGESTIONS.value],
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
      source.welcomeMessage ||
      String(meta.welcomeMessage || DEFAULT_WELCOME.value),
  };
}

function buildMetaJson() {
  const merged = {
    ...formModel.value.metaExtras,
    defaultSuggestions: normalizedDefaultSuggestions.value,
    enableAutoSuggestion: Boolean(formModel.value.enableAutoSuggestion),
    icon: String(formModel.value.icon || ''),
    modelType: 'REASONING',
    welcomeMessage: (formModel.value.welcomeMessage || DEFAULT_WELCOME.value).trim(),
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
    const history: LcChatMessage[] = messages
      .filter((item) => {
        const role = String(item.role || '').toLowerCase();
        return role === 'user' || role === 'assistant';
      })
      .map((item) => ({
        content: String(item.message || ''),
        id: String(item.id || item.chatId || `${conversationId}-${Date.now()}`),
        role: String(item.role || '').toLowerCase() as LcChatRole,
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
        formModel.value.welcomeMessage || DEFAULT_WELCOME.value
      ).trim(),
    };

    if (isCreateMode.value) {
      await agentApi.create(payload);
      message.success($t('agents.builder.messages.created'));
      void router.push('/agents');
      return;
    }

    await agentApi.update(agentId.value, payload);
    formModel.value.status = targetStatus;
    message.success($t('agents.builder.messages.saved'));
  } finally {
    saving.value = false;
  }
}

async function handleChatSubmit(payload: LcChatSendPayload) {
  const content = payload.text;
  if (isCreateMode.value || !agentId.value) {
    message.warning($t('agents.builder.messages.saveBeforeDebug'));
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
        attachments: payload.attachments.map((item) => ({
          contentType: item.file.type,
          id: item.ossId,
          name: item.name,
          size: item.size,
          url: item.remoteUrl,
        })),
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
      error instanceof Error
        ? error.message
        : $t('agents.builder.messages.chatRequestFailed');
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
      chat.failTurn(
        assistantId,
        String(nested?.message || $t('agents.builder.messages.generationFailed')),
      );
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
      // 检索完成属于内部流转事件，不作为消息的永久附注展示。
      break;
    }
    case CHAT_STREAM_EVENT.TIMEOUT: {
      chat.failTurn(assistantId, $t('agents.builder.messages.chatTimeout'));
      break;
    }
    case CHAT_STREAM_EVENT.TOOL_BEFORE: {
      const nested = extractNestedEvent(event);
      chat.setTurnMeta(
        assistantId,
        nested?.tool_name
          ? $t('agents.builder.runtime.callingSkill', { name: nested.tool_name })
          : $t('agents.builder.runtime.callingSkillGeneric'),
      );
      break;
    }
    case CHAT_STREAM_EVENT.TOOL_EXECUTED: {
      const nested = extractNestedEvent(event);
      const failed = Boolean(nested?.failed || nested?.status === 'failed');
      chat.setTurnMeta(
        assistantId,
        failed ? $t('agents.builder.runtime.skillFailed') : '',
      );
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
        :status="formModel.status"
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
        <NEmpty :description="$t('agents.builder.loadingDetail')" />
      </div>
    </div>
  </Page>
</template>
