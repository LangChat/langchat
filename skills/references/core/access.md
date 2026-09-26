# 权限控制详细配置

## 权限模式

### 前端访问控制（frontend）

路由权限在前端固定配置，适合角色较固定的系统。

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    accessMode: 'frontend',
  },
});

// 路由配置
{
  meta: {
    authority: ['super', 'admin'],  // 指定角色
  },
}

// 登录时设置用户角色
authStore.setUserInfo({
  ...userInfo,
  roles: ['super', 'admin'],  // 必须是数组
});
```

### 后端访问控制（backend）

通过接口动态生成路由表。

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    accessMode: 'backend',
  },
});

// src/router/access.ts
async function generateAccess(options: GenerateMenuAndRoutesOptions) {
  return await generateAccessible(preferences.app.accessMode, {
    fetchMenuListAsync: async () => {
      return await getAllMenus();  // 后端返回菜单数据
    },
  });
}
```

后端菜单数据格式：

```ts
const menus = [
  {
    name: 'Dashboard',
    path: '/dashboard',
    component: '/dashboard/index',  // 视图路径
    meta: {
      title: '仪表盘',
      icon: 'mdi:view-dashboard',
      noBasicLayout: false,  // 是否不使用基础布局
    },
  },
];
```

### 混合访问控制（mixed）

同时使用前端和后端权限控制。

```ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    accessMode: 'mixed',
  },
});
```

## 按钮权限控制

### 获取权限码

```ts
// src/store/auth.ts
const accessCodes = await getAccessCodes();
accessStore.setAccessCodes(accessCodes);
// 返回格式: ['AC_100100', 'AC_100110', 'AC_100120']
```

### 组件方式

```vue
<script setup>
import { AccessControl } from '@vben/access';
</script>

<template>
  <!-- 权限码方式 -->
  <AccessControl :codes="['AC_100100']" type="code">
    <Button>有权限可见</Button>
  </AccessControl>

  <!-- 角色方式 -->
  <AccessControl :codes="['super', 'admin']">
    <Button>管理员可见</Button>
  </AccessControl>
</template>
```

### API 方式

```vue
<script setup>
import { useAccess } from '@vben/access';

const { hasAccessByCodes, hasAccessByRoles } = useAccess();
</script>

<template>
  <!-- 权限码判断 -->
  <Button v-if="hasAccessByCodes(['AC_100100'])">有权限可见</Button>

  <!-- 角色判断 -->
  <Button v-if="hasAccessByRoles(['admin'])">管理员可见</Button>

  <!-- 多权限满足其一 -->
  <Button v-if="hasAccessByCodes(['AC_100100', 'AC_100110'])">
    任一权限可见
  </Button>
</template>
```

### 指令方式

```vue
<template>
  <!-- 权限码指令 -->
  <Button v-access:code="'AC_100100'">单个权限码</Button>
  <Button v-access:code="['AC_100100', 'AC_100110']">多个权限码</Button>

  <!-- 角色指令 -->
  <Button v-access:role="'super'">单个角色</Button>
  <Button v-access:role="['super', 'admin']">多个角色</Button>
</template>
```

## 菜单可见但禁止访问

```ts
{
  meta: {
    menuVisibleWithForbidden: true,  // 菜单可见，访问跳转403
  },
}
```
