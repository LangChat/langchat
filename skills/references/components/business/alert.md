# Vben Alert 轻量提示框

提供纯 JavaScript 调用的轻量提示框，适合快速创建 `alert`、`confirm`、`prompt` 这类简单交互。

## Alert 提示框

```ts
import { alert } from '@vben/common-ui';

// 基础用法
await alert('操作成功');

// 带标题
await alert('操作成功', '提示');

// 完整配置
await alert({
  title: '提示',
  content: '操作成功',
  icon: 'success', // 'error' | 'info' | 'question' | 'success' | 'warning'
  confirmText: '确定',
  centered: true,
});
```

## Confirm 确认框

```ts
import { confirm } from '@vben/common-ui';

// 基础用法
const result = await confirm('确定要删除吗？');
if (result) {
  // 用户点击确认
}

// 完整配置
const result = await confirm({
  title: '确认删除',
  content: '删除后数据无法恢复，确定要删除吗？',
  icon: 'warning',
  confirmText: '删除',
  cancelText: '取消',
  beforeClose: async ({ isConfirm }) => {
    if (isConfirm) {
      // 返回 false 阻止关闭
      return await doDelete();
    }
    return true;
  },
});
```

## Prompt 输入框

```ts
import { prompt } from '@vben/common-ui';

// 基础用法
const value = await prompt('请输入名称：');
if (value) {
  console.log('用户输入:', value);
}

// 带默认值
const value = await prompt({
  title: '请输入名称',
  defaultValue: '默认名称',
});

// 自定义输入组件
const value = await prompt({
  title: '请选择类型',
  component: Select,
  componentProps: {
    options: [
      { label: '类型A', value: 'a' },
      { label: '类型B', value: 'b' },
    ],
  },
  defaultValue: 'a',
});
```

## useAlertContext

在自定义组件内获取弹窗上下文：

```vue
<script setup lang="ts">
import { useAlertContext } from '@vben/common-ui';

const { doConfirm, doCancel } = useAlertContext();

function handleConfirm() {
  // 触发确认操作
  doConfirm();
}

function handleCancel() {
  // 触发取消操作
  doCancel();
}
</script>
```

## Props 类型

```ts
type IconType = 'error' | 'info' | 'question' | 'success' | 'warning';

interface AlertProps {
  title?: string;
  content: Component | string;
  icon?: Component | IconType;
  confirmText?: string;
  cancelText?: string;
  showCancel?: boolean;
  centered?: boolean;
  bordered?: boolean;
  buttonAlign?: 'center' | 'end' | 'start';
  overlayBlur?: number;
  beforeClose?: (scope: { isConfirm: boolean }) => boolean | Promise<boolean>;
  footer?: Component | string;
}

interface PromptProps<T = any> extends AlertProps {
  component?: Component;
  componentProps?: Record<string, any>;
  defaultValue?: T;
  modelPropName?: string;
}
```

## 使用场景

- 简单的确认提示
- 删除操作确认
- 快速输入收集
- 不需要复杂布局的弹窗

## 与 Modal 的区别

| 特性 | Alert | Modal |
|------|-------|-------|
| 调用方式 | 纯JS调用 | 组件式 |
| 复杂度 | 简单 | 可复杂 |
| 自定义内容 | 有限 | 完全自定义 |
| 表单支持 | prompt有限 | 完整支持 |
| 适用场景 | 快速确认/提示 | 复杂弹窗业务 |
