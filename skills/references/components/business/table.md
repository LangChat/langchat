# Vben Vxe Table 表格

基于 [vxe-table](https://vxetable.cn/v4/#/grid/api?apiKey=grid) 和 `Vben Form` 做了二次封装，用于构建带搜索表单的列表页面。

## 基础用法

```vue
<script setup lang="ts">
import { useVbenVxeGrid } from '#/adapter/vxe-table';

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [
      { type: 'seq', width: 50 },
      { field: 'name', title: '名称' },
      { field: 'age', title: '年龄' },
    ],
    data: [
      { name: '张三', age: 18 },
      { name: '李四', age: 20 },
    ],
  },
});
</script>

<template>
  <Grid />
</template>
```

## 远程加载

```vue
<script setup lang="ts">
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getUserListApi } from '#/api';

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [
      { type: 'seq', width: 50 },
      { field: 'name', title: '名称' },
      { field: 'age', title: '年龄' },
    ],
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          const res = await getUserListApi({
            page: page.currentPage,
            pageSize: page.pageSize,
          });
          return {
            items: res.data.list,
            total: res.data.total,
          };
        },
      },
    },
  },
});
</script>
```

## 搜索表单

```vue
<script setup lang="ts">
import { useVbenVxeGrid } from '#/adapter/vxe-table';

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: [
      {
        component: 'Input',
        fieldName: 'name',
        label: '名称',
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
  },
  gridOptions: {
    toolbarConfig: {
      search: true, // 显示搜索面板开关按钮
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const res = await getUserListApi({
            ...formValues,
            page: page.currentPage,
            pageSize: page.pageSize,
          });
          return res;
        },
      },
    },
    columns: [...],
  },
});
</script>
```

## 树形表格

```ts
const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [...],
    treeConfig: {
      transform: true,
      parentField: 'parentId',
      rowField: 'id',
    },
  },
});
```

## 固定列

```ts
const columns = [
  { field: 'name', title: '名称', fixed: 'left', width: 100 },
  { field: 'age', title: '年龄' },
  { field: 'address', title: '地址' },
  { field: 'action', title: '操作', fixed: 'right', width: 100 },
];
```

## 单元格编辑

```ts
const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    editConfig: {
      mode: 'cell', // 或 'row'
      trigger: 'click',
    },
    columns: [
      {
        field: 'name',
        title: '名称',
        editRender: { name: 'input' },
      },
    ],
  },
});
```

## 自定义渲染器

```ts
// 适配器配置
import { h } from 'vue';
import { Image, Button } from 'ant-design-vue';

vxeUI.renderer.add('CellImage', {
  renderTableDefault(_renderOpts, params) {
    const { column, row } = params;
    return h(Image, { src: row[column.field] });
  },
});

vxeUI.renderer.add('CellLink', {
  renderTableDefault(renderOpts) {
    const { props } = renderOpts;
    return h(Button, { size: 'small', type: 'link' }, {
      default: () => props?.text,
    });
  },
});

// 使用
const columns = [
  {
    field: 'avatar',
    title: '头像',
    cellRender: { name: 'CellImage' },
  },
  {
    field: 'link',
    title: '链接',
    cellRender: { name: 'CellLink', props: { text: '查看' } },
  },
];
```

## GridApi 方法

| 方法名 | 描述 | 类型 |
|--------|------|------|
| setLoading | 设置loading状态 | `(loading: boolean) => void` |
| setGridOptions | 更新gridOptions | `(options) => void` |
| reload | 重新加载，重置分页 | `(params?) => void` |
| query | 重新查询，保留分页 | `(params?) => void` |
| grid | vxe-grid实例 | `VxeGridInstance` |
| formApi | 搜索表单API | `FormApi` |
| toggleSearchForm | 切换搜索表单状态 | `(show?: boolean) => boolean` |

## Props 属性

| 属性名 | 描述 | 类型 |
|--------|------|------|
| tableTitle | 表格标题 | `string` |
| tableTitleHelp | 表格标题帮助信息 | `string` |
| class | 外层容器的class | `string` |
| gridClass | vxe-grid的class | `string` |
| gridOptions | vxe-grid配置 | `VxeTableGridOptions` |
| gridEvents | vxe-grid事件 | `VxeGridListeners` |
| formOptions | 搜索表单配置 | `VbenFormProps` |
| showSearchForm | 是否显示搜索表单 | `boolean` |
| separator | 搜索表单与表格的分隔条 | `boolean \| SeparatorOptions` |

## 插槽

| 插槽名 | 描述 |
|--------|------|
| toolbar-actions | 工具栏左侧区域 |
| toolbar-tools | 工具栏右侧区域 |
| table-title | 自定义表格标题 |
| form-* | 搜索表单插槽转发 |

## 适配器配置

```ts
// src/adapter/vxe-table.ts
import { setupVbenVxeTable, useVbenVxeGrid } from '@vben/plugins/vxe-table';
import { useVbenForm } from './form';

setupVbenVxeTable({
  configVxeTable: (vxeUI) => {
    vxeUI.setConfig({
      grid: {
        align: 'center',
        border: false,
        columnConfig: {
          resizable: true,
        },
        minHeight: 180,
        proxyConfig: {
          autoLoad: true,
          response: {
            result: 'items',
            total: 'total',
            list: 'items',
          },
        },
        showOverflow: true,
        size: 'small',
      },
    });
  },
  useVbenForm,
});

export { useVbenVxeGrid };
```
