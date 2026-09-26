# 路由与菜单详细配置

## 路由类型

### 核心路由
框架内置路由，位于 `src/router/routes/core/`，包含根路由、登录路由、404路由等。

### 静态路由
位于 `src/router/routes/index/`，项目启动时已确定的路由。

### 动态路由
位于 `src/router/routes/modules/`，根据用户权限动态生成。

## 路由 Meta 配置

```ts
interface RouteMeta {
  // 页面标题（必填）
  title: string;

  // 菜单/标签页图标
  icon?: string;

  // 激活图标
  activeIcon?: string;

  // 菜单排序（仅一级菜单有效）
  order?: number;

  // 开启 KeepAlive 缓存
  keepAlive?: boolean;

  // 在菜单中隐藏
  hideInMenu?: boolean;

  // 在标签页中隐藏
  hideInTab?: boolean;

  // 在面包屑中隐藏
  hideInBreadcrumb?: boolean;

  // 子菜单在菜单中隐藏
  hideChildrenInMenu?: boolean;

  // 权限控制
  authority?: string[];

  // 忽略权限，直接访问
  ignoreAccess?: boolean;

  // 菜单可见但禁止访问（跳转403）
  menuVisibleWithForbidden?: boolean;

  // 固定标签页
  affixTab?: boolean;

  // 固定标签页顺序
  affixTabOrder?: number;

  // 标签页最大打开数量
  maxNumOfOpenTab?: number;

  // 徽标
  badge?: string;

  // 徽标类型 'dot' | 'normal'
  badgeType?: 'dot' | 'normal';

  // 徽标颜色
  badgeVariants?: 'default' | 'destructive' | 'primary' | 'success' | 'warning';

  // 外链跳转路径
  link?: string;

  // 在新窗口打开
  openInNewWindow?: boolean;

  // iframe 地址
  iframeSrc?: string;

  // 激活指定菜单路径
  activePath?: string;

  // 不使用基础布局
  noBasicLayout?: boolean;

  // 完整路径作为 tab key
  fullPathKey?: boolean;

  // DOM 缓存（解决复杂页面切换卡顿）
  domCached?: boolean;
}
```

## 新增页面示例

1. 添加路由文件 `src/router/routes/modules/home.ts`：

```ts
import type { RouteRecordRaw } from 'vue-router';

import { $t } from '#/locales';

const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'mdi:home',
      title: $t('page.home.title'),
      order: 1000,
    },
    name: 'Home',
    path: '/home',
    redirect: '/home/index',
    children: [
      {
        name: 'HomeIndex',
        path: '/home/index',
        component: () => import('#/views/home/index.vue'),
        meta: {
          icon: 'mdi:home',
          title: $t('page.home.index'),
        },
      },
    ],
  },
];

export default routes;
```

2. 添加页面组件 `src/views/home/index.vue`：

```vue
<template>
  <div>
    <h1>Home Page</h1>
  </div>
</template>
```

## 多级路由示例

```ts
const routes: RouteRecordRaw[] = [
  {
    meta: {
      icon: 'ic:baseline-view-in-ar',
      title: '多级菜单',
    },
    name: 'Nested',
    path: '/nested',
    redirect: '/nested/menu1',
    children: [
      {
        name: 'Menu1',
        path: '/nested/menu1',
        component: () => import('#/views/nested/menu1.vue'),
        meta: { title: '菜单1' },
      },
      {
        name: 'Menu2',
        path: '/nested/menu2',
        meta: { title: '菜单2' },
        redirect: '/nested/menu2/menu2-1',
        children: [
          {
            name: 'Menu21',
            path: '/nested/menu2/menu2-1',
            component: () => import('#/views/nested/menu2-1.vue'),
            meta: { title: '菜单2-1' },
          },
        ],
      },
    ],
  },
];
```

## 路由刷新

```ts
import { useRefresh } from '@vben/hooks';

const { refresh } = useRefresh();
refresh(); // 刷新当前路由
```

## 标签页控制

标签页使用唯一 key 标识，优先级：

1. 路由 query 参数 `pageKey`
2. 路由完整路径（`fullPathKey` 不为 false 时）
3. 路由 path（`fullPathKey` 为 false 时）

```ts
// 使用 pageKey 打开多个标签页
router.push({
  path: '/detail',
  query: { pageKey: 'unique-id' },
});
```
