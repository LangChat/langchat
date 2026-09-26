import type {
  AgentChatStreamEvent,
  ChatCompletionRequest,
  KnowledgeReferenceItem,
  ToolExecutionPayload,
} from '#/api/aigc/chat';

export type AgentMessageRole = 'assistant' | 'user';
export type ToolRunStatus = 'completed' | 'failed' | 'running' | 'started';
export type StageStatus = 'error' | 'running' | 'success';
export type AgentChatEntryType = 'message' | 'rag' | 'stage' | 'tool';

export interface AgentChatMessageEvent {
  content: string;
  id: string;
  role: AgentMessageRole;
  time?: number;
  type: 'message';
}

export interface AgentChatToolEvent {
  argumentsText?: string;
  errorText?: string;
  id: string;
  relatedMessageId?: string;
  requestId?: string;
  resultText?: string;
  status: ToolRunStatus;
  time?: number;
  toolName: string;
  type: 'tool';
}

export interface AgentChatRagEvent {
  id: string;
  items: KnowledgeReferenceItem[];
  time?: number;
  type: 'rag';
}

export interface AgentChatStageEvent {
  id: string;
  status: StageStatus;
  text: string;
  time?: number;
  type: 'stage';
}

export type AgentChatEventEntry =
  | AgentChatMessageEvent
  | AgentChatRagEvent
  | AgentChatStageEvent
  | AgentChatToolEvent;

export interface AgentChatEventContext {
  assistantMessageId: string;
}

export interface AgentChatStreamClientOptions {
  onEvent: (event: AgentChatStreamEvent) => void;
  signal?: AbortSignal;
}

export type AgentChatStreamRequest = ChatCompletionRequest;

export type AgentToolPayload = ToolExecutionPayload;

export interface AgentChatChainContext {
  assistantMessageId?: string;
  lastStageId?: string;
  toolRequestMap: Record<string, string>;
}

export interface AgentChatAppendPatch {
  entry: AgentChatEventEntry;
  op: 'append';
}

export interface AgentChatReplacePatch {
  entry: AgentChatEventEntry;
  id: string;
  op: 'replace';
}

export interface AgentChatMessageDeltaPatch {
  delta: string;
  id: string;
  op: 'message.delta';
}

export type AgentChatChainPatch =
  | AgentChatAppendPatch
  | AgentChatMessageDeltaPatch
  | AgentChatReplacePatch;

export interface AgentChatParserResult {
  patches: AgentChatChainPatch[];
  terminate?: boolean;
}
