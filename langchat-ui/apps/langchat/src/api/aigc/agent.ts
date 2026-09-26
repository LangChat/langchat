import type { BaseEntity } from './_shared';

import { createCrudApi } from './_shared';

/**
 * 智能体实体。
 */
export interface AigcAgent extends BaseEntity {
  agentName?: string;
  avatar?: string;
  defaultSuggestions?: string;
  description?: string;
  enableAutoSuggestion?: boolean;
  icon?: string;
  knowledgeIds?: string;
  mcpIds?: string;
  metaJson?: string;
  modelConfigJson?: string;
  reasoningModelId?: string;
  skillIds?: string;
  status?: string;
  systemPrompt?: string;
  tags?: string;
  welcomeMessage?: string;
}

export const agentApi = createCrudApi<AigcAgent>('/v1/aigc/agents');
