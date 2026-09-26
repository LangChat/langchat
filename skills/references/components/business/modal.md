# Vben Modal 模态框

框架提供的模态框组件，支持拖拽、全屏、自动高度、loading等功能。

## 基础用法

```vue
<script setup lang="ts">
import { useVbenModal } from '#/adapter';

const [Modal, modalApi] = useVbenModal({
  title: '标题',
  onConfirm: () => {
    console.log('确认');
    modalApi.close();
  },
});
</script>

<template>
  <Button @click="modalApi.open()">打开</Button>
  <Modal>
    弹窗内容
  </Modal>
</template>
```

## 组件抽离

```vue
<!-- parent.vue -->
<script setup lang="ts">
import { useVbenModal } from '#/adapter';
import ChildForm from './child-form.vue';

const [Modal, modalApi] = useVbenModal({
  connectedComponent: ChildForm,
  onConfirm: () => {
    const data = modalApi.getData();
    console.log(data);
  },
});
</script>

<template>
  <Button @click="modalApi.open()">打开</Button>
  <Modal />
</template>

<!-- child-form.vue -->
<script setup lang="ts">
import { useVbenModal } from '#/adapter';

const [Form, formApi] = useVbenForm({...});
const [Modal, modalApi] = useVbenModal({
  onOpenChange: (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData();
      formApi.setValues(data);
    }
  },
  onConfirm: async () => {
    const values = await formApi.validateAndSubmitForm();
    modalApi.setData(values);
    modalApi.close();
  },
});
</script>
```

## 拖拽功能

```vue
<script setup lang="ts">
const [Modal, modalApi] = useVbenModal({
  draggable: true,
});
</script>
```

## 全屏功能

```vue
<script setup lang="ts">
const [Modal, modalApi] = useVbenModal({
  fullscreen: true,           // 默认全屏
  fullscreenButton: true,     // 显示全屏按钮
});
</script>
```

## Loading 状态

```vue
<script setup lang="ts">
const [Modal, modalApi] = useVbenModal({
  onConfirm: async () => {
    modalApi.setState({ loading: true });
    try {
      await saveData();
      modalApi.close();
    } finally {
      modalApi.setState({ loading: false });
    }
  },
});
</script>
```

## Lock 锁定状态

```vue
<script setup lang="ts">
const [Modal, modalApi] = useVbenModal({
  onConfirm: async () => {
    modalApi.lock();
    try {
      await saveData();
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
});
</script>
```

## 动画类型

```vue
<script setup lang="ts">
// 滑动动画（默认）
const [Modal, modalApi] = useVbenModal({
  animationType: 'slide',
});

// 缩放动画
const [Modal, modalApi] = useVbenModal({
  animationType: 'scale',
});
</script>
```

## 挂载到内容区域

```vue
<script setup lang="ts">
const [Modal, modalApi] = useVbenModal({
  appendToMain: true, // 挂载到内容区域，不遮挡导航菜单
});
</script>

<template>
  <!-- 需要设置 auto-content-height -->
  <Page auto-content-height>
    <Modal />
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
| fullscreen | 全屏显示 | `boolean` | `false` |
| fullscreenButton | 显示全屏按钮 | `boolean` | `true` |
| draggable | 可拖拽 | `boolean` | `false` |
| closable | 显示关闭按钮 | `boolean` | `true` |
| centered | 居中显示 | `boolean` | `false` |
| modal | 显示遮罩 | `boolean` | `true` |
| header | 显示header | `boolean` | `true` |
| footer | 显示footer | `boolean` | `true` |
| confirmLoading | 确认按钮loading | `boolean` | `false` |
| confirmDisabled | 禁用确认按钮 | `boolean` | `false` |
| closeOnClickModal | 点击遮罩关闭 | `boolean` | `true` |
| closeOnPressEscape | ESC关闭 | `boolean` | `true` |
| confirmText | 确认按钮文本 | `string` | `确认` |
| cancelText | 取消按钮文本 | `string` | `取消` |
| showCancelButton | 显示取消按钮 | `boolean` | `true` |
| showConfirmButton | 显示确认按钮 | `boolean` | `true` |
| class | modal的class（宽度） | `string` | - |
| contentClass | 内容区class | `string` | - |
| footerClass | 底部区class | `string` | - |
| headerClass | 顶部区class | `string` | - |
| bordered | 显示border | `boolean` | `false` |
| zIndex | ZIndex层级 | `number` | `1000` |
| overlayBlur | 遮罩模糊度 | `number` | - |
| animationType | 动画类型 | `'slide' \| 'scale'` | `slide` |
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
| default | 弹窗内容 |
| prepend-footer | 取消按钮左侧 |
| center-footer | 取消和确认中间 |
| append-footer | 确认按钮右侧 |

## modalApi 方法

| 方法 | 描述 | 类型 |
|------|------|------|
| open | 打开弹窗 | `() => void` |
| close | 关闭弹窗 | `() => void` |
| setState | 设置状态 | `(state) => modalApi` |
| setData | 设置共享数据 | `<T>(data: T) => modalApi` |
| getData | 获取共享数据 | `<T>() => T` |
| useStore | 获取响应式状态 | - |
| lock | 锁定弹窗 | `(isLock?: boolean) => modalApi` |
| unlock | 解锁弹窗 | `() => modalApi` |

## 设置默认属性

```ts
// apps/<app>/src/bootstrap.ts
import { setDefaultModalProps } from '@vben/common-ui';

setDefaultModalProps({
  zIndex: 2000,
  draggable: true,
  fullscreenButton: false,
});
```

## 设置宽度

```vue
<script setup lang="ts">
const [Modal, modalApi] = useVbenModal({
  class: 'w-[600px]',  // Tailwind CSS
});
</script>
```
