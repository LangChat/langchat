import type { BaseEntity } from './_shared';

import { createCrudApi } from './_shared';

/**
 * 向量库实体。
 */
export interface AigcVectorStore extends BaseEntity {
  databaseName?: string;
  dimension?: number;
  host?: string;
  name?: string;
  password?: string;
  port?: number;
  provider?: string;
  tableName?: string;
  username?: string;
}

export const vectorStoreApi = createCrudApi<AigcVectorStore>(
  '/v1/aigc/vector-stores',
);
