export interface LabelOption {
  label: string;
  value: number | string;
}

export const MODEL_TYPE_OPTIONS: LabelOption[] = [
  { label: '聊天模型', value: 'CHAT' },
  { label: '推理模型', value: 'REASONING' },
  { label: '向量模型', value: 'EMBEDDING' },
  { label: '重排模型', value: 'RERANK' },
  { label: '视觉模型', value: 'VISION' },
  { label: '语音模型', value: 'SPEECH' },
];

export const MODEL_PROVIDER_OPTIONS: LabelOption[] = [
  { label: 'OpenAI', value: 'OPENAI' },
  { label: 'DeepSeek', value: 'DEEPSEEK' },
  { label: 'Ollama', value: 'OLLAMA' },
  { label: '通义千问', value: 'DASHSCOPE' },
  { label: 'Azure OpenAI', value: 'AZURE_OPENAI' },
];

export const VECTOR_PROVIDER_OPTIONS: LabelOption[] = [
  { label: 'PGVector', value: 'PGVECTOR' },
  { label: 'Milvus', value: 'MILVUS' },
];

export const MCP_TRANSPORT_OPTIONS: LabelOption[] = [
  { label: 'Docker', value: 'SSE' },
  { label: 'HTTP', value: 'HTTP' },
  { label: 'Stdio', value: 'STDIO' },
];

export const DOC_TYPE_OPTIONS: LabelOption[] = [
  { label: '文件', value: 'FILE' },
  { label: 'Markdown', value: 'MARKDOWN' },
  { label: '文本', value: 'TEXT' },
  { label: '问答', value: 'QA' },
];

export const AGENT_STATUS_OPTIONS: LabelOption[] = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '停用', value: 'DISABLED' },
];

export const MEMORY_TYPE_OPTIONS: LabelOption[] = [
  { label: '窗口记忆', value: 'WINDOW' },
  { label: '摘要记忆', value: 'SUMMARY' },
  { label: '长期记忆', value: 'LONG_TERM' },
];

export const AIGC_COMMON_TAG_OPTIONS: LabelOption[] = [
  { label: '通用', value: '通用' },
  { label: '生产', value: '生产' },
  { label: '测试', value: '测试' },
  { label: '内部', value: '内部' },
  { label: '外部', value: '外部' },
  { label: '核心', value: '核心' },
];

export function findOptionLabel(options: LabelOption[], value?: null | string) {
  if (!value) {
    return '未配置';
  }
  return options.find((option) => option.value === value)?.label ?? value;
}
