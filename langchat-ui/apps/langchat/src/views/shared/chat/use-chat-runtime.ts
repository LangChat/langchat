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

import { computed, ref } from 'vue';

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

interface UseChatRuntimeOptions {
  enableHistory?: boolean;
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
      description: item.description || '当前未配置智能体描述。',
      id: item.id ?? '',
      subtitle: item.status || 'DRAFT',
      title: item.agentName || '未命名智能体',
    })),
  );

  const conversationSidebarItems = computed<LcChatSidebarItem[]>(() =>
    conversations.value.map((item) => ({
      id: item.id ?? '',
      meta: item.updateTime
        ? new Date(item.updateTime).toLocaleString('zh-CN', { hour12: false })
        : '',
      subtitle: item.shareId ? `分享标识 ${item.shareId}` : '普通会话',
      title: item.title || '未命名会话',
    })),
  );

  const messageItems = computed<LcChatMessageItem[]>(() =>
    messageRecords.value.map((item) => ({
      content: item.message || '',
      id: item.id ?? '',
      meta: item.createTime
        ? new Date(item.createTime).toLocaleString('zh-CN', { hour12: false })
        : '',
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
            label: '推理模型',
            value:
              lookups.value.models.find(
                (item) => item.value === selectedAgent.value?.reasoningModelId,
              )?.label || '未配置',
          },
        ],
        title: '模型配置',
      },
      {
        emptyText: '当前未配置知识库',
        tags: knowledgeLabels,
        title: '关联知识库',
        tone: 'info',
      },
      {
        emptyText: '当前未配置技能',
        tags: skillLabels,
        title: '关联技能',
        tone: 'success',
      },
      {
        emptyText: '当前未配置系统提示词',
        items: selectedAgent.value.systemPrompt
          ? [{ label: '系统提示词', value: selectedAgent.value.systemPrompt }]
          : [],
        title: '系统提示词',
      },
      {
        emptyText: '当前未配置模型参数',
        items: selectedAgent.value.modelConfigJson
          ? [{ label: '模型参数 JSON', value: modelConfigText }]
          : [],
        title: '参数覆盖',
      },
    ];
  });

  const runtimeEvents = computed<LcChatEventItem[]>(
    () => runtimeEventRecords.value,
  );

  const conversationSubtitle = computed(() =>
    selectedAgent.value
      ? `${selectedAgent.value.agentName || '未命名智能体'} 的对话上下文`
      : '请选择一个智能体开始对话',
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
        error instanceof Error ? error.message : '聊天请求失败';
      pushRuntimeEvent({
        description,
        id: `event-error-${Date.now()}`,
        title: '对话请求失败',
      });
      updateLocalAssistantMessage(
        assistantMessageId,
        `请求失败：${description}`,
      );
      messageApi.error('发送消息失败');
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
          description: '模型已开始生成回复内容。',
          id: event.id || `event-start-${Date.now()}`,
          meta: formatEventTime(event.created),
          title: '开始回复',
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
        pushRuntimeEvent({
          description:
            items.length > 0
              ? items
                  .slice(0, 3)
                  .map(
                    (item) =>
                      item.docsName || item.knowledgeName || '未命名片段',
                  )
                  .join('、')
              : '本次未返回可展示的检索命中。',
          id: event.id || `event-rag-${Date.now()}`,
          meta: `命中 ${items.length} 条`,
          title: '知识库检索完成',
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
          description: payload?.arguments || '当前技能未返回入参内容。',
          id: event.id || `event-tool-before-${Date.now()}`,
          meta: payload?.toolName || '未命名技能',
          title: '开始调用技能',
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
          description: payload?.result || '当前技能未返回执行结果。',
          id: event.id || `event-tool-after-${Date.now()}`,
          meta: payload?.toolName || '未命名技能',
          title: payload?.failed ? '技能执行失败' : '技能执行完成',
        });
        break;
      }
      case CHAT_STREAM_EVENT.LOG_DELTA: {
        const payload = parseChunkEvent(event.payload);
        if (!payload || payload.type !== 'log') {
          break;
        }
        pushRuntimeEvent({
          description: String(payload.message || '收到执行日志事件。'),
          id: event.id || `event-log-${Date.now()}`,
          meta: String(payload.phase || ''),
          title: '链路日志',
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
            ? `完成原因：${finishReason}`
            : '模型响应已完成。',
          id: event.id || `event-completed-${Date.now()}`,
          meta: usage
            ? `输入 ${usage.prompt_tokens || usage.promptTokens || 0} / 输出 ${usage.completion_tokens || usage.completionTokens || 0}`
            : formatEventTime(event.created),
          title: '回复已落库',
        });
        break;
      }
      case CHAT_STREAM_EVENT.MESSAGE_STOP: {
        pushRuntimeEvent({
          description: '本次流式输出已结束。',
          id: event.id || `event-stop-${Date.now()}`,
          meta: formatEventTime(event.created),
          title: '流式输出结束',
        });
        break;
      }
      case CHAT_STREAM_EVENT.TIMEOUT: {
        pushRuntimeEvent({
          description: '服务端流式响应超时。',
          id: event.id || `event-timeout-${Date.now()}`,
          meta: formatEventTime(event.created),
          title: '对话超时',
        });
        updateLocalAssistantMessage(
          assistantMessageId,
          '对话超时，请稍后重试。',
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
          description: payload?.message || '服务端返回了异常事件。',
          id: event.id || `event-runtime-error-${Date.now()}`,
          meta: formatEventTime(event.created),
          title: '运行时异常',
        });
        updateLocalAssistantMessage(
          assistantMessageId,
          payload?.message || '对话生成失败。',
        );
        break;
      }
      case CHAT_STREAM_EVENT.DONE: {
        pushRuntimeEvent({
          description: '已收到本次事件流结束标记。',
          id: event.id || `event-done-${Date.now()}`,
          meta: formatEventTime(event.created),
          title: '事件流完成',
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
    return new Date(timestamp * 1000).toLocaleString('zh-CN', {
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
