/**
 * 解析标签列表字符串。
 */
export function parseTagList(rawValue?: null | string | string[]) {
  if (Array.isArray(rawValue)) {
    return rawValue.map((item) => String(item ?? '').trim()).filter(Boolean);
  }
  if (!rawValue) {
    return [];
  }
  const value = String(rawValue).trim();
  if (!value) {
    return [];
  }

  if (value.startsWith('[')) {
    try {
      const parsed = JSON.parse(value);
      if (Array.isArray(parsed)) {
        return parsed.map((item) => String(item ?? '').trim()).filter(Boolean);
      }
    } catch {
      // ignore
    }
  }

  return value
    .split(/[,\n\r，]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

/**
 * 序列化标签列表为逗号分隔字符串。
 */
export function stringifyTagList(rawValue?: null | string | string[]) {
  const tags = parseTagList(rawValue);
  return tags.join(',');
}
