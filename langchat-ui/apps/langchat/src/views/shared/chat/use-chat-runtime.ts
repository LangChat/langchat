import type { AigcAgent } from '#/api/aigc/agent';
import type {
  AgentChatStreamEvent,
  AigcConversation,
  AigcMessage,
  KnowledgeReferenceItem,
  OpenAiChatCompletionChunk,
  OpenAiDeltaEventPayload,
  OpenAiChatCompletionResult,
  ToolExecutionPayload,
} from '#/api/aigc/chat';
import type {
  LcChatContextSection,
  LcChatEventItem,
  LcChatMessageItem,
  LcChatSendPayload,
  LcChatSidebarItem,
} from '#/components/LcChat/types';

import { computed, ref, unref } from 'vue';

import { $t, i18n } from '@vben/locales';

import { message as messageApi } from '#/adapter/naive';
import { agentApi } from '#/api/aigc/agent';
import {
  CHAT_ROLE,
  CHAT_STREAM_EVENT,
  createChatCompletionStreamApi,
  listConversationApi,
  listConversationMessagesApi,
} from '#/api/aigc/chat';
import { resolveOptionLabels } from '#/views/shared/aigc/id-list';
import { useAigcLookups } from '#/views/shared/aigc/lookups';
import { formatRelativeTime } from '#/views/shared/aigc/time';

interface UseChatRuntimeOptions {
  enableHistory?: boolean;
}

/** 当前界面语言(时间格式化等需要 locale 感知的场景)。 */
function currentLocale() {
  return unref(i18n.global.locale) || 'zh-CN';
}

