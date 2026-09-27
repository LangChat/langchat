import { unref } from 'vue';

import { $t, i18n } from '@vben/locales';

/**
 * 卡片通用时间展示：无时间戳回退为“刚刚”，其余按当前界面语言格式化。
 */
export function formatRelativeTime(timestamp?: number | string): string {
  if (!timestamp) {
    return $t('common.status.justNow');
  }
  const locale = unref(i18n.global.locale) || 'zh-CN';
  const time =
    typeof timestamp === 'number' ? timestamp : Number(timestamp) || 0;
  return new Date(time).toLocaleString(locale, { hour12: false });
}
