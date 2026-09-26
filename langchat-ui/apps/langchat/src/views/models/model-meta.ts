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

const MODEL_TYPE_ALIAS_MAP: Record<string, ModelTypeKey> = {
  CHAT: 'TEXT2TEXT',
  EMBEDDING: 'EMBEDDINGS',
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

export const MODEL_TYPE_META: Record<ModelTypeKey, ModelTypeMeta> = {
  EMBEDDINGS: {
    configItems: ['dimension', 'timeout', 'baseUrl', 'apiKey'],
    description: '向量化检索、召回、聚类',
    icon: DatabaseZap,
    label: '向量模型',
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
    description: '图像理解、多模态问答',
    icon: Image,
    label: '图像理解',
  },
  OCR: {
    configItems: ['timeout', 'baseUrl', 'apiKey'],
    description: '图文识别、结构化抽取',
    icon: FileSearch,
    label: '光学识别',
  },
  SPEECH2TEXT: {
    configItems: ['timeout', 'baseUrl', 'apiKey'],
    description: '语音识别、实时听写',
    icon: AudioLines,
    label: '语音识别',
  },
  TEXT2IMAGE: {
    configItems: ['timeout', 'baseUrl', 'apiKey'],
    description: '文生图、风格化生成',
    icon: ImagePlus,
    label: '文生图',
  },
  TEXT2SPEECH: {
    configItems: ['timeout', 'baseUrl', 'apiKey'],
    description: '语音播报、语音克隆',
    icon: AudioWaveform,
    label: '文本转语音',
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
    description: '通用对话、推理、代码生成',
    icon: Bot,
    label: '文本生成',
  },
  TEXT2VIDEO: {
    configItems: ['timeout', 'baseUrl', 'apiKey'],
    description: '文生视频、镜头编排',
    icon: Video,
    label: '文生视频',
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
      TEXT2IMAGE: ['gpt-image-1'],
      TEXT2TEXT: ['gpt-4.1', 'gpt-4o', 'gpt-4.1-mini'],
    },
  },
  DASHSCOPE: {
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    icon: pi('DASHSCOPE'),
    label: '通义千问',
    models: {
      EMBEDDINGS: ['text-embedding-v3'],
      TEXT2IMAGE: ['wanx2.1-t2i-turbo', 'wanx2.1-t2i-plus'],
      TEXT2TEXT: ['qwen-max', 'qwen-plus', 'qwen-turbo', 'qwen3-max'],
    },
  },
  DEEPSEEK: {
    baseUrl: 'https://api.deepseek.com/v1',
    icon: pi('DEEPSEEK'),
    label: 'DeepSeek',
    models: {
      TEXT2TEXT: ['deepseek-chat', 'deepseek-reasoner'],
    },
  },
  ZHIPU: {
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    icon: pi('ZHIPU'),
    label: '智谱',
    models: {
      EMBEDDINGS: ['embedding-3'],
      TEXT2TEXT: ['glm-4-plus', 'glm-4-air'],
    },
  },
  VOLCENGINE: {
    baseUrl: 'https://ark.cn-beijing.volces.com/api/v3',
    icon: pi('VOLCENGINE'),
    label: '火山引擎',
    models: {
      EMBEDDINGS: ['doubao-embedding-large'],
      TEXT2TEXT: ['doubao-pro-32k', 'doubao-lite-32k'],
    },
  },
  GEMINI: {
    baseUrl: 'https://generativelanguage.googleapis.com/v1beta',
    icon: pi('GEMINI'),
    label: 'Google Gemini',
    models: {
      EMBEDDINGS: ['text-embedding-004'],
      TEXT2IMAGE: ['imagen-3.0-generate-002'],
      TEXT2TEXT: ['gemini-2.5-pro', 'gemini-2.5-flash'],
    },
  },
  MINIMAX: {
    baseUrl: 'https://api.minimax.chat/v1',
    icon: pi('MINIMAX'),
    label: 'MiniMax',
    models: {
      TEXT2TEXT: ['MiniMax-Text-01'],
    },
  },
  OLLAMA: {
    baseUrl: 'http://localhost:11434/v1',
    icon: pi('OLLAMA'),
    label: 'Ollama',
    models: {
      EMBEDDINGS: ['bge-m3', 'nomic-embed-text'],
      TEXT2TEXT: ['llama3.1', 'qwen2.5', 'deepseek-r1'],
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
  label: '自定义供应商',
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
