import type { BaseEntity } from '#/api/aigc/_shared';

import { createCrudApi } from '#/api/aigc/_shared';
import { requestClient } from '#/api/request';

/**
 * 角色实体。
 */
export interface AigcRole extends BaseEntity {
  code?: string;
  description?: string;
  name?: string;
}

/**
 * 角色菜单关联实体。
 */
export interface AigcRoleMenu extends BaseEntity {
  menuId?: string;
  roleId?: string;
}

export const roleApi = createCrudApi<AigcRole>('/v1/auth/roles');

export const roleMenuApi = {
  create: (payload: Partial<AigcRoleMenu>) =>
    requestClient.post<boolean>('/v1/auth/role-menus', payload),
  list: () => requestClient.get<AigcRoleMenu[]>('/v1/auth/role-menus'),
  remove: (payload: Partial<AigcRoleMenu>) =>
    requestClient.delete<boolean>('/v1/auth/role-menus', {
      data: payload,
    } as any),
};
