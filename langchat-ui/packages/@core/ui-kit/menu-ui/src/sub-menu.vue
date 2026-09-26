<script setup lang="ts">
import type { MenuRecordRaw } from '@vben-core/typings';

import { computed } from 'vue';

import { MenuBadge, MenuItem, SubMenu as SubMenuComp } from './components';
import { useMenuContext } from './hooks';
import SubMenu from './sub-menu.vue';

interface Props {
  /**
   * 菜单项
   */
  menu: MenuRecordRaw;
}

defineOptions({
  name: 'SubMenuUi',
});

const props = withDefaults(defineProps<Props>(), {});

const rootMenu = useMenuContext();
const flatGroupMode = computed(() => rootMenu?.props.flatGroupMode ?? false);

/**
 * 判断是否有子节点，动态渲染 menu-item/sub-menu-item
 */
const hasChildren = computed(() => {
  const { menu } = props;
  return (
    Reflect.has(menu, 'children') && !!menu.children && menu.children.length > 0
  );
});
</script>

<template>
  <!-- Leaf node: no children -->
  <MenuItem
    v-if="!hasChildren"
    :key="menu.path"
    :active-icon="menu.activeIcon"
    :badge="menu.badge"
    :badge-type="menu.badgeType"
    :badge-variants="menu.badgeVariants"
    :icon="menu.icon"
    :path="menu.path"
    :query="menu.query"
  >
    <template #title>
      <span>{{ menu.name }}</span>
    </template>
  </MenuItem>

  <!-- Flat group mode: label + children always visible -->
  <template v-else-if="flatGroupMode">
    <li
      :key="`${menu.path}_group_label`"
      class="menu-group-label"
    >
      <span class="menu-group-label-text">{{ menu.name }}</span>
    </li>
    <template v-for="childItem in menu.children || []" :key="childItem.path">
      <SubMenu :menu="childItem" />
    </template>
  </template>

  <!-- Default: collapsible accordion group -->
  <SubMenuComp
    v-else
    :key="`${menu.path}_sub`"
    :active-icon="menu.activeIcon"
    :icon="menu.icon"
    :path="menu.path"
  >
    <template #content>
      <MenuBadge
        :badge="menu.badge"
        :badge-type="menu.badgeType"
        :badge-variants="menu.badgeVariants"
        class="right-6"
      />
    </template>
    <template #title>
      <span>{{ menu.name }}</span>
    </template>
    <template v-for="childItem in menu.children || []" :key="childItem.path">
      <SubMenu :menu="childItem" />
    </template>
  </SubMenuComp>
</template>

<style scoped>
.menu-group-label {
  pointer-events: none;
  padding: 10px 16px 4px 16px;
}

.menu-group-label-text {
  font-size: 11px;
  font-weight: 500;
  line-height: 1.2;
  color: hsl(var(--muted-foreground));
  opacity: 0.6;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}
</style>
