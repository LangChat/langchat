import type { BaseEntity } from './_shared';

import { createCrudApi } from './_shared';

/**
 * 知识库实体。
 */
export interface AigcKnowledge extends BaseEntity {
  coverUrl?: string;
  description?: string;
  maxResults?: number;
  minScore?: number;
  modelId?: string;
  name?: string;
  rerank?: boolean;
  rerankModelId?: string;
  tags?: string;
  vectorModelId?: string;
  vectorStoreId?: string;
  visionModelId?: string;
}

export const knowledgeApi = createCrudApi<AigcKnowledge>('/v1/aigc/knowledges');
