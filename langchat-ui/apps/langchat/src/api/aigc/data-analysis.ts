import type {
  AgentChatStreamEvent,
  OpenAiChatCompletionChunk,
} from './chat';

import { $t } from '@vben/locales';

import {
  getRequestClientAuthHeaders,
  requestClient,
  resolveRequestClientUrl,
} from '#/api/request';

export interface DataAnalysisTableItem {
  columnCount: number;
  comment?: string;
  name: string;
  sourceName: string;
}

export interface DataAnalysisCatalogItem {
  datasourceId: string;
  datasourceName: string;
  dbType: string;
  tables: DataAnalysisTableItem[];
}

export interface DataAnalysisRequest {
  conversationId?: string;
  datasourceId: string;
  modelId?: string;
  question: string;
  tableNames: string[];
}

export function listDataAnalysisCatalog() {
  return requestClient.get<DataAnalysisCatalogItem[]>(
    '/v1/core/data-analysis/catalog',
  );
}

export async function streamDataAnalysis(
  payload: DataAnalysisRequest,
  onEvent: (event: AgentChatStreamEvent) => void,
  signal?: AbortSignal,
) {
  const response = await fetch(
    resolveRequestClientUrl('/v1/core/data-analysis/chat/stream'),
    {
      body: JSON.stringify(payload),
      headers: {
        ...getRequestClientAuthHeaders(),
        'Content-Type': 'application/json',
      },
      method: 'POST',
      signal,
    },
  );
  if (!response.ok) {
    throw new Error(
      (await response.text()) || $t('dataAnalysis.messages.apiRequestFailed'),
    );
  }
  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error($t('dataAnalysis.messages.apiStreamEmpty'));
  }
  const decoder = new TextDecoder();
  let buffer = '';
  while (true) {
    const { done, value } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });
    const blocks = buffer.split('\n\n');
    buffer = blocks.pop() || '';
    for (const block of blocks) {
      const event = parseAnalysisStreamBlock(block);
      if (event) onEvent(event);
    }
    if (done) break;
  }
  if (buffer.trim()) {
    const event = parseAnalysisStreamBlock(buffer);
    if (event) onEvent(event);
  }
}

function parseAnalysisStreamBlock(block: string): AgentChatStreamEvent | null {
  const dataText = block
    .split(/\r?\n/u)
    .map((line) => line.trim())
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trim())
    .join('');
  if (!dataText) return null;
  if (dataText === '[DONE]') {
    return { created: Date.now(), eventType: 'done', payload: { marker: '[DONE]' } };
  }
  const chunk = JSON.parse(dataText) as OpenAiChatCompletionChunk;
  const choice = chunk.choices?.[0];
  const delta = choice?.delta;
  const eventType = String(
    delta?.event?.name ||
      (delta?.content
        ? 'message.delta'
        : delta?.role
          ? 'message.start'
          : choice?.finish_reason || choice?.finishReason
            ? 'message.stop'
            : 'message.delta'),
  );
  return {
    completionId: chunk.id,
    created: chunk.created,
    eventType,
    id: `${chunk.id || 'analysis'}:${eventType}:${Date.now()}`,
    payload: chunk,
  };
}
