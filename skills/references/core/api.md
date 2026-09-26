# API 请求与服务端交互

## 请求客户端配置

配置文件位于 `src/api/request.ts`：

```ts
import { RequestClient } from '@vben/request';
import { useAppConfig } from '@vben/hooks';
import { useAccessStore } from '@vben/stores';

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

function createRequestClient(baseURL: string) {
  const client = new RequestClient({ baseURL });

  // 请求拦截器 - 添加 Token
  client.addRequestInterceptor({
    fulfilled: async (config) => {
      const accessStore = useAccessStore();
      config.headers.Authorization = `Bearer ${accessStore.accessToken}`;
      return config;
    },
  });

  // 响应拦截器 - 处理返回数据
  client.addResponseInterceptor(
    defaultResponseInterceptor({
      codeField: 'code',
      dataField: 'data',
      successCode: 0,
    }),
  );

  // 响应拦截器 - Token 过期处理
  client.addResponseInterceptor(
    authenticateResponseInterceptor({
      client,
      doReAuthenticate,
      doRefreshToken,
      enableRefreshToken: true,
      formatToken: (token) => token ? `Bearer ${token}` : null,
    }),
  );

  // 响应拦截器 - 错误处理
  client.addResponseInterceptor(
    errorMessageResponseInterceptor((msg, error) => {
      message.error(msg);
    }),
  );

  return client;
}

export const requestClient = createRequestClient(apiURL);
```

## API 定义示例

```ts
// src/api/user.ts
import { requestClient } from '#/api/request';

interface UserInfo {
  id: number;
  username: string;
  realName: string;
  roles: string[];
}

// GET 请求
export async function getUserInfoApi() {
  return requestClient.get<UserInfo>('/user/info');
}

// POST 请求
export async function loginApi(data: { username: string; password: string }) {
  return requestClient.post<{ accessToken: string }>('/auth/login', data);
}

// PUT 请求
export async function updateUserApi(user: Partial<UserInfo>) {
  return requestClient.put<UserInfo>(`/user/${user.id}`, user);
}

// DELETE 请求
export async function deleteUserApi(id: number) {
  return requestClient.delete(`/user/${id}`);
}

// 带参数的 GET 请求
export async function getUserListApi(params: { page: number; size: number }) {
  return requestClient.get<{ list: UserInfo[]; total: number }>('/user/list', {
    params,
  });
}
```

## 扩展配置

```ts
type ExtendOptions = {
  // 参数序列化方式
  paramsSerializer?: 'brackets' | 'comma' | 'indices' | 'repeat';

  // 响应返回方式
  // 'raw' - 原始 AxiosResponse
  // 'body' - 响应体（不检查 code）
  // 'data' - 解构后的 data 字段（默认）
  responseReturn?: 'body' | 'data' | 'raw';
};
```

## 多接口地址

```ts
const { apiURL, otherApiURL } = useAppConfig(
  import.meta.env,
  import.meta.env.PROD,
);

export const requestClient = createRequestClient(apiURL);
export const otherRequestClient = createRequestClient(otherApiURL);
```

## 代理配置

开发环境代理配置在 `vite.config.mts`：

```ts
export default defineConfig(async () => {
  return {
    vite: {
      server: {
        proxy: {
          '/api': {
            target: 'http://localhost:5320/api',
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api/, ''),
            ws: true,
          },
        },
      },
    },
  };
});
```

## 刷新 Token

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    enableRefreshToken: true,
  },
});

// src/api/request.ts
async function doRefreshToken() {
  const accessStore = useAccessStore();
  const resp = await refreshTokenApi();
  const newToken = resp.data;
  accessStore.setAccessToken(newToken);
  return newToken;
}
```

## 接口返回格式

默认接口返回格式：

```ts
interface HttpResponse<T = any> {
  code: number;      // 0 表示成功
  data: T;
  message: string;
}
```

如需自定义，修改 `defaultResponseInterceptor` 配置。
