import { baseRequestClient, requestClient } from '#/api/request';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    password?: string;
    username?: string;
  }

  /** 登录接口返回值 */
  export interface LoginResult {
    accessToken: string;
    tokenName: string;
  }

  export interface BackendAuthCurrentUserResponse {
    displayName?: string;
    permissions?: string[];
    roles?: string[];
    tenantId?: string;
    userId: string;
    username: string;
  }

  export interface BackendAuthLoginResponse extends BackendAuthCurrentUserResponse {
    tokenName?: string;
    tokenValue: string;
  }

  export interface RefreshTokenResult {
    data: string;
    status: number;
  }
}

/**
 * 登录
 */
export async function loginApi(data: AuthApi.LoginParams) {
  const response = await requestClient.post<AuthApi.BackendAuthLoginResponse>(
    '/v1/auth/login',
    data,
  );
  return {
    accessToken: response.tokenValue,
    tokenName: response.tokenName || 'Authorization',
  };
}

/**
 * 刷新accessToken
 */
export async function refreshTokenApi() {
  return baseRequestClient.post<AuthApi.RefreshTokenResult>(
    '/v1/auth/refresh',
    {
      withCredentials: true,
    },
  );
}

/**
 * 退出登录
 */
export async function logoutApi() {
  return requestClient.post<boolean>('/v1/auth/logout');
}

/**
 * 获取用户权限码
 */
export async function getAccessCodesApi() {
  const response =
    await requestClient.get<AuthApi.BackendAuthCurrentUserResponse>(
      '/v1/auth/me',
    );
  return response.permissions ?? [];
}
