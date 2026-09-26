import dashscopeSvg from '#/assets/provider-icons/alibabacloud.svg';
import anthropicSvg from '#/assets/provider-icons/anthropic.svg';
import azureSvg from '#/assets/provider-icons/azure.svg';
import cohereSvg from '#/assets/provider-icons/cohere.svg';
import deepseekSvg from '#/assets/provider-icons/deepseek.svg';
import geminiSvg from '#/assets/provider-icons/gemini.svg';
import groqSvg from '#/assets/provider-icons/groq.svg';
import minimaxSvg from '#/assets/provider-icons/minimax.svg';
import mistralSvg from '#/assets/provider-icons/mistral.svg';
import ollamaSvg from '#/assets/provider-icons/ollama.svg';
import openaiSvg from '#/assets/provider-icons/openai.svg';
import siliconflowSvg from '#/assets/provider-icons/siliconflow.svg';
import togetherSvg from '#/assets/provider-icons/together.svg';
import volcengineSvg from '#/assets/provider-icons/volcengine.svg';
import xaiSvg from '#/assets/provider-icons/xai.svg';
import zhipuSvg from '#/assets/provider-icons/zhipu.svg';

export const PROVIDER_ICON_MAP = {
  ANTHROPIC: anthropicSvg,
  AZURE_OPENAI: azureSvg,
  COHERE: cohereSvg,
  DASHSCOPE: dashscopeSvg,
  DEEPSEEK: deepseekSvg,
  GEMINI: geminiSvg,
  GROQ: groqSvg,
  MINIMAX: minimaxSvg,
  MISTRAL: mistralSvg,
  OLLAMA: ollamaSvg,
  OPENAI: openaiSvg,
  SILICONFLOW: siliconflowSvg,
  TOGETHER: togetherSvg,
  VOLCENGINE: volcengineSvg,
  XAI: xaiSvg,
  ZHIPU: zhipuSvg,
} as const;

export type ProviderKey = keyof typeof PROVIDER_ICON_MAP;

export function getProviderIconUrl(provider?: string): string {
  if (!provider) return '';
  return (PROVIDER_ICON_MAP as Record<string, string>)[provider] || '';
}
