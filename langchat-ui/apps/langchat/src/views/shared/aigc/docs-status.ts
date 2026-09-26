/**
 * 解析文档向量化状态标签。
 */
export function resolveDocsStatusLabel(status?: null | string) {
  switch (status) {
    case 'completed':
      return '已完成';
    case 'failed':
      return '失败';
    case 'running':
      return '执行中';
    case 'pending':
      return '待处理';
    default:
      return '未知';
  }
}

/**
 * 解析文档向量化状态类型。
 */
export function resolveDocsStatusType(status?: null | string) {
  switch (status) {
    case 'completed':
      return 'success';
    case 'failed':
      return 'error';
    case 'running':
      return 'warning';
    case 'pending':
      return 'info';
    default:
      return 'default';
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
  return new Date(timestamp).toLocaleString('zh-CN', { hour12: false });
}
