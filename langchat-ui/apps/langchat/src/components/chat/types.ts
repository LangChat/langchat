import type { Component } from 'vue';

/** 消息角色(OpenAI 协议子集,聊天场景只关心 user / assistant)。 */
export type ChatRole = 'assistant' | 'user';

/** 单条消息的生命周期状态。 */
export type ChatMessageStatus = 'completed' | 'error' | 'running';

/**
 * 聊天消息:OpenAI message 结构(content / role)之上,
 * 补充展示层字段(id / status / meta / extra)。
 */
export interface ChatMessage {
  content: string;
  /** 上层扩展数据(经 message-append 插槽透出,如图表结果、事件链) */
  extra?: Record<string, unknown>;
  id: string;
  /** 辅助说明行(如工具执行状态) */
  meta?: string;
  role: ChatRole;
  status: ChatMessageStatus;
}

/** 空状态建议问题。 */
export interface ChatSuggestion {
  icon?: Component;
  label: string;
  /** 点击后回填到输入框的内容(缺省使用 label) */
  value?: string;
}

/** send 事件负载。 */
export interface ChatSendPayload {
  modelId?: string;
  text: string;
}

/** ChatLayout 暴露给上层的状态/方法(OpenAI 协议风格的 turn 生命周期)。 */
export interface ChatLayoutApi {
  /** 追加一段流式增量文本 */
  appendDelta: (id: string, chunk: string) => void;
  /** 开启一轮对话:追加用户消息 + 助手占位消息,返回助手消息 ID */
  beginTurn: (userText: string) => string;
  /** 结束一轮对话(可选传入最终全文) */
  completeTurn: (id: string, finalContent?: string) => void;
  /** 标记失败 */
  failTurn: (id: string, errorMessage: string) => void;
  /** 只读消息列表 */
  readonly messages: ChatMessage[];
  /** 平滑滚动到底部 */
  scrollToBottom: () => void;
  /** 重置会话 */
  reset: () => void;
  /** 用一组消息整体替换(历史回填) */
  setMessages: (list: ChatMessage[]) => void;
  /** 写入扩展数据 */
  setTurnExtra: (id: string, extra: Record<string, unknown>) => void;
  /** 写入辅助说明行 */
  setTurnMeta: (id: string, meta: string) => void;
}
