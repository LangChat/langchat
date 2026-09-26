import type { BaseEntity } from './_shared';

import { createCrudApi } from './_shared';

/**
 * MCP 实体。
 */
export interface AigcMcp extends BaseEntity {
  authorized?: boolean;
  coverUrl?: string;
  description?: string;
  dockerHost?: string;
  dockerImage?: string;
  headers?: string;
  mcpJson?: string;
  name?: string;
  siteUrl?: string;
  sseUrl?: string;
  tags?: string;
  timeout?: number;
  transport?: string;
  uuid?: string;
}

export const mcpApi = createCrudApi<AigcMcp>('/v1/aigc/mcp');
