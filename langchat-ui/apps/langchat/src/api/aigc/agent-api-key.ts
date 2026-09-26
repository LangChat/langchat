import type { BaseEntity } from './_shared';
import type { AigcMessage } from './chat';

import { requestClient } from '#/api/request';

/**
 * Agent API Key 实体。
 */
export interface AigcAgentApiKey extends BaseEntity {
  agentId?: string;
  apiKey?: string;
  callCount?: number;
  inputTokens?: number;
  lastCallTime?: number;
  name?: string;
  outputTokens?: number;
  remark?: string;
  status?: string;
}

/** OpenAI 兼容接口地址（相对当前后端）。 */
export const OPENAI_COMPATIBLE_PATH = '/v1/chat/completions';

export function listAgentApiKeysApi(agentId: string) {
  return requestClient.get<AigcAgentApiKey[]>(
    `/v1/aigc/agents/${agentId}/api-keys`,
  );
}

export function createAgentApiKeyApi(
  agentId: string,
  data: { name?: string; remark?: string },
) {
  return requestClient.post<AigcAgentApiKey>(
    `/v1/aigc/agents/${agentId}/api-keys`,
    data,
  );
}

export function updateAgentApiKeyApi(id: string, data: Partial<AigcAgentApiKey>) {
  return requestClient.put<boolean>(`/v1/aigc/agents/api-keys/${id}`, data);
}

export function removeAgentApiKeyApi(id: string) {
  return requestClient.delete<boolean>(`/v1/aigc/agents/api-keys/${id}`);
}

export function listAgentApiKeyMessagesApi(id: string) {
  return requestClient.get<AigcMessage[]>(
    `/v1/aigc/agents/api-keys/${id}/messages`,
  );
}
