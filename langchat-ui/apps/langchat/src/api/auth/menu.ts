import type { BaseEntity } from '#/api/aigc/_shared';

import { createCrudApi } from '#/api/aigc/_shared';
import { requestClient } from '#/api/request';

/**
 * 菜单实体。
 */
export interface AigcMenu extends BaseEntity {
  component?: string;
  icon?: string;
  isDisabled?: boolean;
  isExt?: boolean;
  isKeepalive?: boolean;
  isShow?: boolean;
  name?: string;
  orderNo?: number;
  parentId?: string;
  path?: string;
  perms?: string;
  type?: string;
}

/**
 * 菜单树节点。
 */
export interface AigcMenuTreeNode extends AigcMenu {
  children?: AigcMenuTreeNode[];
}

export const menuApi = {
  ...createCrudApi<AigcMenu>('/v1/auth/menus'),
  listTree: (params?: { keyword?: string; type?: string }) =>
    requestClient.get<AigcMenuTreeNode[]>('/v1/auth/menus/tree', { params }),
};
