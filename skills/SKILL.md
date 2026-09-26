---
name: vben
description: Vben Admin 5.0 前端框架开发技能。用于开发基于 Vue3、Vite、TypeScript 的中后台管理系统。
TRIGGER when: 编写/修改 Vben Admin 项目代码、配置路由菜单、设置权限控制、主题定制、组件开发、API对接、状态管理、国际化配置。
DO NOT trigger when: 编写后端代码、纯前端通用开发（与 Vben 框架无关）。
---

# Vben Admin 开发技能

基于 Vben Admin 5.0 文档的专业开发指导技能，帮助快速开发中后台管理系统。

## 项目结构

采用 Monorepo 架构，核心目录：

```
├── apps/                    # 应用目录
│   ├── web-antd/           # Ant Design Vue 应用
│   ├── web-ele/            # Element Plus 应用
│   ├── web-naive/          # Naive UI 应用
│   └── backend-mock/       # Mock 后端服务
├── packages/               # 共享包
│   ├── @core/              # 核心包（UI组件、布局等）
│   ├── effects/            # 副作用包（权限、请求、hooks等）
│   ├── stores/             # 状态管理
│   ├── locales/            # 国际化
│   └── utils/              # 工具函数
└── internal/               # 内部工具配置
```

## 常用命令

```bash
# 开发
pnpm dev:antd          # 启动 Ant Design 应用
pnpm dev:ele           # 启动 Element Plus 应用
pnpm dev:naive         # 启动 Naive UI 应用

# 构建
pnpm build             # 构建所有应用
pnpm build:antd        # 构建指定应用

# 其他
pnpm lint              # 代码检查
pnpm check:type        # 类型检查
pnpm reinstall         # 重新安装依赖
```

## 核心开发指南

### 路由与菜单

路由配置位于 `src/router/routes/modules/` 目录：

```ts
import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'mdi:home',
      title: $t('page.home.title'),
      authority: ['admin'],      // 权限控制
      order: 1000,               // 菜单排序
      keepAlive: true,           // 开启缓存
      hideInMenu: false,         // 菜单中隐藏
      hideInTab: false,          // 标签页中隐藏
    },
    name: 'Home',
    path: '/home',
    component: () => import('#/views/home/index.vue'),
  },
];

export default routes;
```

### 权限控制

三种模式在 `preferences.ts` 中配置：

```ts
import { defineOverridesPreferences } from '@vben/preferences';

export const overridesPreferences = defineOverridesPreferences({
  app: {
    accessMode: 'frontend',  // 'frontend' | 'backend' | 'mixed'
  },
});
```

按钮级权限：

```vue
<script setup>
import { AccessControl, useAccess } from '@vben/access';
const { hasAccessByCodes, hasAccessByRoles } = useAccess();
</script>

<template>
  <!-- 权限码方式 -->
  <AccessControl :codes="['AC_100100']" type="code">
    <Button>有权限可见</Button>
  </AccessControl>

  <!-- 角色方式 -->
  <Button v-if="hasAccessByRoles(['admin'])">管理员可见</Button>

  <!-- 指令方式 -->
  <Button v-access:code="'AC_100100'">有权限可见</Button>
</template>
```

### 偏好设置配置

在应用目录的 `preferences.ts` 中配置：

```ts
import { defineOverridesPreferences } from '@vben/preferences';

export const overridesPreferences = defineOverridesPreferences({
  app: {
    layout: 'sidebar-nav',           // 布局方式
    locale: 'zh-CN',                 // 语言
    dynamicTitle: true,              // 动态标题
    watermark: false,                // 水印
    loginExpiredMode: 'page',        // 登录过期模式
  },
  theme: {
    mode: 'dark',                    // 主题模式
    builtinType: 'default',          // 内置主题
    colorPrimary: 'hsl(212 100% 45%)', // 主题色
  },
  sidebar: {
    collapsed: false,                // 侧边栏折叠
    width: 224,                      // 侧边栏宽度
  },
  tabbar: {
    enable: true,                    // 标签页
    keepAlive: true,                 // 缓存
  },
});
```

