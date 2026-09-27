import type { LabelOption } from '#/views/shared/aigc/options';
import type { Component } from 'vue';
import {
  AudioLines,
  AudioWaveform,
  Bot,
  DatabaseZap,
  FileSearch,
  Image,
  ImagePlus,
  Video,
} from '@vben/icons';

import { $t } from '@vben/locales';

import { PROVIDER_ICON_MAP } from './provider-icons';

function pi(key: string) {
  return (PROVIDER_ICON_MAP as Record<string, string>)[key] || '';
}

export type ModelTypeKey =
  | 'EMBEDDINGS'
  | 'IMAGE2TEXT'
  | 'OCR'
  | 'SPEECH2TEXT'
  | 'TEXT2IMAGE'
  | 'TEXT2SPEECH'
  | 'TEXT2TEXT'
  | 'TEXT2VIDEO';

export interface ModelProviderMeta {
  baseUrl?: string;
  icon: string;
  label: string;
  models: Partial<Record<ModelTypeKey, string[]>>;
}

export interface ModelTypeMeta {
  configItems: string[];
  description: string;
  icon: Component;
  label: string;
}

/** 后端/前端历史枚举值到规范模型类型的归一化映射。 */
const MODEL_TYPE_ALIAS_MAP: Record<string, ModelTypeKey> = {
  CHAT: 'TEXT2TEXT',
  EMBEDDING: 'EMBEDDINGS',
  EMBEDDINGS: 'EMBEDDINGS',
  IMAGE2TEXT: 'IMAGE2TEXT',
  OCR: 'OCR',
  REASONING: 'TEXT2TEXT',
  SPEECH: 'SPEECH2TEXT',
  SPEECH2TEXT: 'SPEECH2TEXT',
  TEXT2IMAGE: 'TEXT2IMAGE',
  TEXT2SPEECH: 'TEXT2SPEECH',
  TEXT2TEXT: 'TEXT2TEXT',
  TEXT2VIDEO: 'TEXT2VIDEO',
  VISION: 'IMAGE2TEXT',
};

/**
 * 模型类型元信息：label/description 为多语言文案，经 getter 惰性读取，
 * 保持导出结构不变且随界面语言切换。
 */
export const MODEL_TYPE_META: Record<ModelTypeKey, ModelTypeMeta> = {
  EMBEDDINGS: {
    configItems: ['dimension', 'timeout', 'baseUrl', 'apiKey'],
    get description() {
      return $t('models.type.EMBEDDINGS.description');
    },
    icon: DatabaseZap,
    get label() {
      return $t('models.type.EMBEDDINGS.label');
    },
  },
  IMAGE2TEXT: {
    configItems: [
      'maxToken',
      'temperature',
      'topP',
      'timeout',
      'baseUrl',
      'apiKey',
    ],
    get description() {
      return $t('models.type.IMAGE2TEXT.description');
    },
    icon: Image,
    get label() {
      return $t('models.type.IMAGE2TEXT.label');
    },
  },
  OCR: {
    configItems: ['timeout', 'baseUrl', 'apiKey'],
    get description() {
      return $t('models.type.OCR.description');
    },
    icon: FileSearch,
    get label() {
      return $t('models.type.OCR.label');
    },
  },
  SPEECH2TEXT: {
    configItems: ['timeout', 'baseUrl', 'apiKey'],
    get description() {
      return $t('models.type.SPEECH2TEXT.description');
    },
    icon: AudioLines,
    get label() {
      return $t('models.type.SPEECH2TEXT.label');
    },
  },
  TEXT2IMAGE: {
    configItems: ['timeout', 'baseUrl', 'apiKey'],
    get description() {
      return $t('models.type.TEXT2IMAGE.description');
    },
    icon: ImagePlus,
    get label() {
      return $t('models.type.TEXT2IMAGE.label');
    },
  },
  TEXT2SPEECH: {
    configItems: ['timeout', 'baseUrl', 'apiKey'],
    get description() {
      return $t('models.type.TEXT2SPEECH.description');
    },
    icon: AudioWaveform,
    get label() {
      return $t('models.type.TEXT2SPEECH.label');
    },
  },
  TEXT2TEXT: {
    configItems: [
      'maxToken',
      'temperature',
      'topP',
      'timeout',
      'baseUrl',
      'apiKey',
    ],
    get description() {
      return $t('models.type.TEXT2TEXT.description');
    },
    icon: Bot,
    get label() {
      return $t('models.type.TEXT2TEXT.label');
    },
  },
  TEXT2VIDEO: {
    configItems: ['timeout', 'baseUrl', 'apiKey'],
    get description() {
      return $t('models.type.TEXT2VIDEO.description');
    },
    icon: Video,
    get label() {
      return $t('models.type.TEXT2VIDEO.label');
    },
  },
};

