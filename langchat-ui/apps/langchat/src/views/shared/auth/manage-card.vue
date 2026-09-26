<!--
  - © 2024-present LangChat团队. 版权所有.
  -->
<script lang="ts" setup>
/**
 * 系统管理通用页面骨架：整页一张带边框的大卡片。
 * 顶部为自定义搜索区（无内置展开/折叠按钮），其余空间为表格区域。
 */
</script>

<template>
  <div
    class="flex h-full min-h-0 flex-col overflow-hidden rounded-lg border border-border bg-card"
  >
    <div
      v-if="$slots.search"
      class="flex flex-wrap items-center gap-x-5 gap-y-3 border-b border-border px-4 py-4"
    >
      <slot name="search"></slot>
    </div>
    <slot></slot>
  </div>
</template>
