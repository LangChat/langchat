import type { AigcMenuTreeNode } from '#/api/auth/menu';

/**
 * 扁平化菜单树。
 */
export function flattenMenuTree(items: AigcMenuTreeNode[]) {
  const result: AigcMenuTreeNode[] = [];

  function walk(nodes: AigcMenuTreeNode[]) {
    for (const item of nodes) {
      result.push(item);
      if ((item.children?.length ?? 0) > 0) {
        walk(item.children ?? []);
      }
    }
  }

  walk(items);

  return result;
}

/**
 * 构建菜单名称映射。
 */
export function buildMenuNameMap(items: AigcMenuTreeNode[]) {
  return Object.fromEntries(
    flattenMenuTree(items).map((item) => [
      item.id ?? '',
      item.name || '未命名菜单',
    ]),
  ) as Record<string, string>;
}

/**
 * 过滤菜单树，排除指定节点及其子节点。
 */
export function filterMenuTree(
  items: AigcMenuTreeNode[],
  excludeId?: string,
): AigcMenuTreeNode[] {
  return items
    .filter((item) => item.id !== excludeId)
    .map((item) => ({
      ...item,
      children: filterMenuTree(item.children ?? [], excludeId),
    }));
}
