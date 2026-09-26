# Page 页面组件

`Page` 是页面内容区最常用的顶层布局容器，内置了标题区、内容区和底部区三部分结构。

## 基础用法

```vue
<script setup lang="ts">
import { Page } from '@vben/common-ui';
</script>

<template>
  <Page title="页面标题" description="页面描述">
    <!-- 页面内容 -->
  </Page>
</template>
```

## 自动高度

```vue
<template>
  <!-- 开启自动高度计算，内容区会自动扣减头部和底部高度 -->
  <Page title="页面标题" auto-content-height>
    <div class="h-full overflow-auto">
      <!-- 内容 -->
    </div>
  </Page>
</template>
```

## 完整示例

```vue
<script setup lang="ts">
import { Page } from '@vben/common-ui';
</script>

<template>
  <Page
    title="用户管理"
    description="管理系统用户信息"
    auto-content-height
  >
    <template #extra>
      <Button type="primary">新增用户</Button>
    </template>

    <!-- 页面内容 -->
    <div class="p-4">
      <Table />
    </div>

    <template #footer>
      <Pagination />
    </template>
  </Page>
</template>
```

## Props 属性

| 属性名 | 描述 | 类型 | 默认值 |
|--------|------|------|--------|
| title | 页面标题 | `string` | - |
| description | 页面描述 | `string` | - |
| contentClass | 内容区域的class | `string` | - |
| headerClass | 头部区域的class | `string` | - |
| footerClass | 底部区域的class | `string` | - |
| autoContentHeight | 自动计算内容区高度 | `boolean` | `false` |
| heightOffset | 额外扣减的高度偏移量 | `number` | `0` |

## 插槽

| 插槽名 | 描述 |
|--------|------|
| default | 页面内容 |
| title | 页面标题 |
| description | 页面描述 |
| extra | 页面头部右侧内容 |
| footer | 页面底部内容 |

## 注意事项

- 如果 `title`、`description`、`extra` 三者都没有提供有效内容，头部区域不会渲染
- 开启 `autoContentHeight` 时，内容区需要设置 `overflow-auto` 来处理滚动
- 配合 Modal/Drawer 的 `appendToMain` 属性使用时，需要开启 `autoContentHeight`
