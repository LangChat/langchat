import type { BaseEntity } from './_shared';

import { createCrudApi } from './_shared';

/**
 * 模型实体。
 */
export interface AigcModelConfig {
  /** 向量模型输出维度。 */
  dimension?: number;
  /** 不同供应商、模型类型的差异化配置。 */
  [key: string]: unknown;
}

export interface AigcModel extends BaseEntity {
  apiKey?: string;
  baseUrl?: string;
  billingType?: string;
  configJson?: AigcModelConfig;
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
