import { $t, i18n } from '@vben/locales';
import { unref } from 'vue';

/**
 * 解析文档向量化状态标签。
 */
export function resolveDocsStatusLabel(status?: null | string) {
  switch (status) {
    case 'completed': {
      return $t('docs.status.completed');
    }
    case 'failed': {
      return $t('docs.status.failed');
    }
    case 'running': {
      return $t('docs.status.running');
    }
    case 'pending': {
      return $t('docs.status.pending');
    }
    default: {
      return $t('common.status.unknown');
    }
  }
}

/**
 * 解析文档向量化状态类型。
 */
export function resolveDocsStatusType(status?: null | string) {
  switch (status) {
    case 'completed': {
      return 'success';
    }
    case 'failed': {
      return 'error';
    }
    case 'running': {
      return 'warning';
    }
    case 'pending': {
      return 'info';
    }
    default: {
      return 'default';
    }
  }
}

/**
 * 格式化耗时。
 */
export function formatDocsDuration(costMs?: null | number) {
  if (costMs === null || costMs === undefined) {
    return '--';
  }
  if (costMs < 1000) {
    return `${costMs} ms`;
  }
  return `${(costMs / 1000).toFixed(2)} s`;
}

/**
 * 格式化时间。
 */
export function formatDocsTimestamp(timestamp?: null | number) {
  if (!timestamp) {
    return '--';
  }
  const locale = unref(i18n.global.locale) || 'zh-CN';
  return new Date(timestamp).toLocaleString(locale, { hour12: false });
}
