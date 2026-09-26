import type {
  AgentChatStreamEvent,
  KnowledgeReferenceItem,
  OpenAiChatCompletionChunk,
  OpenAiDeltaEventPayload,
  ToolExecutionPayload,
} from '#/api/aigc/chat';
import type {
  AgentChatChainContext,
  AgentChatChainPatch,
  AgentChatParserResult,
  ToolRunStatus,
} from './types';

import { CHAT_STREAM_EVENT as CHAT_EVENT } from '#/api/aigc/chat';

function now() {
  return Date.now();
}

function normalizeEventTime(created?: number) {
  if (!created) {
    return now();
  }
  if (created > 10_000_000_000) {
    return created;
  }
  return created * 1000;
}

function resolveToolStatus(payload?: ToolExecutionPayload): ToolRunStatus {
  if (payload?.failed) {
    return 'failed';
  }
  return 'completed';
}

function resolveToolId(
  event: AgentChatStreamEvent,
  payload?: ToolExecutionPayload,
) {
  return String(
    payload?.request_id || payload?.requestId || event.id || `tool-${now()}`,
  );
}

function ensureAssistantMessage(
  context: AgentChatChainContext,
  time: number,
): AgentChatChainPatch[] {
  if (context.assistantMessageId) {
    return [];
  }
  const messageId = `assistant-${now()}`;
  context.assistantMessageId = messageId;
  return [
    {
      entry: {
        content: '',
        id: messageId,
        role: 'assistant',
        time,
        type: 'message',
      },
      op: 'append',
    },
  ];
}

function parseDeltaText(payload: unknown) {
  const chunk = payload as OpenAiChatCompletionChunk | undefined;
  return String(chunk?.choices?.[0]?.delta?.content || '');
}

function parseEventPayload(payload: unknown) {
  const chunk = payload as OpenAiChatCompletionChunk | undefined;
  const delta = chunk?.choices?.[0]?.delta;
  const event = delta?.event as OpenAiDeltaEventPayload | undefined;
  return event;
}

function parseRagItems(payload: unknown) {
  const event = parseEventPayload(payload);
  const itemsFromEvent = (event?.items || []).filter(Boolean);
  if (itemsFromEvent.length > 0) {
    return itemsFromEvent;
  }
  const items = (
    (payload as { items?: KnowledgeReferenceItem[] } | undefined)?.items || []
  ).filter(Boolean);
  return items;
}

function parseToolPayload(payload: unknown): ToolExecutionPayload {
  const event = parseEventPayload(payload);
  if (!event) {
    return (payload as ToolExecutionPayload | undefined) || {};
  }
  if (event.type === 'tool' || String(event.name || '').startsWith('tool.')) {
    return {
      arguments: String(event.arguments || ''),
      failed: Boolean(event.failed || event.status === 'failed'),
      requestId: String(event.request_id || event.requestId || ''),
      result: String(event.result || ''),
      toolName: String(event.tool_name || event.toolName || ''),
    };
  }
  return (payload as ToolExecutionPayload | undefined) || {};
}

function parseErrorMessage(payload: unknown) {
  const event = parseEventPayload(payload);
  if (
    event &&
    (event.type === 'error' || String(event.name || '').includes('error'))
  ) {
    return String(event.message || '');
  }
  return String((payload as { message?: string } | undefined)?.message || '');
}

export function createAgentChatChainContext(): AgentChatChainContext {
  return {
    assistantMessageId: undefined,
    lastStageId: undefined,
    toolRequestMap: {},
  };
}

export function parseAgentChatStreamEvent(
  event: AgentChatStreamEvent,
  context: AgentChatChainContext,
): AgentChatParserResult {
  const patches: AgentChatChainPatch[] = [];
  const eventType = String(event.eventType || '');
  const time = normalizeEventTime(event.created);

  switch (eventType) {
    case CHAT_EVENT.MESSAGE_START: {
      patches.push(...ensureAssistantMessage(context, time));
      break;
    }
    case CHAT_EVENT.MESSAGE_DELTA: {
      patches.push(...ensureAssistantMessage(context, time));
      const delta = parseDeltaText(event.payload);
      if (delta && context.assistantMessageId) {
        patches.push({
          delta,
          id: context.assistantMessageId,
          op: 'message.delta',
        });
      }
      break;
    }
    case CHAT_EVENT.RAG_RETRIEVED: {
      const items = parseRagItems(event.payload);
      patches.push({
        entry: {
          id: String(event.id || `rag-${now()}`),
          items,
          time,
          type: 'rag',
        },
        op: 'append',
      });
      break;
    }
    case CHAT_EVENT.TOOL_BEFORE: {
      const payload = parseToolPayload(event.payload);
      const eventId = resolveToolId(event, payload);
      if (payload?.requestId) {
        context.toolRequestMap[String(payload.requestId)] = eventId;
      }
      patches.push({
        entry: {
          argumentsText: payload?.arguments,
          id: eventId,
          relatedMessageId: context.assistantMessageId,
          requestId: payload?.requestId,
          status: 'running',
          time,
          toolName: payload?.toolName || '未命名工具',
          type: 'tool',
        },
        op: 'append',
      });
      break;
    }
    case CHAT_EVENT.TOOL_EXECUTED: {
      const payload = parseToolPayload(event.payload);
      const lookupId = payload?.requestId
        ? context.toolRequestMap[String(payload.requestId)]
        : '';
      const eventId = lookupId || resolveToolId(event, payload);
      patches.push({
        entry: {
          argumentsText: payload?.arguments,
          errorText: payload?.failed ? payload?.result : '',
          id: eventId,
          relatedMessageId: context.assistantMessageId,
          requestId: payload?.requestId,
          resultText: payload?.failed ? '' : payload?.result,
          status: resolveToolStatus(payload),
          time,
          toolName: payload?.toolName || '未命名工具',
          type: 'tool',
        },
        id: eventId,
        op: 'replace',
      });
      break;
    }
    case CHAT_EVENT.LOG_DELTA: {
      break;
    }
    case CHAT_EVENT.MESSAGE_COMPLETED: {
      break;
    }
    case CHAT_EVENT.MESSAGE_STOP: {
      break;
    }
    case CHAT_EVENT.ERROR: {
      const errorMessage = parseErrorMessage(event.payload) || '对话执行异常';
      patches.push(...ensureAssistantMessage(context, time));
      if (context.assistantMessageId) {
        patches.push({
          delta: `\n${errorMessage}`,
          id: context.assistantMessageId,
          op: 'message.delta',
        });
      }
      break;
    }
    case CHAT_EVENT.TIMEOUT: {
      patches.push(...ensureAssistantMessage(context, time));
      if (context.assistantMessageId) {
        patches.push({
          delta: '\n对话响应超时，请稍后重试。',
          id: context.assistantMessageId,
          op: 'message.delta',
        });
      }
      break;
    }
    case CHAT_EVENT.DONE: {
      return {
        patches,
        terminate: true,
      };
    }
    default:
      break;
  }

  return { patches };
}
