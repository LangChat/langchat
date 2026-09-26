import type { Component } from 'vue';

import type { AttachmentItem } from './composer/types';

/** 消息角色(OpenAI 协议子集,聊天场景只关心 user / assistant)。 */
export type LcChatRole = 'assistant' | 'user';

/** 单条消息的生命周期状态。 */
export type LcChatMessageStatus = 'completed' | 'error' | 'running';

/**
 * 聊天消息:OpenAI message 结构(content / role)之上,
 * 补充展示层字段(id / status / meta / extra)。
 */
export interface LcChatMessage {
  content: string;
  /** 上层扩展数据(经 message-append 插槽透出,如图表结果、事件链) */
  extra?: Record<string, unknown>;
  id: string;
  /** 辅助说明行(如工具执行状态) */
  meta?: string;
  role: LcChatRole;
  status: LcChatMessageStatus;
}

/** 空状态建议问题。 */
export interface LcChatSuggestion {
  icon?: Component;
  label: string;
  /** 点击后回填到输入框的内容(缺省使用 label) */
  value?: string;
}

/** send 事件负载。 */
export interface LcChatSendPayload {
  attachments: AttachmentItem[];
  modelId?: string;
  text: string;
}

/** LcChat 暴露给上层的状态/方法(OpenAI 协议风格的 turn 生命周期)。 */
export interface LcChatApi {
  /** 追加一段流式增量文本 */
  appendDelta: (id: string, chunk: string) => void;
  /** 开启一轮对话:追加用户消息 + 助手占位消息,返回助手消息 ID */
  beginTurn: (userText: string) => string;
  /** 结束一轮对话(可选传入最终全文) */
  completeTurn: (id: string, finalContent?: string) => void;
  /** 标记失败 */
  failTurn: (id: string, errorMessage: string) => void;
  /** 返回可跨组件重建保存的消息快照 */
  getMessages: () => LcChatMessage[];
  /** 只读消息列表 */
  readonly messages: LcChatMessage[];
  /** 平滑滚动到底部 */
  scrollToBottom: () => void;
  /** 重置会话 */
  reset: () => void;
  /** 用一组消息整体替换(历史回填) */
  setMessages: (list: LcChatMessage[]) => void;
  /** 写入扩展数据 */
  setTurnExtra: (id: string, extra: Record<string, unknown>) => void;
  /** 写入辅助说明行 */
  setTurnMeta: (id: string, meta: string) => void;
}

export interface LcChatSidebarItem {
  description?: string;
  id: string;
  meta?: string;
  subtitle?: string;
  title: string;
}

export interface LcChatEventItem {
  description: string;
  id: string;
  meta?: string;
  title: string;
}

export interface LcChatContextSection {
  emptyText?: string;
  items?: Array<{ label: string; value: string }>;
  tags?: string[];
  title: string;
  tone?: 'info' | 'success';
}

export interface LcChatMessageItem {
  content: string;
  id: string;
  meta?: string;
  role: 'assistant' | 'system' | 'tool' | 'user';
}
