import type { BaseEntity } from '#/api/aigc/_shared';

import { createCrudApi } from '#/api/aigc/_shared';
import { requestClient } from '#/api/request';

/**
 * 用户实体。
 */
export interface AigcUser extends BaseEntity {
  avatar?: string;
  deptId?: string;
  email?: string;
  password?: string;
  phone?: string;
  realName?: string;
  sex?: string;
  status?: number;
  username?: string;
}

/**
 * 用户角色关联实体。
 */
export interface AigcUserRole extends BaseEntity {
  roleId?: string;
  userId?: string;
}

export const userApi = createCrudApi<AigcUser>('/v1/auth/users');

export const userRoleApi = {
  create: (payload: Partial<AigcUserRole>) =>
    requestClient.post<boolean>('/v1/auth/user-roles', payload),
  list: () => requestClient.get<AigcUserRole[]>('/v1/auth/user-roles'),
  remove: (payload: Partial<AigcUserRole>) =>
    requestClient.delete<boolean>('/v1/auth/user-roles', {
      data: payload,
    } as any),
};
