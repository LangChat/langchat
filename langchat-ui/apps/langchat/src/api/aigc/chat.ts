import type { BaseEntity } from './_shared';

import {
  getRequestClientAuthHeaders,
  requestClient,
  resolveRequestClientUrl,
} from '#/api/request';

export const CHAT_ROLE = {
  ASSISTANT: 'assistant',
  SYSTEM: 'system',
  TOOL: 'tool',
  USER: 'user',
} as const;

export const CHAT_STREAM_EVENT = {
  ANALYSIS_ECHART: 'analysis.echart',
  ANALYSIS_TABLE: 'analysis.table',
  DONE: 'done',
  ERROR: 'error',
  LOG_DELTA: 'log.delta',
  MESSAGE_COMPLETED: 'message.completed',
  MESSAGE_DELTA: 'message.delta',
  MESSAGE_START: 'message.start',
  MESSAGE_STOP: 'message.stop',
  RAG_RETRIEVED: 'rag.retrieved',
  TIMEOUT: 'timeout',
  TOOL_BEFORE: 'tool.before',
  TOOL_EXECUTED: 'tool.executed',
} as const;

/**
 * 会话实体。
 */
export interface AigcConversation extends BaseEntity {
  agentId?: string;
  shareId?: string;
  title?: string;
}

/**
 * 消息实体。
 */
export interface AigcMessage extends BaseEntity {
  agentId?: string;
  attachments?: string;
  chatId?: string;
  conversationId?: string;
  duration?: number;
  finishReason?: string;
  inputToken?: number;
  likes?: boolean;
  message?: string;
  model?: string;
  outputToken?: number;
  role?: string;
  traceInfo?: string;
  type?: string;
}

export interface AigcMessageEvent extends BaseEntity {
  agentId?: string;
  chatId?: string;
  conversationId?: string;
  eventIndex?: number;
  eventName?: string;
  eventStatus?: string;
  eventType?: string;
  messageId?: string;
  payloadJson?: string;
}

/**
 * 对话消息输入。
 */
export interface ChatCompletionMessage {
  content?: string;
  name?: string;
  role:
    | typeof CHAT_ROLE.ASSISTANT
    | typeof CHAT_ROLE.SYSTEM
    | typeof CHAT_ROLE.TOOL
    | typeof CHAT_ROLE.USER;
}

export interface ChatAttachment {
  contentType?: string;
  id?: string;
  name?: string;
  size?: number;
  url?: string;
}

/**
 * 对话完成请求。
 */
export interface ChatCompletionRequest {
  agentId?: string;
  attachments?: ChatAttachment[];
  conversationId?: string;
  messages: ChatCompletionMessage[];
  model?: string;
  stream?: boolean;
  temperature?: number;
  topP?: number;
}

/**
 * 会话列表查询。
 */
export interface ConversationQuery {
  agentId?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface CreateConversationPayload {
  agentId: string;
  title?: string;
}

export interface MessageEventQuery {
  chatId?: string;
  conversationId?: string;
  messageId?: string;
}

/**
 * 流式事件对象。
 */
export interface AgentChatStreamEvent {
  completionId?: string;
  conversationId?: string;
  created?: number;
  eventType?: string;
  id?: string;
  payload?: OpenAiChatCompletionChunk | { marker: '[DONE]' };
}

export interface OpenAiDeltaEventPayload {
  chart_type?: string;
  code?: string;
  columns?: string[];
  detail?: Record<string, unknown>;
  failed?: boolean;
  finish_reason?: string;
  items?: KnowledgeReferenceItem[];
  message?: string;
  message_id?: string;
  name?: string;
  option?: Record<string, unknown>;
  phase?: string;
  request_id?: string;
  requestId?: string;
  result?: string;
  row_count?: number;
  rows?: Record<string, unknown>[];
  sql?: string;
  status?: string;
  tool_name?: string;
  toolName?: string;
  title?: string;
  type?: 'error' | 'log' | 'rag' | 'tool' | string;
  [key: string]: unknown;
}

/**
 * OpenAI 风格增量对象。
 */
export interface OpenAiChatCompletionDelta {
  content?: string;
  event?: OpenAiDeltaEventPayload;
  role?: string;
}

/**
 * OpenAI 风格选项对象。
 */
export interface OpenAiChatCompletionChoice {
  delta?: OpenAiChatCompletionDelta;
  finish_reason?: null | string;
  finishReason?: string;
  index?: number;
}

/**
 * OpenAI 风格流式分片。
 */
export interface OpenAiChatCompletionChunk {
  choices?: OpenAiChatCompletionChoice[];
  created?: number;
  id?: string;
  model?: string;
  object?: string;
}

/**
 * OpenAI 风格完成结果。
 */
export interface OpenAiChatCompletionResult {
  conversation_id?: string;
  conversationId?: string;
  finish_reason?: string;
  finishReason?: string;
  id?: string;
  message_id?: string;
  messageId?: string;
  model?: string;
  usage?: {
    completion_tokens?: number;
    completionTokens?: number;
    prompt_tokens?: number;
    promptTokens?: number;
    total_tokens?: number;
    totalTokens?: number;
  };
}

/**
 * 知识检索条目。
 */
export interface KnowledgeReferenceItem {
  content?: string;
  docsId?: string;
  docsName?: string;
  knowledgeId?: string;
  knowledgeName?: string;
  score?: number;
  segmentId?: string;
}

/**
 * Tool 执行结果。
 */
export interface ToolExecutionPayload {
  arguments?: string;
  failed?: boolean;
  request_id?: string;
  requestId?: string;
  result?: string;
  tool_name?: string;
  toolName?: string;
}

interface StreamHandlers {
  onEvent?: (event: AgentChatStreamEvent) => void;
}

/**
 * 获取会话列表。
 */
export function listConversationApi(params?: ConversationQuery) {
  return requestClient.get<AigcConversation[]>('/v1/chat/conversations', {
    params,
  });
}

/**
 * 创建会话。
 */
export function createConversationApi(payload: CreateConversationPayload) {
  return requestClient.post<AigcConversation>('/v1/chat/conversations', payload);
}

/**
 * 获取会话消息列表。
 */
export function listConversationMessagesApi(conversationId: string) {
  return requestClient.get<AigcMessage[]>(
    `/v1/chat/conversations/${conversationId}/messages`,
  );
}

export function updateConversationApi(
  conversationId: string,
  payload: Partial<AigcConversation>,
) {
  return requestClient.put<boolean>(
    `/v1/chat/conversations/${conversationId}`,
    payload,
  );
}

export function removeConversationApi(conversationId: string) {
  return requestClient.delete<boolean>(`/v1/chat/conversations/${conversationId}`);
}

export function listMessageEventsApi(params?: MessageEventQuery) {
  return requestClient.get<AigcMessageEvent[]>('/v1/aigc/message-events', {
    params,
  });
}

/**
 * 提交一次对话请求。
 */
export function createChatCompletionApi(payload: ChatCompletionRequest) {
  return requestClient.post('/v1/chat/completions', payload);
}

/**
 * 以 SSE 方式发起对话流请求。
 */
export async function createChatCompletionStreamApi(
  payload: ChatCompletionRequest,
  handlers: StreamHandlers = {},
  signal?: AbortSignal,
) {
  const response = await fetch(
    resolveRequestClientUrl('/v1/chat/completions'),
    {
      body: JSON.stringify({
        ...payload,
        stream: true,
      }),
      headers: {
        ...getRequestClientAuthHeaders(),
        'Content-Type': 'application/json',
      },
      method: 'POST',
      signal,
    },
  );

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `聊天请求失败，状态码 ${response.status}`);
  }

  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error('聊天响应流为空');
  }

  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), {
      stream: !done,
    });
    const blocks = buffer.split('\n\n');
    buffer = blocks.pop() || '';

    for (const block of blocks) {
      const parsedEvent = parseStreamEventBlock(block);
      if (parsedEvent) {
        handlers.onEvent?.(parsedEvent);
      }
    }

    if (done) {
      break;
    }
  }

  if (buffer.trim()) {
    const parsedEvent = parseStreamEventBlock(buffer);
    if (parsedEvent) {
      handlers.onEvent?.(parsedEvent);
    }
  }
}

