# Vben CountToAnimator 数字动画

用于展示数字滚动动画效果。

## 基础用法

```vue
<script setup lang="ts">
import { CountToAnimator } from '@vben/common-ui';
</script>

<template>
  <CountToAnimator :start-val="0" :end-val="2024" :duration="1500" />
</template>
```

## 自定义格式

```vue
<template>
  <!-- 带前缀和后缀 -->
  <CountToAnimator
    :start-val="0"
    :end-val="9999"
    prefix="¥"
    suffix="元"
  />

  <!-- 带小数位 -->
  <CountToAnimator
    :start-val="0"
    :end-val="99.99"
    :decimals="2"
  />

  <!-- 自定义分隔符 -->
  <CountToAnimator
    :start-val="0"
    :end-val="1000000"
    separator=","
  />
</template>
```

## 手动控制

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { CountToAnimator } from '@vben/common-ui';

const countRef = ref();

function handleStart() {
  countRef.value?.reset();
}
</script>

<template>
  <CountToAnimator
    ref="countRef"
    :start-val="0"
    :end-val="9999"
    :autoplay="false"
  />
  <Button @click="handleStart">开始动画</Button>
</template>
```

## Props 属性

| 属性名 | 描述 | 类型 | 默认值 |
|--------|------|------|--------|
| startVal | 起始值 | `number` | `0` |
| endVal | 结束值 | `number` | `2021` |
| duration | 动画持续时间（ms） | `number` | `1500` |
| autoplay | 是否自动播放 | `boolean` | `true` |
| prefix | 前缀 | `string` | `''` |
| suffix | 后缀 | `string` | `''` |
| separator | 千分位分隔符 | `string` | `','` |
| decimal | 小数点分隔符 | `string` | `'.'` |
| decimals | 保留小数位数 | `number` | `0` |
| color | 文本颜色 | `string` | `''` |
| useEasing | 是否启用过渡预设 | `boolean` | `true` |
| transition | 过渡预设名称 | `string` | `'linear'` |

## Events 事件

| 事件名 | 描述 | 类型 |
|--------|------|------|
| started | 动画开始时触发 | `() => void` |
| finished | 动画结束时触发 | `() => void` |

## Methods 方法

| 方法名 | 描述 | 类型 |
|--------|------|------|
| reset | 重置并重新执行动画 | `() => void` |

## 过渡预设

```vue
<template>
  <CountToAnimator
    :end-val="1000"
    transition="easeOutQuart"
  />
</template>
```

可用的过渡预设：
- `linear`
- `easeInQuad`
- `easeOutQuad`
- `easeInOutQuad`
- `easeInCubic`
- `easeOutCubic`
- `easeInOutCubic`
- `easeOutQuart`
- `easeOutExpo`
- 等等...

## 使用场景

- 统计数据展示
- 仪表盘数字
- 倒计时效果
- 金融数字展示
