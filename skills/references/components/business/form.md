# Vben Form 表单

框架提供的表单组件，基于 [vee-validate](https://vee-validate.logaretm.com/v4/) 进行表单验证，支持多UI框架适配。

## 基础用法

```vue
<script setup lang="ts">
import { useVbenForm } from '#/adapter/form';

const [Form, formApi] = useVbenForm({
  schema: [
    {
      component: 'Input',
      fieldName: 'name',
      label: '姓名',
      rules: 'required',
    },
    {
      component: 'InputNumber',
      fieldName: 'age',
      label: '年龄',
    },
    {
      component: 'Select',
      fieldName: 'status',
      label: '状态',
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ],
      },
    },
  ],
});
</script>

<template>
  <Form />
</template>
```

## 表单提交

```vue
<script setup lang="ts">
import { useVbenForm } from '#/adapter/form';

const [Form, formApi] = useVbenForm({
  schema: [...],
  handleSubmit: async (values) => {
    console.log('提交数据:', values);
    // 调用API保存数据
  },
  handleReset: () => {
    console.log('重置表单');
  },
});

// 手动提交
async function submit() {
  const values = await formApi.validateAndSubmitForm();
  console.log(values);
}
</script>
```

## 表单校验

### 预定义规则

```ts
const schema = [
  {
    component: 'Input',
    fieldName: 'name',
    label: '姓名',
    rules: 'required', // 必填
  },
  {
    component: 'Select',
    fieldName: 'type',
    label: '类型',
    rules: 'selectRequired', // 下拉必选
  },
];
```

### Zod 校验

```ts
import { z } from '#/adapter/form';

const schema = [
  {
    component: 'Input',
    fieldName: 'email',
    label: '邮箱',
    rules: z.string().email({ message: '请输入正确的邮箱' }),
  },
  {
    component: 'Input',
    fieldName: 'password',
    label: '密码',
    rules: z.string().min(6, { message: '密码至少6位' }),
  },
  {
    component: 'Input',
    fieldName: 'phone',
    label: '手机号',
    rules: z.string().regex(/^1[3-9]\d{9}$/, { message: '请输入正确的手机号' }),
  },
];
```

## 表单联动

```ts
const schema = [
  {
    component: 'Select',
    fieldName: 'type',
    label: '类型',
    componentProps: {
      options: [
        { label: '个人', value: 'personal' },
        { label: '企业', value: 'company' },
      ],
    },
  },
  {
    component: 'Input',
    fieldName: 'companyName',
    label: '企业名称',
    dependencies: {
      triggerFields: ['type'],
      // 显示条件
      show: (values) => values.type === 'company',
      // 必填条件
      required: (values) => values.type === 'company',
      // 动态组件参数
      componentProps: (values) => ({
        placeholder: values.type === 'company' ? '请输入企业名称' : '',
      }),
    },
  },
];
```

## 查询表单

```vue
<script setup lang="ts">
import { useVbenForm } from '#/adapter/form';

const [Form, formApi] = useVbenForm({
  schema: [...],
  // 查询表单不触发验证
  handleSubmit: (values) => {
    emit('search', values);
  },
  // 字段变化时提交（防抖）
  submitOnChange: true,
  // 显示折叠按钮
  showCollapseButton: true,
  collapsedRows: 1,
});
</script>
```

## 表单操作

```vue
<script setup lang="ts">
const [Form, formApi] = useVbenForm({...});

// 获取表单值
async function getValues() {
  const values = await formApi.getValues();
  console.log(values);
}

// 设置表单值
async function setValues() {
  await formApi.setValues({
    name: '张三',
    age: 18,
  });
}

// 设置单个字段值
formApi.setFieldValue('name', '李四');

// 重置表单
formApi.resetForm();

// 验证表单
try {
  await formApi.validate();
} catch (errors) {
  console.log('验证失败:', errors);
}

// 更新schema
formApi.updateSchema([
  {
    fieldName: 'name',
    label: '新标签',
  },
]);

// 获取字段组件实例
const inputRef = formApi.getFieldComponentRef('name');
</script>
```

## Props 属性

| 属性名 | 描述 | 类型 | 默认值 |
|--------|------|------|--------|
| layout | 表单布局 | `'horizontal' \| 'vertical' \| 'inline'` | `horizontal` |
| schema | 表单配置 | `FormSchema[]` | - |
| commonConfig | 通用配置 | `FormCommonConfig` | - |
| showDefaultActions | 显示默认操作按钮 | `boolean` | `true` |
| showCollapseButton | 显示折叠按钮 | `boolean` | `false` |
| collapsedRows | 折叠时显示的行数 | `number` | `1` |
| handleSubmit | 提交回调 | `(values) => void` | - |
| handleReset | 重置回调 | `() => void` | - |
| handleValuesChange | 值变化回调 | `(values, fieldsChanged) => void` | - |
| submitOnEnter | 回车提交 | `boolean` | `false` |
| submitOnChange | 字段变化提交 | `boolean` | `false` |

## FormSchema 配置

```ts
interface FormSchema {
  component: Component | string;      // 组件
  componentProps?: object;            // 组件参数
  defaultValue?: any;                 // 默认值
  dependencies?: FormItemDependencies; // 依赖联动
  description?: string;               // 描述
  fieldName: string;                  // 字段名
  help?: string;                      // 帮助信息
  hide?: boolean;                     // 隐藏
  label?: string;                     // 标签
  rules?: string | ZodSchema;         // 校验规则
  suffix?: string;                    // 后缀
}
```

## 组件类型

```ts
type ComponentType =
  | 'Input'          // 输入框
  | 'InputNumber'    // 数字输入框
  | 'InputPassword'  // 密码输入框
  | 'Textarea'       // 文本域
  | 'Select'         // 下拉选择
  | 'TreeSelect'     // 树选择
  | 'RadioGroup'     // 单选组
  | 'CheckboxGroup'  // 多选组
  | 'Checkbox'       // 复选框
  | 'Switch'         // 开关
  | 'DatePicker'     // 日期选择
  | 'RangePicker'    // 日期范围
  | 'TimePicker'     // 时间选择
  | 'Upload'         // 上传
  | 'Rate'           // 评分
  | 'AutoComplete'   // 自动完成
  | 'Divider'        // 分割线
  | 'Space';         // 间距
```

## 时间字段映射

```ts
const [Form, formApi] = useVbenForm({
  schema: [
    {
      component: 'RangePicker',
      fieldName: 'timeRange',
      label: '时间范围',
    },
  ],
  // 将 timeRange 映射到 startTime 和 endTime
  fieldMappingTime: [
    ['timeRange', ['startTime', 'endTime'], 'YYYY-MM-DD HH:mm:ss'],
  ],
});
```

## 插槽

| 插槽名 | 描述 |
|--------|------|
| reset-before | 重置按钮之前 |
| submit-before | 提交按钮之前 |
| expand-before | 展开按钮之前 |
| expand-after | 展开按钮之后 |
| {fieldName} | 字段自定义插槽 |

## 自定义组件

```vue
<script setup lang="ts">
import MyCustomComponent from './MyCustomComponent.vue';

const [Form, formApi] = useVbenForm({
  schema: [
    {
      component: MyCustomComponent,
      fieldName: 'custom',
      label: '自定义组件',
      componentProps: {
        placeholder: '请输入',
      },
    },
  ],
});
</script>

<template>
  <Form>
    <template #custom="slotProps">
      <MyCustomComponent v-bind="slotProps" />
    </template>
  </Form>
</template>
```

## 适配器配置

```ts
// src/adapter/form.ts
import { setupVbenForm, useVbenForm as useForm } from '@vben/common-ui';
import { $t } from '@vben/locales';

setupVbenForm({
  config: {
    baseModelPropName: 'value',
    emptyStateValue: null,
    modelPropNameMap: {
      Checkbox: 'checked',
      Switch: 'checked',
      Upload: 'fileList',
    },
  },
  defineRules: {
    required: (value, _params, ctx) => {
      if (value === undefined || value === null || value.length === 0) {
        return $t('ui.formRules.required', [ctx.label]);
      }
      return true;
    },
  },
});

export const useVbenForm = useForm;
```
