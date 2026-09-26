# Vben EllipsisText 省略文本

用于展示超长文本，支持省略、Tooltip 提示以及点击展开收起。

## 基础用法

```vue
<script setup lang="ts">
import { EllipsisText } from '@vben/common-ui';
</script>

<template>
  <EllipsisText :max-width="200">
    这是一段很长的文本内容，超出部分会被省略显示...
  </EllipsisText>
</template>
```

## 可折叠文本

```vue
<template>
  <EllipsisText :line="2" expand>
    这是一段很长的文本内容，默认显示2行，点击可以展开查看全部内容。
    展开后再次点击可以收起。
  </EllipsisText>
</template>
```

## 自定义 Tooltip

```vue
<template>
  <EllipsisText>
    这是一段文本
    <template #tooltip>
      <div>自定义提示内容</div>
    </template>
  </EllipsisText>
</template>
```

## 仅省略时显示 Tooltip

```vue
<template>
  <!-- 只有文本被截断时才显示 Tooltip -->
  <EllipsisText tooltip-when-ellipsis>
    这是一段文本
  </EllipsisText>
</template>
```

## Props 属性

| 属性名 | 描述 | 类型 | 默认值 |
|--------|------|------|--------|
| expand | 是否支持点击展开/收起 | `boolean` | `false` |
| line | 文本最大显示行数 | `number` | `1` |
| maxWidth | 文本区域最大宽度 | `number \| string` | `'100%'` |
| placement | 提示浮层位置 | `'top' \| 'bottom' \| 'left' \| 'right'` | `'top'` |
| tooltip | 是否启用文本提示 | `boolean` | `true` |
| tooltipWhenEllipsis | 是否仅在文本被截断时显示提示 | `boolean` | `false` |
| ellipsisThreshold | 文本截断检测阈值 | `number` | `3` |
| tooltipBackgroundColor | 提示背景色 | `string` | `''` |
| tooltipColor | 提示文字颜色 | `string` | `''` |
| tooltipFontSize | 提示文字大小（px） | `number` | `14` |
| tooltipMaxWidth | 提示内容最大宽度（px） | `number` | - |
| tooltipOverlayStyle | 提示内容区域样式 | `CSSProperties` | `{ textAlign: 'justify' }` |

## Events 事件

| 事件名 | 描述 | 类型 |
|--------|------|------|
| expandChange | 展开状态变化时触发 | `(isExpand: boolean) => void` |

## 插槽

| 插槽名 | 描述 |
|--------|------|
| default | 文本内容 |
| tooltip | 自定义提示内容 |

## 表格中使用

```vue
<script setup lang="ts">
import { EllipsisText } from '@vben/common-ui';

const columns = [
  {
    field: 'description',
    title: '描述',
    slots: {
      default: ({ row }) => {
        return h(EllipsisText, {
          maxWidth: 200,
          line: 2,
          expand: true,
        }, () => row.description);
      },
    },
  },
];
</script>
```

## 使用场景

- 表格长文本列
- 列表项描述
- 评论内容展示
- 日志信息展示
