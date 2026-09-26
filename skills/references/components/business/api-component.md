# Vben ApiComponent API组件包装器

用于包装其它组件，为目标组件提供自动获取远程数据的能力。

## 基础用法

包装 Select 组件，自动获取远程选项：

```vue
<script setup lang="ts">
import { ApiComponent } from '@vben/common-ui';
import { Select } from 'ant-design-vue';

async function fetchOptions() {
  const res = await getUserListApi();
  return res.data;
}
</script>

<template>
  <ApiComponent
    v-model="selectedValue"
    :api="fetchOptions"
    :component="Select"
    label-field="name"
    value-field="id"
  />
</template>
```

## 包装级联选择器

```vue
<script setup lang="ts">
import { ApiComponent } from '@vben/common-ui';
import { Cascader } from 'ant-design-vue';

async function fetchTreeData() {
  const res = await getRegionTreeApi();
  return res.data;
}
</script>

<template>
  <ApiComponent
    v-model="selectedRegion"
    :api="fetchTreeData"
    :component="Cascader"
    :immediate="false"
    children-field="children"
    loading-slot="suffixIcon"
    visible-event="onDropdownVisibleChange"
  />
</template>
```

## 请求参数

```vue
<script setup lang="ts">
const params = ref({ type: 'user' });

async function fetchOptions(params) {
  const res = await getOptionsApi(params);
  return res.data;
}
</script>

<template>
  <ApiComponent
    v-model="value"
    :api="fetchOptions"
    :params="params"
    :component="Select"
  />
</template>
```

## 请求前后处理

```vue
<script setup lang="ts">
async function beforeFetch(params) {
  // 请求前处理参数
  return { ...params, status: 1 };
}

async function afterFetch(data) {
  // 请求后处理数据
  return data.map(item => ({
    ...item,
    label: `${item.name} (${item.code})`,
  }));
}
</script>

<template>
  <ApiComponent
    v-model="value"
    :api="fetchOptions"
    :component="Select"
    :before-fetch="beforeFetch"
    :after-fetch="afterFetch"
  />
</template>
```

## 自动选择选项

```vue
<template>
  <!-- 自动选择第一个选项 -->
  <ApiComponent
    v-model="value"
    :api="fetchOptions"
    :component="Select"
    auto-select="first"
  />

  <!-- 有且仅有一个选项时自动选择 -->
  <ApiComponent
    v-model="value"
    :api="fetchOptions"
    :component="Select"
    auto-select="one"
  />
</template>
```

## Props 属性

| 属性名 | 描述 | 类型 | 默认值 |
|--------|------|------|--------|
| modelValue | 当前值 | `any` | - |
| component | 目标组件 | `Component` | - |
| api | 获取数据的函数 | `(arg?) => Promise<any>` | - |
| params | 传递给api的参数 | `object` | - |
| resultField | 从结果中提取数组的字段名 | `string` | - |
| labelField | label字段名 | `string` | `label` |
| valueField | value字段名 | `string` | `value` |
| childrenField | 子级数据字段名 | `string` | - |
| optionsPropName | 目标组件接收options的属性名 | `string` | `options` |
| modelPropName | 目标组件的双向绑定属性名 | `string` | `modelValue` |
| immediate | 是否立即调用api | `boolean` | `true` |
| alwaysLoad | 每次显示时重新请求 | `boolean` | `false` |
| beforeFetch | 请求前的回调 | `(params) => any` | - |
| afterFetch | 请求后的回调 | `(data) => any` | - |
| options | 直接传入选项数据 | `OptionsItem[]` | - |
| visibleEvent | 触发请求的事件名 | `string` | - |
| loadingSlot | 显示loading的插槽名 | `string` | - |
| numberToString | 将value从数字转为string | `boolean` | `false` |
| autoSelect | 自动设置选项 | `'first' \| 'last' \| 'one'` | `false` |

## Methods 方法

| 方法 | 描述 | 类型 |
|------|------|------|
| getComponentRef | 获取被包装组件的实例 | `() => T` |
| updateParam | 设置接口请求参数 | `(params) => void` |
| getOptions | 获取已加载的选项数据 | `() => OptionsItem[]` |
| getValue | 获取当前值 | `() => any` |

## 并发和缓存

使用 Tanstack Query 包装接口请求，实现并发控制和缓存：

```ts
import { useQuery } from '@tanstack/vue-query';

function useUserOptions() {
  return useQuery({
    queryKey: ['user-options'],
    queryFn: () => getUserListApi(),
    staleTime: 5 * 60 * 1000, // 5分钟缓存
  });
}
```

## 适配器配置

在应用适配器中预包装组件：

```ts
// src/adapter/component.ts
import { ApiComponent } from '@vben/common-ui';
import { Select, TreeSelect } from 'ant-design-vue';

const components = {
  ApiSelect: (props, { attrs, slots }) => {
    return h(ApiComponent, {
      ...props,
      ...attrs,
      component: Select,
    }, slots);
  },
  ApiTreeSelect: (props, { attrs, slots }) => {
    return h(ApiComponent, {
      ...props,
      ...attrs,
      component: TreeSelect,
      childrenField: 'children',
    }, slots);
  },
};
```
