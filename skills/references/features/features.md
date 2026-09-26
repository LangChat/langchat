# 常用功能

## 动态标题

根据页面内容动态更新浏览器标题。

### 开启动态标题

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    dynamicTitle: true,
  },
});
```

### 路由配置标题

```ts
const routes = [
  {
    meta: {
      title: '用户管理',
    },
    name: 'User',
    path: '/user',
  },
];
```

### 页面内设置标题

```ts
import { useTabbar } from '@vben/tabbar';

const { setTitle } = useTabbar();

// 动态设置标题
setTitle('用户详情 - ID: 123');
```

## 水印

为页面添加水印保护。

### 开启水印

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    watermark: true,
  },
});
```

### 自定义水印内容

```vue
<script setup lang="ts">
import { useWatermark } from '@vben/common-ui';

const { setWatermark } = useWatermark();

setWatermark({
  content: '机密文档',
  fontSize: 16,
  color: 'rgba(0, 0, 0, 0.15)',
});
</script>
```

## 页面缓存

保持页面状态，切换路由时不销毁组件。

### 路由级别缓存

```ts
const routes = [
  {
    meta: {
      keepAlive: true,  // 开启缓存
    },
    name: 'UserList',
    path: '/user/list',
  },
];
```

### 全局配置

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  tabbar: {
    enable: true,
    keepAlive: true,  // 全局开启缓存
    persist: true,    // 持久化标签页
  },
});
```

## 页面加载进度条

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  transition: {
    progress: true,  // 显示页面加载进度条
    loading: true,   // 显示页面加载动画
  },
});
```

## 页面过渡动画

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  transition: {
    enable: true,
    name: 'fade-slide',  // 动画名称
  },
});
```

### 可选动画

- `fade` - 淡入淡出
- `fade-slide` - 滑动淡入淡出
- `fade-bottom` - 底部滑入
- `fade-scale` - 缩放淡入

## 面包屑

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  breadcrumb: {
    enable: true,        // 显示面包屑
    showHome: false,     // 显示首页图标
    showIcon: true,      // 显示图标
    hideOnlyOne: false,  // 只有一个时隐藏
    styleType: 'normal', // 样式类型
  },
});
```

## 标签页

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  tabbar: {
    enable: true,           // 显示标签页
    height: 38,             // 标签页高度
    showIcon: true,         // 显示图标
    showMore: true,         // 显示更多按钮
    showMaximize: true,     // 显示最大化按钮
    draggable: true,        // 可拖拽
    wheelable: true,        // 滚轮切换
    persist: true,          // 持久化
    keepAlive: true,        // 缓存
    maxCount: 0,            // 最大数量（0不限制）
    styleType: 'chrome',    // 样式：chrome | plain | card
  },
});
```

## 页脚版权

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  footer: {
    enable: true,   // 显示页脚
    fixed: false,   // 固定在底部
    height: 32,     // 高度
  },
  copyright: {
    enable: true,
    companyName: 'My Company',
    companySiteLink: 'https://example.com',
    date: '2024',
    icp: '备案号',
    icpLink: 'https://beian.miit.gov.cn',
  },
});
```

## 锁屏

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  widget: {
    lockScreen: true,  // 显示锁屏按钮
  },
  shortcutKeys: {
    globalLockScreen: true,  // 锁屏快捷键
  },
});
```

## 全局搜索

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  widget: {
    globalSearch: true,  // 显示搜索按钮
  },
  shortcutKeys: {
    globalSearch: true,  // 搜索快捷键
  },
});
```

## 通知中心

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  widget: {
    notification: true,  // 显示通知图标
  },
});
```

## 全屏

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  widget: {
    fullscreen: true,  // 显示全屏按钮
  },
});
```

## 刷新按钮

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  widget: {
    refresh: true,  // 显示刷新按钮
  },
});
```

## 主题切换

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  widget: {
    themeToggle: true,  // 显示主题切换按钮
  },
});
```

## 检查更新

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    enableCheckUpdates: true,      // 开启检查更新
    checkUpdatesInterval: 1,       // 检查间隔（小时）
  },
});
```

## 色弱模式

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    colorWeakMode: true,
  },
});
```

## 灰色模式

```ts
// preferences.ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    colorGrayMode: true,
  },
});
```
