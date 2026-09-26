import type { BaseEntity } from './_shared';

import { createCrudApi } from './_shared';

/**
 * 模型实体。
 */
export interface AigcModel extends BaseEntity {
  apiKey?: string;
  baseUrl?: string;
  billingType?: string;
  dimension?: number;
  endpoint?: string;
  inputPrice?: number;
  maxToken?: number;
  model?: string;
  name?: string;
  outputPrice?: number;
  provider?: string;
  secretKey?: string;
  temperature?: number;
  timeout?: number;
  timesPrice?: number;
  topP?: number;
  type?: string;
}

export const modelApi = createCrudApi<AigcModel>('/v1/aigc/models');