### API 请求

请求配置在 `src/api/request.ts`：

```ts
import { requestClient } from '#/api/request';

// GET 请求
export async function getUserInfoApi() {
  return requestClient.get<UserInfo>('/user/info');
}

// POST 请求
export async function saveUserApi(user: UserInfo) {
  return requestClient.post<UserInfo>('/user', user);
}
```

代理配置在 `vite.config.mts`：

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
          },
        },
      },
    },
  };
});
```

### 国际化

使用 `$t()` 函数：

```ts
import { $t } from '#/locales';

meta: {
  title: $t('page.home.title'),
}
```

语言文件在 `packages/locales/` 目录。

### 主题定制

CSS 变量覆盖：

```css
:root {
  --primary: 212 100% 45%;
  --sidebar: 0 0% 100%;
  --header: 0 0% 100%;
}

.dark {
  --background: 222.34deg 10.43% 12.27%;
  --sidebar: 222.34deg 10.43% 12.27%;
}
```

### 登录接口对接

需要的接口：

```ts
// src/api/core/auth.ts
export async function loginApi(data: LoginParams) {
  return requestClient.post<LoginResult>('/auth/login', data);
}

// src/api/core/user.ts
export async function getUserInfoApi() {
  return requestClient.get<UserInfo>('/user/info');
}

// src/api/core/auth.ts (可选)
export async function getAccessCodesApi() {
  return requestClient.get<string[]>('/auth/codes');
}
```

## 环境变量

```bash
# .env.development
VITE_PORT=5555
VITE_GLOB_API_URL=/api
VITE_NITRO_MOCK=true

# .env.production
VITE_GLOB_API_URL=https://api.example.com
VITE_COMPRESS=gzip
VITE_ROUTER_HISTORY=hash
```

## 别名配置

使用 `#` 开头的路径别名：

```ts
// package.json
{
  "imports": {
    "#/*": "./src/*"
  }
}

// 使用
import { useAuthStore } from '#/store';
```

## 详细参考文档

需要更多细节时，查阅以下参考文件：

### 核心功能 (references/core/)
- **路由菜单**: `references/core/route.md` - 路由配置、Meta属性、多级菜单
- **权限控制**: `references/core/access.md` - 三种权限模式、按钮级权限
- **偏好设置**: `references/core/preferences.md` - 完整配置项说明
- **主题定制**: `references/core/theme.md` - CSS变量、内置主题、自定义主题
- **API请求**: `references/core/api.md` - 请求配置、拦截器、多接口地址
- **国际化**: `references/core/locale.md` - 语言配置、新增语言包
- **登录对接**: `references/core/login.md` - 登录接口、Token刷新
- **图标使用**: `references/core/icons.md` - Iconify图标、SVG图标

### 业务组件 (references/components/business/)
- **Page页面**: `references/components/business/page.md` - 页面布局容器、标题区、内容区
- **表单组件**: `references/components/business/form.md` - Vben Form表单配置、校验、联动
- **表格组件**: `references/components/business/table.md` - Vben Vxe Table表格配置、搜索、远程加载
- **模态框**: `references/components/business/modal.md` - Vben Modal配置、拖拽、全屏
- **抽屉**: `references/components/business/drawer.md` - Vben Drawer配置、组件抽离
- **轻量提示框**: `references/components/business/alert.md` - alert、confirm、prompt调用
- **API组件包装器**: `references/components/business/api-component.md` - 远程数据自动加载

### 通用组件 (references/components/common/)
- **数字动画**: `references/components/common/count-to.md` - CountToAnimator数字滚动动画
- **省略文本**: `references/components/common/ellipsis-text.md` - EllipsisText文本省略展开

### 功能配置 (references/features/)
- **常用功能**: `references/features/features.md` - 水印、缓存、动态标题等

### 构建部署 (references/deployment/)
- **构建部署**: `references/deployment/deploy.md` - 构建配置、Nginx、Docker
- **常见问题**: `references/deployment/faq.md` - 依赖安装、打包部署、错误排查
