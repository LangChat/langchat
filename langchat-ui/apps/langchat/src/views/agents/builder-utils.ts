interface ModelConfig {
  maxOutputTokens?: number;
  temperature?: number;
  topP?: number;
}

export function parseMetaJson(raw?: string) {
  if (!raw) {
    return {} as Record<string, unknown>;
  }
  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object'
      ? (parsed as Record<string, unknown>)
      : {};
  } catch {
    return {};
  }
}

export function parseSuggestions(raw: unknown): string[] {
  if (Array.isArray(raw)) {
    return raw.map((item) => String(item || '').trim()).filter(Boolean);
  }
  if (!raw) {
    return [];
  }
  const text = String(raw).trim();
  if (!text) {
    return [];
  }
  if (text.startsWith('[')) {
    try {
      const parsed = JSON.parse(text);
      if (Array.isArray(parsed)) {
        return parsed.map((item) => String(item || '').trim()).filter(Boolean);
      }
    } catch {
      // noop
    }
  }
  return text
    .split(/[\n,，]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

export function parseModelConfig(raw?: string): ModelConfig {
  if (!raw) {
    return {};
  }
  try {
    const parsed = JSON.parse(raw);
    if (parsed && typeof parsed === 'object') {
      return {
        maxOutputTokens: toNumber(
          (parsed as Record<string, unknown>).maxOutputTokens,
        ),
        temperature: toNumber((parsed as Record<string, unknown>).temperature),
        topP: toNumber((parsed as Record<string, unknown>).topP),
      };
    }
    return {};
  } catch {
    return {};
  }
}

export function buildModelConfigJson(config: ModelConfig) {
  const payload: Record<string, number> = {};
  if (typeof config.temperature === 'number') {
    payload.temperature = config.temperature;
  }
  if (typeof config.topP === 'number') {
    payload.topP = config.topP;
  }
  if (typeof config.maxOutputTokens === 'number') {
    payload.maxOutputTokens = Math.round(config.maxOutputTokens);
  }
  return Object.keys(payload).length > 0 ? JSON.stringify(payload) : '';
}

function toNumber(value: unknown) {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : undefined;
  }
  if (typeof value === 'string') {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }
  return undefined;
}

export type { ModelConfig };
