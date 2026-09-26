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
  name?: string;
  tags?: string;
  vectorModelId?: string;
  vectorStoreId?: string;
}

export const knowledgeApi = createCrudApi<AigcKnowledge>('/v1/aigc/knowledges');