export function useChatRuntime(options: UseChatRuntimeOptions = {}) {
  const { enableHistory = false } = options;
  const { loadLookups, lookups } = useAigcLookups({
    knowledges: true,
    models: true,
    skills: true,
  });

  const loading = ref(false);
  const conversationLoading = ref(false);
  const messageLoading = ref(false);
  const sending = ref(false);
  const keyword = ref('');
  const items = ref<AigcAgent[]>([]);
  const conversations = ref<AigcConversation[]>([]);
  const messageRecords = ref<AigcMessage[]>([]);
  const runtimeEventRecords = ref<LcChatEventItem[]>([]);
  const selectedAgentId = ref('');
  const selectedConversationId = ref('');
  const draftMessage = ref('');
  const historyAvailable = ref(enableHistory);
  const streamAbortController = ref<AbortController | null>(null);

  const filteredAgents = computed(() => {
    const query = keyword.value.trim().toLowerCase();
    if (!query) {
      return items.value;
    }
    return items.value.filter((item) =>
      [item.agentName, item.description]
        .map((value) => String(value ?? '').toLowerCase())
        .some((value) => value.includes(query)),
    );
  });

  const selectedAgent = computed(
    () => items.value.find((item) => item.id === selectedAgentId.value) ?? null,
  );

  const agentSidebarItems = computed<LcChatSidebarItem[]>(() =>
    filteredAgents.value.map((item) => ({
      description: item.description || $t('chat.runtime.agentDescriptionFallback'),
      id: item.id ?? '',
      subtitle: item.status || 'DRAFT',
      title: item.agentName || $t('agents.card.unnamed'),
    })),
  );

  const conversationSidebarItems = computed<LcChatSidebarItem[]>(() =>
    conversations.value.map((item) => ({
      id: item.id ?? '',
      meta: item.updateTime ? formatRelativeTime(item.updateTime) : '',
      subtitle: item.shareId
        ? $t('chat.runtime.subtitleShare', { id: item.shareId })
        : $t('chat.runtime.subtitleNormal'),
      title: item.title || $t('chat.conversation.untitled'),
    })),
  );

  const messageItems = computed<LcChatMessageItem[]>(() =>
    messageRecords.value.map((item) => ({
      content: item.message || '',
      id: item.id ?? '',
      meta: item.createTime ? formatRelativeTime(item.createTime) : '',
      role: normalizeRole(item.role),
    })),
  );

  const contextSections = computed<LcChatContextSection[]>(() => {
    if (!selectedAgent.value) {
      return [];
    }
    const knowledgeLabels = resolveOptionLabels(
      lookups.value.knowledges,
      selectedAgent.value.knowledgeIds,
    );
    const skillLabels = resolveOptionLabels(
      lookups.value.skills,
      selectedAgent.value.skillIds,
    );
    let modelConfigText = '--';
    if (selectedAgent.value.modelConfigJson) {
      try {
        modelConfigText = JSON.stringify(
          JSON.parse(selectedAgent.value.modelConfigJson),
          null,
          2,
        );
      } catch {
        modelConfigText = selectedAgent.value.modelConfigJson;
      }
    }
    return [
      {
        items: [
          {
            label: $t('agents.form.reasoningModel'),
            value:
              lookups.value.models.find(
                (item) => item.value === selectedAgent.value?.reasoningModelId,
              )?.label || $t('common.status.notConfigured'),
          },
        ],
        title: $t('chat.runtime.modelConfig'),
      },
      {
        emptyText: $t('chat.runtime.knowledgeEmpty'),
        tags: knowledgeLabels,
        title: $t('chat.runtime.knowledgeSection'),
        tone: 'info',
      },
      {
        emptyText: $t('chat.runtime.skillsEmpty'),
        tags: skillLabels,
        title: $t('chat.runtime.skillsSection'),
        tone: 'success',
      },
      {
        emptyText: $t('chat.runtime.systemPromptEmpty'),
        items: selectedAgent.value.systemPrompt
          ? [
              {
                label: $t('chat.runtime.systemPrompt'),
                value: selectedAgent.value.systemPrompt,
              },
            ]
          : [],
        title: $t('chat.runtime.systemPrompt'),
      },
      {
        emptyText: $t('chat.runtime.paramsEmpty'),
        items: selectedAgent.value.modelConfigJson
          ? [
              {
                label: $t('chat.runtime.paramsLabel'),
                value: modelConfigText,
              },
            ]
          : [],
        title: $t('chat.runtime.paramsOverride'),
      },
    ];
  });

  const runtimeEvents = computed<LcChatEventItem[]>(
    () => runtimeEventRecords.value,
  );

  const conversationSubtitle = computed(() =>
    selectedAgent.value
      ? $t('chat.runtime.contextTitle', {
          name: selectedAgent.value.agentName || $t('agents.card.unnamed'),
        })
      : $t('chat.runtime.selectAgentPrompt'),
  );

  async function loadAgents() {
    loading.value = true;
    try {
      items.value = await agentApi.list();
      if (
        !selectedAgentId.value ||
        !items.value.some((item) => item.id === selectedAgentId.value)
      ) {
        selectedAgentId.value = items.value[0]?.id ?? '';
      }
    } finally {
      loading.value = false;
    }
  }

  async function loadAgentDetail(id: string) {
    if (!id) {
      selectedAgentId.value = '';
      return null;
    }
    loading.value = true;
    try {
      const detail = await agentApi.detail(id);
      const currentIndex = items.value.findIndex((item) => item.id === id);
      if (currentIndex >= 0) {
        items.value[currentIndex] = detail;
      } else {
        items.value.unshift(detail);
      }
      selectedAgentId.value = id;
      return detail;
    } finally {
      loading.value = false;
    }
  }

  async function loadConversations(agentId: string) {
    if (!enableHistory || !agentId) {
      conversations.value = [];
      historyAvailable.value = enableHistory;
      return;
    }
    conversationLoading.value = true;
    try {
      conversations.value = await listConversationApi({
        agentId,
        pageNo: 1,
        pageSize: 20,
      });
      historyAvailable.value = true;
      if (
        !selectedConversationId.value ||
        !conversations.value.some(
          (item) => item.id === selectedConversationId.value,
        )
      ) {
        selectedConversationId.value = conversations.value[0]?.id ?? '';
      }
    } catch {
      conversations.value = [];
      selectedConversationId.value = '';
      historyAvailable.value = false;
    } finally {
      conversationLoading.value = false;
    }
  }

  async function loadMessages(conversationId: string) {
    if (!enableHistory || !conversationId) {
      messageRecords.value = [];
      return;
    }
    messageLoading.value = true;
    try {
      messageRecords.value = await listConversationMessagesApi(conversationId);
      historyAvailable.value = true;
    } catch {
      messageRecords.value = [];
      historyAvailable.value = false;
    } finally {
      messageLoading.value = false;
    }
  }

  async function selectAgent(value: string) {
    selectedAgentId.value = value;
    selectedConversationId.value = '';
    messageRecords.value = [];
    runtimeEventRecords.value = [];
    await loadConversations(value);
  }

  async function selectConversation(value: string) {
    selectedConversationId.value = value;
    await loadMessages(value);
  }

  async function initializeRuntime() {
    await Promise.all([loadLookups(), loadAgents()]);
    if (selectedAgentId.value) {
      await loadConversations(selectedAgentId.value);
      if (selectedConversationId.value) {
        await loadMessages(selectedConversationId.value);
      }
    }
  }

  async function sendMessage(payload?: LcChatSendPayload) {
    const agentId = selectedAgentId.value;
    const content = String(payload?.text ?? draftMessage.value).trim();
    if (!agentId || !content || sending.value) {
      return;
    }

    const attachments = (payload?.attachments || [])
      .filter((item) => item.status === 'uploaded' && item.ossId)
      .map((item) => ({
        contentType: item.file.type,
        id: item.ossId,
        name: item.name,
        size: item.size,
        url: item.remoteUrl,
      }));

    draftMessage.value = '';
    sending.value = true;
    runtimeEventRecords.value = [];
    appendLocalMessage({
      content,
      id: `local-user-${Date.now()}`,
      role: CHAT_ROLE.USER,
    });
    const assistantMessageId = appendLocalMessage({
      content: '',
      id: `local-assistant-${Date.now()}`,
      role: CHAT_ROLE.ASSISTANT,
    });

    streamAbortController.value?.abort();
    const controller = new AbortController();
    streamAbortController.value = controller;

    try {
      await createChatCompletionStreamApi(
        {
          agentId,
          attachments,
          conversationId: selectedConversationId.value || undefined,
          messages: [
            {
              content,
              role: CHAT_ROLE.USER,
            },
          ],
          stream: true,
        },
        {
          onEvent: (event) => {
            applyStreamEvent(event, assistantMessageId);
          },
        },
        controller.signal,
      );
      await syncMessagesAfterStream(agentId);
    } catch (error) {
      const description =
        error instanceof Error
          ? error.message
          : $t('chat.runtime.errors.requestFailed');
      pushRuntimeEvent({
        description,
        id: `event-error-${Date.now()}`,
        title: $t('chat.runtime.errors.notifyTitle'),
      });
      updateLocalAssistantMessage(
        assistantMessageId,
        $t('chat.runtime.errors.notifyDescription', { message: description }),
      );
      messageApi.error($t('chat.runtime.errors.sendFailed'));
    } finally {
      sending.value = false;
      streamAbortController.value = null;
    }
  }

  return {
    agentSidebarItems,
    contextSections,
    conversationLoading,
    conversationSidebarItems,
    conversationSubtitle,
    draftMessage,
    filteredAgents,
    historyAvailable,
    initializeRuntime,
    items,
    keyword,
    loadAgentDetail,
    loadConversations,
    loadLookups,
    loading,
    lookups,
    messageItems,
    messageLoading,
    sending,
    runtimeEvents,
    sendMessage,
    selectAgent,
    selectConversation,
    selectedAgent,
    selectedAgentId,
    selectedConversationId,
  };

  function applyStreamEvent(
    event: AgentChatStreamEvent,
    assistantMessageId: string,
  ) {
    if (event.conversationId) {
      selectedConversationId.value = event.conversationId;
    }
    switch (event.eventType) {
      case CHAT_STREAM_EVENT.MESSAGE_START: {
        pushRuntimeEvent({
          description: $t('chat.runtime.events.startDescription'),
          id: event.id || `event-start-${Date.now()}`,
          meta: formatEventTime(event.created),
          title: $t('chat.runtime.events.startTitle'),
        });
        break;
      }
      case CHAT_STREAM_EVENT.MESSAGE_DELTA: {
        const payload = event.payload as OpenAiChatCompletionChunk | undefined;
        const deltaContent = payload?.choices?.[0]?.delta?.content || '';
        if (deltaContent) {
          appendAssistantChunk(assistantMessageId, deltaContent);
        }
        break;
      }
      case CHAT_STREAM_EVENT.RAG_RETRIEVED: {
        const deltaEvent = parseChunkEvent(event.payload);
        const items = (
          (deltaEvent?.items as KnowledgeReferenceItem[] | undefined) ||
          (event.payload as { items?: KnowledgeReferenceItem[] })?.items ||
          []
        ).filter((item) => item.docsName || item.knowledgeName);
        const hitNames = items
          .slice(0, 3)
          .map(
            (item) =>
              item.docsName ||
              item.knowledgeName ||
              $t('agents.messageLog.untitledSegment'),
          );
        pushRuntimeEvent({
          description:
            hitNames.length > 0
              ? hitNames.join($t('chat.runtime.events.listSeparator'))
              : $t('chat.runtime.events.retrievalNoHits'),
          id: event.id || `event-rag-${Date.now()}`,
          meta: $t('chat.runtime.events.retrievalMeta', {
            count: items.length,
          }),
          title: $t('chat.runtime.events.retrievalTitle'),
        });
        break;
      }
      case CHAT_STREAM_EVENT.TOOL_BEFORE: {
        const deltaEvent = parseChunkEvent(event.payload);
        const payload =
          deltaEvent?.type === 'tool'
            ? ({
                arguments: String(deltaEvent.arguments || ''),
                requestId: String(
                  deltaEvent.request_id || deltaEvent.requestId || '',
                ),
                toolName: String(
                  deltaEvent.tool_name || deltaEvent.toolName || '',
                ),
              } as ToolExecutionPayload)
            : (event.payload as ToolExecutionPayload | undefined);
        pushRuntimeEvent({
          description:
            payload?.arguments || $t('chat.runtime.events.skillStartFallback'),
          id: event.id || `event-tool-before-${Date.now()}`,
          meta: payload?.toolName || $t('skills.empty.unnamed'),
          title: $t('chat.runtime.events.skillStartTitle'),
        });
        break;
      }
      case CHAT_STREAM_EVENT.TOOL_EXECUTED: {
        const deltaEvent = parseChunkEvent(event.payload);
        const payload =
          deltaEvent?.type === 'tool'
            ? ({
                arguments: String(deltaEvent.arguments || ''),
                failed: Boolean(
                  deltaEvent.failed || deltaEvent.status === 'failed',
                ),
                requestId: String(
                  deltaEvent.request_id || deltaEvent.requestId || '',
                ),
                result: String(deltaEvent.result || ''),
                toolName: String(
                  deltaEvent.tool_name || deltaEvent.toolName || '',
                ),
              } as ToolExecutionPayload)
            : (event.payload as ToolExecutionPayload | undefined);
        pushRuntimeEvent({
          description:
            payload?.result || $t('chat.runtime.events.skillEndFallback'),
          id: event.id || `event-tool-after-${Date.now()}`,
          meta: payload?.toolName || $t('skills.empty.unnamed'),
          title: payload?.failed
            ? $t('chat.runtime.events.skillFailedTitle')
            : $t('chat.runtime.events.skillSuccessTitle'),
        });
        break;
      }
      case CHAT_STREAM_EVENT.LOG_DELTA: {
        const payload = parseChunkEvent(event.payload);
        if (!payload || payload.type !== 'log') {
          break;
        }
        pushRuntimeEvent({
          description: String(
            payload.message || $t('chat.runtime.events.logFallback'),
          ),
          id: event.id || `event-log-${Date.now()}`,
          meta: String(payload.phase || ''),
          title: $t('chat.runtime.events.logTitle'),
        });
        break;
      }
      case CHAT_STREAM_EVENT.MESSAGE_COMPLETED: {
        const payload = event.payload as OpenAiChatCompletionResult | undefined;
        const deltaEvent = parseChunkEvent(event.payload);
        const finishReason = String(
          deltaEvent?.finish_reason ||
            payload?.finish_reason ||
            payload?.finishReason ||
            '',
        );
        const usage =
          (deltaEvent?.usage as
            | OpenAiChatCompletionResult['usage']
            | undefined) || payload?.usage;
        pushRuntimeEvent({
          description: finishReason
            ? $t('chat.runtime.events.finishReason', { reason: finishReason })
            : $t('chat.runtime.events.finishDone'),
          id: event.id || `event-completed-${Date.now()}`,
          meta: usage
            ? $t('chat.runtime.events.finishUsage', {
                input: usage.prompt_tokens || usage.promptTokens || 0,
                output: usage.completion_tokens || usage.completionTokens || 0,
              })
            : formatEventTime(event.created),
          title: $t('chat.runtime.events.finishStoredTitle'),
        });
        break;
      }
      case CHAT_STREAM_EVENT.MESSAGE_STOP: {
        pushRuntimeEvent({
          description: $t('chat.runtime.events.streamEndDescription'),
          id: event.id || `event-stop-${Date.now()}`,
          meta: formatEventTime(event.created),
          title: $t('chat.runtime.events.streamEndTitle'),
        });
        break;
      }
      case CHAT_STREAM_EVENT.TIMEOUT: {
        pushRuntimeEvent({
          description: $t('chat.runtime.events.timeoutDescription'),
          id: event.id || `event-timeout-${Date.now()}`,
          meta: formatEventTime(event.created),
          title: $t('chat.runtime.events.timeoutTitle'),
        });
        updateLocalAssistantMessage(
          assistantMessageId,
          $t('chat.runtime.events.timeoutMessage'),
        );
        break;
      }
      case CHAT_STREAM_EVENT.ERROR: {
        const deltaEvent = parseChunkEvent(event.payload);
        const payload =
          deltaEvent?.type === 'error'
            ? { message: String(deltaEvent.message || '') }
            : (event.payload as { message?: string } | undefined);
        pushRuntimeEvent({
          description: payload?.message || $t('chat.runtime.events.errorFallback'),
          id: event.id || `event-runtime-error-${Date.now()}`,
          meta: formatEventTime(event.created),
          title: $t('chat.runtime.events.errorTitle'),
        });
        updateLocalAssistantMessage(
          assistantMessageId,
          payload?.message || $t('chat.runtime.events.failFallback'),
        );
        break;
      }
      case CHAT_STREAM_EVENT.DONE: {
        pushRuntimeEvent({
          description: $t('chat.runtime.events.doneDescription'),
          id: event.id || `event-done-${Date.now()}`,
          meta: formatEventTime(event.created),
          title: $t('chat.runtime.events.doneTitle'),
        });
        break;
      }
      default:
        break;
    }
  }

  function parseChunkEvent(payload: unknown) {
    const chunk = payload as OpenAiChatCompletionChunk | undefined;
    return chunk?.choices?.[0]?.delta?.event as
      | OpenAiDeltaEventPayload
      | undefined;
  }

  async function syncMessagesAfterStream(agentId: string) {
    if (!selectedConversationId.value) {
      return;
    }
    if (enableHistory) {
      await loadConversations(agentId);
      await loadMessages(selectedConversationId.value);
    }
  }

  function appendLocalMessage(input: {
    content: string;
    id: string;
    role: LcChatMessageItem['role'];
  }) {
    messageRecords.value = [
      ...messageRecords.value,
      {
        createTime: Date.now(),
        id: input.id,
        message: input.content,
        role: input.role,
      },
    ];
    return input.id;
  }

  function appendAssistantChunk(messageId: string, chunk: string) {
    messageRecords.value = messageRecords.value.map((item) => {
      if (item.id !== messageId) {
        return item;
      }
      return {
        ...item,
        message: `${item.message || ''}${chunk}`,
      };
    });
  }

  function updateLocalAssistantMessage(messageId: string, content: string) {
    messageRecords.value = messageRecords.value.map((item) => {
      if (item.id !== messageId) {
        return item;
      }
      return {
        ...item,
        message: content,
      };
    });
  }

  function pushRuntimeEvent(event: LcChatEventItem) {
    runtimeEventRecords.value = [event, ...runtimeEventRecords.value].slice(
      0,
      20,
    );
  }

  function formatEventTime(timestamp?: number) {
    if (!timestamp) {
      return '';
    }
    return new Date(timestamp * 1000).toLocaleString(currentLocale(), {
      hour12: false,
    });
  }

  function normalizeRole(role?: string): LcChatMessageItem['role'] {
    const value = String(role || '').toLowerCase();
    switch (value) {
      case CHAT_ROLE.SYSTEM:
        return CHAT_ROLE.SYSTEM;
      case CHAT_ROLE.TOOL:
        return CHAT_ROLE.TOOL;
      case CHAT_ROLE.USER:
        return CHAT_ROLE.USER;
      case CHAT_ROLE.ASSISTANT:
      default:
        return CHAT_ROLE.ASSISTANT;
    }
  }
}
