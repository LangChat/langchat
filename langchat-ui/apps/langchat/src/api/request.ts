/**
 * 该文件可自行根据业务逻辑进行调整
 */
import type { RequestClientOptions } from '@vben/request';

import { useAppConfig } from '@vben/hooks';
import { preferences } from '@vben/preferences';
import {
  authenticateResponseInterceptor,
  defaultResponseInterceptor,
  errorMessageResponseInterceptor,
  RequestClient,
} from '@vben/request';
import { useAccessStore } from '@vben/stores';

import { message } from '#/adapter/naive';
import { useAuthStore } from '#/store';

import { refreshTokenApi } from './core';

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);
const DEFAULT_AUTH_HEADER = 'Authorization';
let currentAuthHeaderName = DEFAULT_AUTH_HEADER;

function formatToken(token: null | string) {
  return token || null;
}

function resolveAuthHeaderName(tokenName?: null | string) {
  return tokenName || currentAuthHeaderName || DEFAULT_AUTH_HEADER;
}

function removeAuthHeader(headers: Record<string, any>, tokenName: string) {
  delete headers[tokenName];
  if (tokenName !== DEFAULT_AUTH_HEADER) {
    delete headers[DEFAULT_AUTH_HEADER];
  }
}

function assignAuthHeader(
  headers: Record<string, any>,
  token: null | string,
  tokenName?: null | string,
) {
  const resolvedTokenName = resolveAuthHeaderName(tokenName);
  if (currentAuthHeaderName !== resolvedTokenName) {
    removeAuthHeader(headers, currentAuthHeaderName);
  }
  currentAuthHeaderName = resolvedTokenName;
  if (!token) {
    removeAuthHeader(headers, resolvedTokenName);
    return;
  }
  headers[resolvedTokenName] = formatToken(token);
}

function createRequestClient(baseURL: string, options?: RequestClientOptions) {
  const client = new RequestClient({
    ...options,
    baseURL,
  });

  /**
   * 重新认证逻辑
   */
  async function doReAuthenticate() {
    console.warn('Access token or refresh token is invalid or expired. ');
    const accessStore = useAccessStore();
    accessStore.setAccessToken(null);
    setRequestClientAuth(null);
    // 动态路由尚未生成（首次导航阶段的 401）：此时 currentRoute 还是初始地址 '/'，
    // 不能用它作为回跳地址，交由路由守卫捕获后携带真实目标路径跳转登录页
    if (!accessStore.isAccessChecked) {
      return;
    }
    if (preferences.app.loginExpiredMode === 'modal') {
      accessStore.setLoginExpired(true);
    } else {
      const authStore = useAuthStore();
      await authStore.logout();
    }
  }

  /**
   * 刷新token逻辑
   */
  async function doRefreshToken() {
    const accessStore = useAccessStore();
    const resp = await refreshTokenApi();
    const newToken = resp.data;
    accessStore.setAccessToken(newToken);
    setRequestClientAuth(newToken);
    return newToken;
  }

  // 请求头处理
  client.addRequestInterceptor({
    fulfilled: async (config) => {
      const accessStore = useAccessStore();
      config.headers = (config.headers ?? {}) as typeof config.headers;
      const headers = config.headers as Record<string, any>;
      assignAuthHeader(headers, accessStore.accessToken);
      config.headers['Accept-Language'] = preferences.app.locale;
      return config;
    },
  });

  // 处理返回的响应数据格式
  client.addResponseInterceptor(
    defaultResponseInterceptor({
      codeField: 'code',
      dataField: 'data',
      successCode: 'SUCCESS',
    }),
  );

  // token过期的处理
  client.addResponseInterceptor(
    authenticateResponseInterceptor({
      client,
      doReAuthenticate,
      doRefreshToken,
      enableRefreshToken: preferences.app.enableRefreshToken,
      formatToken,
    }),
  );

  // 通用的错误处理,如果没有进入上面的错误处理逻辑，就会进入这里
  client.addResponseInterceptor(
    errorMessageResponseInterceptor((msg: string, error) => {
      // 这里可以根据业务进行定制，根据不同业务码返回更细粒度的提示信息。
      const responseData = error?.response?.data ?? {};
      const errorMessage = responseData?.error ?? responseData?.message ?? '';
      // 如果没有错误信息，则会根据状态码进行提示
      message.error(errorMessage || msg);
    }),
  );

  return client;
}

export const requestClient = createRequestClient(apiURL, {
  responseReturn: 'data',
});

export const baseRequestClient = new RequestClient({ baseURL: apiURL });

/**
 * 同步请求客户端默认鉴权头，保证登录后首个请求立即携带 token。
 */
export function setRequestClientAuth(
  token: null | string,
  tokenName: string = DEFAULT_AUTH_HEADER,
) {
  const commonHeaders = requestClient.instance.defaults.headers
    .common as Record<string, any>;
  assignAuthHeader(commonHeaders, token, tokenName);
}

/**
 * 获取请求客户端基础地址。
 */
export function resolveRequestClientUrl(path: string) {
  const normalizedBaseURL = apiURL.endsWith('/') ? apiURL.slice(0, -1) : apiURL;
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${normalizedBaseURL}${normalizedPath}`;
}

/**
 * 获取当前鉴权请求头。
 */
export function getRequestClientAuthHeaders() {
  const accessStore = useAccessStore();
  const headers: Record<string, string> = {
    'Accept-Language': preferences.app.locale,
  };
  assignAuthHeader(headers, accessStore.accessToken, currentAuthHeaderName);
  return headers;
}
