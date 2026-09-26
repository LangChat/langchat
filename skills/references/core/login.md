# 登录对接

对接自定义后端登录接口。

## 需要实现的接口

### 1. 登录接口

```ts
// src/api/core/auth.ts
import { requestClient } from '#/api/request';

export interface LoginParams {
  password: string;
  username: string;
}

export interface LoginResult {
  accessToken: string;
  refreshToken?: string;
}

export async function loginApi(data: LoginParams) {
  return requestClient.post<LoginResult>('/auth/login', data);
}
```

### 2. 获取用户信息接口

```ts
// src/api/core/user.ts
import { requestClient } from '#/api/request';

export interface UserInfo {
  id: number;
  username: string;
  realName: string;
  avatar: string;
  roles: string[];
}

export async function getUserInfoApi() {
  return requestClient.get<UserInfo>('/user/info');
}
```

### 3. 获取权限码接口（可选）

```ts
// src/api/core/auth.ts
export async function getAccessCodesApi() {
  return requestClient.get<string[]>('/auth/codes');
}
```

### 4. 刷新Token接口（可选）

```ts
// src/api/core/auth.ts
export async function refreshTokenApi() {
  return requestClient.post<{ accessToken: string }>('/auth/refresh-token');
}
```

## 登录页配置

```ts
// src/router/routes/core.ts
const routes = [
  {
    meta: {
      title: 'Login',
    },
    name: 'Login',
    path: '/login',
    component: () => import('#/views/_core/authentication/login.vue'),
  },
];
```

## 权限模式配置

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    // 前端模式：路由权限在前端定义
    // 后端模式：路由从后端获取
    // 混合模式：前端定义路由，后端返回权限码
    accessMode: 'frontend',
  },
});
```

## 登录过期处理

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    // 'page' - 跳转到登录页
    // 'modal' - 显示登录过期弹窗
    loginExpiredMode: 'page',
  },
});
```

## Token刷新配置

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
  const newToken = resp.data.accessToken;
  accessStore.setAccessToken(newToken);
  return newToken;
}
```

## 自定义登录页布局

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    // 'panel-left' | 'panel-right' | 'panel-top'
    authPageLayout: 'panel-right',
  },
});
```

## 登录表单配置

登录表单在 `packages/@core/layouts/src/authentication/login.vue` 中定义，可以通过覆盖组件或修改适配器来自定义：

```ts
// 自定义登录表单字段
const loginFormSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    componentProps: {
      placeholder: '请输入用户名',
    },
    fieldName: 'username',
    label: '用户名',
    rules: 'required',
  },
  {
    component: 'InputPassword',
    componentProps: {
      placeholder: '请输入密码',
    },
    fieldName: 'password',
    label: '密码',
    rules: 'required',
  },
];
```

## 第三方登录

如需对接第三方登录，可在登录页添加第三方登录按钮：

```vue
<template>
  <div class="third-party-login">
    <Button @click="handleWechatLogin">微信登录</Button>
    <Button @click="handleGithubLogin">GitHub登录</Button>
  </div>
</template>

<script setup lang="ts">
function handleWechatLogin() {
  // 跳转到微信扫码页或打开微信登录弹窗
}
</script>
```