/**
 * 各供应商已适配的模型能力（models 的 key 即该供应商支持的模型类型）。
 *
 * 当前后端已按供应商接入官方协议：文本生成全量支持；向量模型除
 * DeepSeek / MiniMax 外均支持；文生图仅 OpenAI / 通义千问 / Gemini。
 * 图像理解（VISION/IMAGE2TEXT）已并入文本生成模型能力，不再单独提供。
 */
export const MODEL_PROVIDER_META: Record<string, ModelProviderMeta> = {
  OPENAI: {
    baseUrl: 'https://api.openai.com/v1',
    icon: pi('OPENAI'),
    label: 'OpenAI',
    models: {
      EMBEDDINGS: ['text-embedding-3-small', 'text-embedding-3-large'],
      TEXT2IMAGE: ['gpt-image-1', 'gpt-image-1-mini'],
      TEXT2TEXT: ['gpt-5.1', 'gpt-5', 'gpt-5-mini', 'gpt-5-nano', 'gpt-4.1'],
    },
  },
  DASHSCOPE: {
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    icon: pi('DASHSCOPE'),
    get label() {
      return $t('models.provider.DASHSCOPE');
    },
    models: {
      EMBEDDINGS: [
        'qwen3.7-text-embedding',
        'qwen3.7-text-embedding-flash',
        'qwen3-vl-embedding',
      ],
      TEXT2IMAGE: ['qwen-image-max', 'wanx2.1-t2i-plus'],
      TEXT2TEXT: [
        'qwen3.8-max',
        'qwen3.8-flash',
        'qwen3.7-plus',
        'qwen3.8-omni-flash',
      ],
    },
  },
  DEEPSEEK: {
    baseUrl: 'https://api.deepseek.com/v1',
    icon: pi('DEEPSEEK'),
    label: 'DeepSeek',
    models: {
      TEXT2TEXT: ['deepseek-v4-pro', 'deepseek-flash'],
    },
  },
  ZHIPU: {
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    icon: pi('ZHIPU'),
    get label() {
      return $t('models.provider.ZHIPU');
    },
    models: {
      EMBEDDINGS: ['embedding-3'],
      TEXT2TEXT: ['glm-5.2', 'glm-5-turbo', 'glm-5'],
    },
  },
  VOLCENGINE: {
    baseUrl: 'https://ark.cn-beijing.volces.com/api/v3',
    icon: pi('VOLCENGINE'),
    get label() {
      return $t('models.provider.VOLCENGINE');
    },
    models: {
      EMBEDDINGS: ['doubao-embedding-large'],
      TEXT2TEXT: [
        'doubao-seed-evolving',
        'doubao-seed-2-1-pro-260915',
        'doubao-seed-2-1-lite-260915',
        'doubao-seed-2-1-turbo-260628',
      ],
    },
  },
  GEMINI: {
    baseUrl: 'https://generativelanguage.googleapis.com/v1beta',
    icon: pi('GEMINI'),
    label: 'Google Gemini',
    models: {
      EMBEDDINGS: ['gemini-embedding-2-preview', 'gemini-embedding-001'],
      TEXT2IMAGE: [
        'gemini-3-pro-image',
        'gemini-3.1-flash-image',
        'gemini-3.1-flash-lite-image',
      ],
      TEXT2TEXT: [
        'gemini-3.8-flash',
        'gemini-3.7-flash',
        'gemini-3.5-flash',
        'gemini-3.1-pro-preview',
      ],
    },
  },
  MINIMAX: {
    baseUrl: 'https://api.minimax.chat/v1',
    icon: pi('MINIMAX'),
    label: 'MiniMax',
    models: {
      TEXT2TEXT: [
        'MiniMax-M3',
        'MiniMax-M2.7',
        'MiniMax-M2.7-highspeed',
        'MiniMax-M2.5',
      ],
    },
  },
  OLLAMA: {
    baseUrl: 'http://localhost:11434/v1',
    icon: pi('OLLAMA'),
    label: 'Ollama',
    models: {
      EMBEDDINGS: ['qwen3-embedding', 'bge-m3', 'nomic-embed-text'],
      TEXT2TEXT: ['qwen3.5', 'glm-5', 'gpt-oss:120b', 'deepseek-r1'],
    },
  },
};

