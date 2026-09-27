import type {
  AgentChatStreamEvent,
  OpenAiChatCompletionChunk,
  OpenAiDeltaEventPayload,
} from '#/api/aigc/chat';

import { $t } from '@vben/locales';

export interface AnalysisTableResult {
  columns: string[];
  rowCount: number;
  rows: Record<string, unknown>[];
  sql?: string;
  title: string;
}

export interface AnalysisChartResult {
  chartType: string;
  option: Record<string, unknown>;
  title: string;
}

export type ParsedAnalysisEvent =
  | { content: string; type: 'message.delta' }
  | { message: string; type: 'error' }
  | { name: string; status: string; type: 'tool' }
  | { result: AnalysisChartResult; type: 'chart' }
  | { result: AnalysisTableResult; type: 'table' }
  | { type: 'done' }
  | { type: 'ignore' };

export function parseDataAnalysisEvent(
  streamEvent: AgentChatStreamEvent,
): ParsedAnalysisEvent {
  if (streamEvent.eventType === 'done') return { type: 'done' };
  const chunk = streamEvent.payload as OpenAiChatCompletionChunk | undefined;
  const delta = chunk?.choices?.[0]?.delta;
  const event = delta?.event as OpenAiDeltaEventPayload | undefined;
  if (streamEvent.eventType === 'message.delta' && delta?.content) {
    return { content: delta.content, type: 'message.delta' };
  }
  if (streamEvent.eventType === 'analysis.table' && event) {
    return {
      result: {
        columns: toStringArray(event.columns),
        rowCount: Number(event.row_count || 0),
        rows: Array.isArray(event.rows)
          ? (event.rows as Record<string, unknown>[])
          : [],
        sql: String(event.sql || ''),
        title: String(event.title || $t('dataAnalysis.events.queryResult')),
      },
      type: 'table',
    };
  }
  if (streamEvent.eventType === 'analysis.echart' && event) {
    const option = sanitizeEchartOption(event.option);
    if (!option) {
      return { message: $t('dataAnalysis.events.invalidChartConfig'), type: 'error' };
    }
    return {
      result: {
        chartType: String(event.chart_type || 'auto'),
        option,
        title: String(event.title || $t('dataAnalysis.events.chartTitle')),
      },
      type: 'chart',
    };
  }
  if (streamEvent.eventType === 'tool.before' || streamEvent.eventType === 'tool.executed') {
    return {
      name: String(event?.tool_name || ''),
      status: String(event?.status || ''),
      type: 'tool',
    };
  }
  if (streamEvent.eventType === 'error' || streamEvent.eventType === 'timeout') {
    return {
      message: String(event?.message || $t('dataAnalysis.messages.failed')),
      type: 'error',
    };
  }
  return { type: 'ignore' };
}

export function sanitizeEchartOption(
  value: unknown,
): null | Record<string, unknown> {
  if (!isPlainObject(value)) return null;
  try {
    const serialized = JSON.stringify(value, (key, item) => {
      if (key === '__proto__' || key === 'constructor' || key === 'prototype') {
        return undefined;
      }
      return typeof item === 'function' ? undefined : item;
    });
    const parsed = JSON.parse(serialized) as Record<string, unknown>;
    return Array.isArray(parsed.series) && parsed.series.length > 0
      ? parsed
      : null;
  } catch {
    return null;
  }
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function toStringArray(value: unknown) {
  return Array.isArray(value) ? value.map((item) => String(item)) : [];
}
