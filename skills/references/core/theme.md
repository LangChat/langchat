# 主题定制详细配置

## CSS 变量

框架使用 CSS 变量实现主题定制，所有颜色使用 HSL 格式。

### 核心变量

```css
:root {
  /* 基础背景色 */
  --background: 0 0% 100%;
  --background-deep: 216 20.11% 95.47%;
  --foreground: 210 6% 21%;

  /* 卡片 */
  --card: 0 0% 100%;
  --card-foreground: 222.2 84% 4.9%;

  /* 弹出层 */
  --popover: 0 0% 100%;
  --popover-foreground: 222.2 84% 4.9%;

  /* 静默状态 */
  --muted: 210 40% 96.1%;
  --muted-foreground: 215.4 16.3% 46.9%;

  /* 主题色 */
  --primary: 212 100% 45%;
  --primary-foreground: 0 0% 98%;

  /* 错误色 */
  --destructive: 0 78% 68%;
  --destructive-foreground: 0 0% 98%;

  /* 成功色 */
  --success: 144 57% 58%;
  --success-foreground: 0 0% 98%;

  /* 警告色 */
  --warning: 42 84% 61%;
  --warning-foreground: 0 0% 98%;

  /* 次要色 */
  --secondary: 240 5% 96%;
  --secondary-foreground: 240 6% 10%;

  /* 强调色 */
  --accent: 240 5% 96%;
  --accent-hover: 200deg 10% 90%;
  --accent-foreground: 240 6% 10%;

  /* 边框 */
  --border: 240 5.9% 90%;

  /* 输入框 */
  --input: 240deg 5.88% 90%;
  --input-placeholder: 217 10.6% 65%;
  --input-background: 0 0% 100%;

  /* 圆角 */
  --radius: 0.5rem;

  /* 遮罩 */
  --overlay: 0deg 0% 0% / 30%;

  /* 侧边栏 */
  --sidebar: 0 0% 100%;
  --sidebar-deep: 216 20.11% 95.47%;

  /* 顶栏 */
  --header: 0 0% 100%;
}
```

### 暗色模式变量

```css
.dark {
  --background: 222.34deg 10.43% 12.27%;
  --background-deep: 220deg 13.06% 9%;
  --foreground: 0 0% 95%;

  --card: 222.34deg 10.43% 12.27%;
  --card-foreground: 210 40% 98%;

  --sidebar: 222.34deg 10.43% 12.27%;
  --sidebar-deep: 220deg 13.06% 9%;

  --header: 222.34deg 10.43% 12.27%;

  --border: 240 3.7% 15.9%;
  --input: 0deg 0% 100% / 10%;
}
```

## 修改主题色

```ts
// preferences.ts
import { defineOverridesPreferences } from '@vben/preferences';

export const overridesPreferences = defineOverridesPreferences({
  theme: {
    colorPrimary: 'hsl(212 100% 45%)',
    colorSuccess: 'hsl(144 57% 58%)',
    colorWarning: 'hsl(42 84% 61%)',
    colorDestructive: 'hsl(348 100% 61%)',
  },
});
```

## 切换暗色模式

```ts
export const overridesPreferences = defineOverridesPreferences({
  theme: {
    mode: 'dark',  // 'light' | 'dark'
  },
});
```

## 内置主题

```ts
export const overridesPreferences = defineOverridesPreferences({
  theme: {
    builtinType: 'violet',  // 使用紫色主题
  },
});
```

可用主题：
- `default` - 默认蓝色
- `violet` - 紫色
- `pink` - 粉色
- `rose` - 玫瑰色
- `sky-blue` - 天蓝色
- `deep-blue` - 深蓝色
- `green` - 绿色
- `deep-green` - 深绿色
- `orange` - 橙色
- `yellow` - 黄色
- `zinc` - 锌灰色
- `neutral` - 中性色
- `slate` - 石板色
- `gray` - 灰色
- `custom` - 自定义

## 自定义主题

1. 在 `preferences.ts` 设置：

```ts
export const overridesPreferences = defineOverridesPreferences({
  theme: {
    builtinType: 'my-theme',
  },
});
```

2. 在 CSS 文件中定义变量：

```css
/* light 模式 */
[data-theme='my-theme'] {
  --primary: 262.1 83.3% 57.8%;
  --primary-foreground: 210 20% 98%;
  --background: 0 0% 100%;
  --foreground: 224 71.4% 4.1%;
  /* ... 其他变量 */
}

/* dark 模式 */
.dark[data-theme='my-theme'],
[data-theme='my-theme'] .dark {
  --primary-foreground: 210 20% 98%;
  --background: 224 71.4% 4.1%;
  --foreground: 210 20% 98%;
  /* ... 其他变量 */
}
```

## 特殊模式

### 灰色模式

```ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    colorGrayMode: true,
  },
});
```

### 色弱模式

```ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    colorWeakMode: true,
  },
});
```

## 自定义侧边栏/顶栏颜色

```css
:root {
  --sidebar: 0 0% 100%;
  --header: 0 0% 100%;
}

.dark {
  --sidebar: 222.34deg 10.43% 12.27%;
  --header: 222.34deg 10.43% 12.27%;
}
```

## 水印功能

```ts
export const overridesPreferences = defineOverridesPreferences({
  app: {
    watermark: true,
  },
});

// 动态更新水印内容
import { useWatermark } from '@vben/hooks';

const { updateWatermark } = useWatermark();
await updateWatermark({
  content: 'hello watermark',
});
```
