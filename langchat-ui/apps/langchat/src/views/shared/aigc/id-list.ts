import type { LabelOption } from './options';

/**
 * 解析 ID 列表字段。
 */
export function parseIdList(rawValue?: null | string | string[]) {
  if (Array.isArray(rawValue)) {
    return rawValue.filter(Boolean);
  }
  if (!rawValue) {
    return [];
  }
  const value = rawValue.trim();
  if (!value) {
    return [];
  }
  if (value.startsWith('[')) {
    try {
      const parsed = JSON.parse(value);
      return Array.isArray(parsed)
        ? parsed.map((item) => String(item ?? '').trim()).filter(Boolean)
        : [];
    } catch {
      return [];
    }
  }
  return value
    .split(/[,\n\r]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

/**
 * 序列化 ID 列表字段。
 */
export function stringifyIdList(value?: null | string | string[]) {
  const items = parseIdList(value);
  return items.length > 0 ? JSON.stringify(items) : '[]';
}

/**
 * 根据选项解析标签列表。
 */
export function resolveOptionLabels(
  options: LabelOption[],
  rawValue?: null | string | string[],
) {
  const values = parseIdList(rawValue);
  if (values.length === 0) {
    return [];
  }
  return values.map(
    (value) =>
      options.find((item) => String(item.value) === value)?.label ?? value,
  );
}