const MODEL_CONFIG_FIELD_LABEL_MAP: Record<string, string> = {
  apiKey: 'apiKey',
  baseUrl: 'baseUrl',
  dimension: 'dimension',
  maxToken: 'maxToken',
  temperature: 'temperature',
  timeout: 'timeout',
  topP: 'topP',
};

const DEFAULT_PROVIDER_META: ModelProviderMeta = {
  baseUrl: '',
  icon: '',
  get label() {
    return $t('models.provider.CUSTOM');
  },
  models: {},
};

/** 自定义/未知供应商的默认能力：仅文本生成与向量模型。 */
const DEFAULT_PROVIDER_CAPABILITIES: ModelTypeKey[] = [
  'TEXT2TEXT',
  'EMBEDDINGS',
];

export function normalizeModelType(type?: string) {
  if (!type) {
    return 'TEXT2TEXT' as ModelTypeKey;
  }
  const normalized = String(type).toUpperCase();
  return MODEL_TYPE_ALIAS_MAP[normalized] || 'TEXT2TEXT';
}

export function getModelTypeMeta(type?: string): ModelTypeMeta {
  return MODEL_TYPE_META[normalizeModelType(type)];
}

export function getModelTypeIcon(type?: string) {
  return getModelTypeMeta(type).icon;
}

export function getModelTypeLabel(type?: string) {
  return getModelTypeMeta(type).label;
}

export function getModelTypeOptions(): LabelOption[] {
  return (Object.keys(MODEL_TYPE_META) as ModelTypeKey[]).map((key) => ({
    label: MODEL_TYPE_META[key].label,
    value: key,
  }));
}

export function getModelTypeConfigItems(type?: string) {
  return getModelTypeMeta(type).configItems.map(
    (item) => MODEL_CONFIG_FIELD_LABEL_MAP[item] || item,
  );
}

export function getModelTypeConfigFields(type?: string) {
  return [...getModelTypeMeta(type).configItems];
}

export function getProviderMeta(provider?: string) {
  if (!provider) {
    return DEFAULT_PROVIDER_META;
  }
  return (
    MODEL_PROVIDER_META[provider] || {
      ...DEFAULT_PROVIDER_META,
      label: provider,
    }
  );
}

export function getProviderIcon(provider?: string): string {
  return getProviderMeta(provider).icon;
}

export function getProviderLabel(provider?: string) {
  return getProviderMeta(provider).label;
}

export function getProviderBaseUrl(provider?: string) {
  return getProviderMeta(provider).baseUrl || '';
}

export function getModelProviderOptions(): LabelOption[] {
  return Object.entries(MODEL_PROVIDER_META).map(([value, meta]) => ({
    label: meta.label,
    value,
  }));
}

export function getProviderSupportedTypes(provider?: string): ModelTypeKey[] {
  const modelTypes = Object.keys(
    getProviderMeta(provider).models,
  ) as ModelTypeKey[];
  if (modelTypes.length > 0) {
    return (Object.keys(MODEL_TYPE_META) as ModelTypeKey[]).filter((type) =>
      modelTypes.includes(type),
    );
  }
  // 未在能力清单中的自定义供应商：默认只提供文本生成与向量模型
  return DEFAULT_PROVIDER_CAPABILITIES.filter((type) => type in MODEL_TYPE_META);
}

/** 全部供应商能力的并集（用于“全部供应商”视图的模型类型页签）。 */
export function getAllProviderCapabilities(): ModelTypeKey[] {
  const capabilitySet = new Set<ModelTypeKey>();
  for (const meta of Object.values(MODEL_PROVIDER_META)) {
    for (const key of Object.keys(meta.models) as ModelTypeKey[]) {
      capabilitySet.add(key);
    }
  }
  return (Object.keys(MODEL_TYPE_META) as ModelTypeKey[]).filter((type) =>
    capabilitySet.has(type),
  );
}

export function getProviderTypeOptions(provider?: string): LabelOption[] {
  return getProviderSupportedTypes(provider).map((type) => ({
    label: MODEL_TYPE_META[type].label,
    value: type,
  }));
}

export function getProviderRecommendedModels(provider?: string, type?: string) {
  const meta = getProviderMeta(provider);
  return meta.models[normalizeModelType(type)] || [];
}
