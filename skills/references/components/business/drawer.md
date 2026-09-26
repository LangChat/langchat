# Vben Drawer 抽屉

框架提供的抽屉组件，支持自动高度、loading等功能。

## 基础用法

```vue
<script setup lang="ts">
import { useVbenDrawer } from '#/adapter';

const [Drawer, drawerApi] = useVbenDrawer({
  title: '标题',
  onConfirm: () => {
    console.log('确认');
    drawerApi.close();
  },
});
</script>

<template>
  <Button @click="drawerApi.open()">打开</Button>
  <Drawer>
    抽屉内容
  </Drawer>
</template>
```

## 组件抽离

```vue
<!-- parent.vue -->
<script setup lang="ts">
import { useVbenDrawer } from '#/adapter';
import ChildForm from './child-form.vue';

const [Drawer, drawerApi] = useVbenDrawer({
  connectedComponent: ChildForm,
  onConfirm: () => {
    const data = drawerApi.getData();
    console.log(data);
  },
});
</script>

<template>
  <Button @click="drawerApi.open()">打开</Button>
  <Drawer />
</template>

<!-- child-form.vue -->
<script setup lang="ts">
import { useVbenDrawer } from '#/adapter';

const [Form, formApi] = useVbenForm({...});
const [Drawer, drawerApi] = useVbenDrawer({
  onOpenChange: (isOpen) => {
    if (isOpen) {
      const data = drawerApi.getData();
      formApi.setValues(data);
    }
  },
  onConfirm: async () => {
    const values = await formApi.validateAndSubmitForm();
    drawerApi.setData(values);
    drawerApi.close();
  },
});
</script>
```

## 弹出位置

```vue
<script setup lang="ts">
// 左侧弹出
const [Drawer, drawerApi] = useVbenDrawer({
  placement: 'left',
});

// 右侧弹出（默认）
const [Drawer, drawerApi] = useVbenDrawer({
  placement: 'right',
});

// 顶部弹出
const [Drawer, drawerApi] = useVbenDrawer({
  placement: 'top',
});

// 底部弹出
const [Drawer, drawerApi] = useVbenDrawer({
  placement: 'bottom',
});
</script>
```

## Loading 状态

```vue
<script setup lang="ts">
const [Drawer, drawerApi] = useVbenDrawer({
  onConfirm: async () => {
    drawerApi.setState({ loading: true });
    try {
      await saveData();
      drawerApi.close();
    } finally {
      drawerApi.setState({ loading: false });
    }
  },
});
</script>
```

## Lock 锁定状态

```vue
<script setup lang="ts">
const [Drawer, drawerApi] = useVbenDrawer({
  onConfirm: async () => {
    drawerApi.lock();
    try {
      await saveData();
      drawerApi.close();
    } finally {
      drawerApi.unlock();
    }
  },
});
</script>
```

## 挂载到内容区域

```vue
<script setup lang="ts">
const [Drawer, drawerApi] = useVbenDrawer({
  appendToMain: true, // 挂载到内容区域，不遮挡导航菜单
});
</script>

<template>
  <!-- 需要设置 auto-content-height -->
  <Page auto-content-height>
    <Drawer />
  </Page>
</template>
```

## Props 属性

| 属性名 | 描述 | 类型 | 默认值 |
|--------|------|------|--------|
| title | 标题 | `string` | - |
| titleTooltip | 标题提示 | `string` | - |
| description | 描述信息 | `string` | - |
| isOpen | 打开状态 | `boolean` | `false` |
| loading | 加载状态 | `boolean` | `false` |
| closable | 显示关闭按钮 | `boolean` | `true` |
| closeIconPlacement | 关闭按钮位置 | `'left' \| 'right'` | `right` |
| modal | 显示遮罩 | `boolean` | `true` |
| header | 显示header | `boolean` | `true` |
| footer | 显示footer | `boolean` | `true` |
| confirmLoading | 确认按钮loading | `boolean` | `false` |
| closeOnClickModal | 点击遮罩关闭 | `boolean` | `true` |
| closeOnPressEscape | ESC关闭 | `boolean` | `true` |
| confirmText | 确认按钮文本 | `string` | `确认` |
| cancelText | 取消按钮文本 | `string` | `取消` |
| showCancelButton | 显示取消按钮 | `boolean` | `true` |
| showConfirmButton | 显示确认按钮 | `boolean` | `true` |
| placement | 弹出位置 | `'left' \| 'right' \| 'top' \| 'bottom'` | `right` |
| class | drawer的class | `string` | - |
| zIndex | ZIndex层级 | `number` | `1000` |
| overlayBlur | 遮罩模糊度 | `number` | - |
| connectedComponent | 连接组件 | `Component` | - |
| destroyOnClose | 关闭时销毁 | `boolean` | `false` |
| appendToMain | 挂载到内容区 | `boolean` | `false` |

## Event 事件

| 事件名 | 描述 | 类型 |
|--------|------|------|
| onBeforeClose | 关闭前触发 | `() => boolean \| Promise<boolean>` |
| onCancel | 取消按钮触发 | `() => void` |
| onConfirm | 确认按钮触发 | `() => void` |
| onOpenChange | 打开/关闭时触发 | `(isOpen: boolean) => void` |
| onOpened | 打开动画完毕 | `() => void` |
| onClosed | 关闭动画完毕 | `() => void` |

## 插槽

| 插槽名 | 描述 |
|--------|------|
| default | 抽屉内容 |
| prepend-footer | 取消按钮左侧 |
| center-footer | 取消和确认中间 |
| append-footer | 确认按钮右侧 |
| close-icon | 关闭按钮图标 |
| extra | 额外内容(标题右侧) |

## drawerApi 方法

| 方法 | 描述 | 类型 |
|------|------|------|
| open | 打开抽屉 | `() => void` |
| close | 关闭抽屉 | `() => void` |
| setState | 设置状态 | `(state) => drawerApi` |
| setData | 设置共享数据 | `<T>(data: T) => drawerApi` |
| getData | 获取共享数据 | `<T>() => T` |
| useStore | 获取响应式状态 | - |
| lock | 锁定抽屉 | `(isLock?: boolean) => drawerApi` |
| unlock | 解锁抽屉 | `() => drawerApi` |

## 设置默认属性

```ts
// apps/<app>/src/bootstrap.ts
import { setDefaultDrawerProps } from '@vben/common-ui';

setDefaultDrawerProps({
  zIndex: 2000,
  placement: 'left',
});
```
