import type { UserInfo } from '@vben/types';

import { $t } from '@vben/locales';
import { preferences } from '@vben/preferences';

import { requestClient } from '#/api/request';

interface BackendAuthCurrentUserResponse {
  displayName?: string;
  permissions?: string[];
  roles?: string[];
  tenantId?: string;
  userId: string;
  username: string;
}

/**
 * 获取用户信息
 */
export async function getUserInfoApi() {
  const response =
    await requestClient.get<BackendAuthCurrentUserResponse>('/v1/auth/me');
  return {
    avatar: preferences.app.defaultAvatar,
    desc: response.tenantId
      ? $t('layout.user.currentTenant', { tenantId: response.tenantId })
      : $t('layout.user.defaultDesc'),
    homePath: '/explore',
    realName: response.displayName || response.username,
    roles: response.roles ?? [],
    token: '',
    userId: response.userId,
    username: response.username,
  } satisfies UserInfo;
}
