import { $t } from '@vben/locales';

export interface LabelOption {
  label: string;
  value: number | string;
}

/** 模型配置与向量库配置共用的常见向量维度。 */
export function vectorDimensionOptions(): LabelOption[] {
  return [256, 384, 512, 768, 1024, 1536, 2048, 3072, 4096].map(
    (value) => ({
      label: String(value),
      value,
    }),
  );
}

export function modelTypeOptions(): LabelOption[] {
  return [
    { label: $t('models.typeOptions.chat'), value: 'CHAT' },
    { label: $t('models.typeOptions.reasoning'), value: 'REASONING' },
    { label: $t('models.typeOptions.embedding'), value: 'EMBEDDING' },
    { label: $t('models.typeOptions.vision'), value: 'VISION' },
    { label: $t('models.typeOptions.speech'), value: 'SPEECH' },
  ];
}

export function modelProviderOptions(): LabelOption[] {
  return [
    { label: 'OpenAI', value: 'OPENAI' },
    { label: 'DeepSeek', value: 'DEEPSEEK' },
    { label: 'Ollama', value: 'OLLAMA' },
    { label: $t('models.provider.DASHSCOPE'), value: 'DASHSCOPE' },
    { label: 'Azure OpenAI', value: 'AZURE_OPENAI' },
  ];
}

/** 向量库供应商标识为产品名，保持原文即可。 */
export const VECTOR_PROVIDER_OPTIONS: LabelOption[] = [
  { label: 'PGVector', value: 'PGVECTOR' },
  { label: 'Milvus', value: 'MILVUS' },
];

/** MCP 传输协议为技术协议名，保持原文即可。 */
export const MCP_TRANSPORT_OPTIONS: LabelOption[] = [
  { label: 'Docker', value: 'SSE' },
  { label: 'HTTP', value: 'HTTP' },
  { label: 'Stdio', value: 'STDIO' },
];

export function docTypeOptions(): LabelOption[] {
  return [
    { label: $t('docs.typeOptions.file'), value: 'FILE' },
    { label: 'Markdown', value: 'MARKDOWN' },
    { label: $t('docs.typeOptions.text'), value: 'TEXT' },
    { label: $t('docs.typeOptions.qa'), value: 'QA' },
  ];
}

export function agentStatusOptions(): LabelOption[] {
  return [
    { label: $t('agents.status.draft'), value: 'DRAFT' },
    { label: $t('agents.status.published'), value: 'PUBLISHED' },
    { label: $t('agents.status.disabled'), value: 'DISABLED' },
  ];
}

export function memoryTypeOptions(): LabelOption[] {
  return [
    { label: $t('agents.memory.window'), value: 'WINDOW' },
    { label: $t('agents.memory.summary'), value: 'SUMMARY' },
    { label: $t('agents.memory.longTerm'), value: 'LONG_TERM' },
  ];
}

export function aigcCommonTagOptions(): LabelOption[] {
  return [
    { label: $t('agents.scope.general'), value: '通用' },
    { label: $t('agents.scope.production'), value: '生产' },
    { label: $t('agents.scope.testing'), value: '测试' },
    { label: $t('agents.scope.internal'), value: '内部' },
    { label: $t('agents.scope.external'), value: '外部' },
    { label: $t('agents.scope.core'), value: '核心' },
  ];
}

export function findOptionLabel(options: LabelOption[], value?: null | string) {
  if (!value) {
    return $t('common.status.notConfigured');
  }
  return options.find((option) => option.value === value)?.label ?? value;
}
