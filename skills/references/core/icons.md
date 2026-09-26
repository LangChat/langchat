# 图标使用

框架支持多种图标使用方式。

## Iconify 图标

推荐使用 [Iconify](https://iconify.design/)，支持100+图标集。

### 基础用法

```vue
<script setup lang="ts">
import { Icon } from '@vben/icons';
</script>

<template>
  <!-- 使用 Iconify 图标 -->
  <Icon icon="mdi:home" />
  <Icon icon="carbon:user" />
  <Icon icon="ant-design:setting" />
  <Icon icon="lucide:search" />
</template>
```

### 图标大小

```vue
<template>
  <Icon icon="mdi:home" class="size-4" />
  <Icon icon="mdi:home" class="size-6" />
  <Icon icon="mdi:home" class="size-8" />
</template>
```

### 图标颜色

```vue
<template>
  <Icon icon="mdi:home" class="text-primary" />
  <Icon icon="mdi:home" class="text-red-500" />
</template>
```

## 路由菜单图标

```ts
const routes = [
  {
    meta: {
      icon: 'mdi:home',
      title: '首页',
    },
    name: 'Home',
    path: '/home',
  },
  {
    meta: {
      icon: 'carbon:user',
      title: '用户管理',
    },
    name: 'User',
    path: '/user',
  },
];
```

## 常用图标集

| 图标集 | 前缀 | 示例 |
|--------|------|------|
| Material Design | `mdi:` | `mdi:home` |
| Carbon | `carbon:` | `carbon:user` |
| Ant Design | `ant-design:` | `ant-design:setting` |
| Lucide | `lucide:` | `lucide:search` |
| Font Awesome | `fa:` | `fa:home` |
| Remix Icon | `ri:` | `ri:home-line` |

## SVG 图标

### 全局注册 SVG 图标

```ts
// 将 SVG 文件放入 src/assets/icons/ 目录
// 文件名即为图标名
```

```vue
<template>
  <svg-icon name="custom-icon" />
</template>
```

## Tailwind CSS 图标

```vue
<template>
  <div class="i-mdi-home"></div>
  <div class="i-carbon-user text-xl"></div>
</template>
```

## 图标搜索

- [Iconify 图标搜索](https://icon-sets.iconify.design/)
- [Material Design Icons](https://pictogrammers.com/library/mdi/)
- [Lucide Icons](https://lucide.dev/icons/)

## 自定义图标组件

```vue
<script setup lang="ts">
import { Icon } from '@vben/icons';

defineProps<{
  icon: string;
  size?: number;
}>();
</script>

<template>
  <Icon
    :icon="icon"
    :class="size ? `size-${size}` : 'size-5'"
  />
</template>
```