function parseStreamEventBlock(block: string) {
  const lines = block
    .split(/\r?\n/u)
    .map((line) => line.trim())
    .filter(Boolean);
  let eventName = '';
  let dataText = '';

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim();
    }
    if (line.startsWith('data:')) {
      dataText += line.slice(5).trim();
    }
  }

  if (!dataText) {
    return null;
  }
  if (dataText === '[DONE]') {
    return {
      created: Date.now(),
      eventType: CHAT_STREAM_EVENT.DONE,
      payload: {
        marker: '[DONE]',
      },
    } as AgentChatStreamEvent;
  }

  const payload = JSON.parse(dataText) as OpenAiChatCompletionChunk;
  return toStreamEvent(payload, eventName);
}

function toStreamEvent(
  payload: OpenAiChatCompletionChunk,
  sseEventName?: string,
): AgentChatStreamEvent {
  const delta = payload.choices?.[0]?.delta;
  const deltaEvent = delta?.event;
  const inferredEventType = inferEventType(payload, sseEventName);
  const detail = deltaEvent?.detail || {};
  const detailConversationId = String(
    (detail as Record<string, unknown>).conversation_id ||
      (detail as Record<string, unknown>).conversationId ||
      '',
  );
  return {
    completionId: payload.id,
    conversationId: detailConversationId || undefined,
    created: payload.created,
    eventType: inferredEventType,
    id: buildStreamEventId(payload, inferredEventType, deltaEvent),
    payload,
  };
}

function inferEventType(
  payload: OpenAiChatCompletionChunk,
  sseEventName?: string,
) {
  if (sseEventName) {
    return sseEventName;
  }
  const choice = payload.choices?.[0];
  const delta = choice?.delta;
  const deltaEvent = delta?.event;
  const eventName = String(deltaEvent?.name || '');
  if (eventName) {
    return eventName;
  }
  if (delta?.content) {
    return CHAT_STREAM_EVENT.MESSAGE_DELTA;
  }
  if (delta?.role === CHAT_ROLE.ASSISTANT) {
    return CHAT_STREAM_EVENT.MESSAGE_START;
  }
  if (choice?.finish_reason || choice?.finishReason) {
    return CHAT_STREAM_EVENT.MESSAGE_STOP;
  }
  return CHAT_STREAM_EVENT.MESSAGE_DELTA;
}

function buildStreamEventId(
  payload: OpenAiChatCompletionChunk,
  eventType: string,
  deltaEvent?: OpenAiDeltaEventPayload,
) {
  const suffix = [
    String(deltaEvent?.request_id || deltaEvent?.requestId || ''),
    String(deltaEvent?.message_id || ''),
    String(deltaEvent?.phase || ''),
    String(deltaEvent?.tool_name || deltaEvent?.toolName || ''),
    String(payload.choices?.[0]?.delta?.content || '').slice(0, 16),
    String(
      payload.choices?.[0]?.finish_reason ||
        payload.choices?.[0]?.finishReason ||
        '',
    ),
    String(payload.created || ''),
  ]
    .filter(Boolean)
    .join(':');
  return [
    String(payload.id || 'chunk'),
    eventType,
    suffix || String(Math.random()).slice(2, 8),
  ].join(':');
}
