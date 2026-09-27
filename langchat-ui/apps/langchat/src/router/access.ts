import type {
  ComponentRecordType,
  GenerateMenuAndRoutesOptions,
} from '@vben/types';
import type { RouteRecordStringComponent } from '@vben/types';

import { generateAccessible } from '@vben/access';
import { preferences } from '@vben/preferences';

import { message } from '#/adapter/naive';
import { getAllMenusApi } from '#/api';
import { BasicLayout, IFrameView } from '#/layouts';
import { $t, $te } from '#/locales';
import { normalizeMenuIcons } from '#/router/menu-icon-resolver';

const forbiddenComponent = () => import('#/views/_core/fallback/forbidden.vue');

/**
 * 后端菜单的 meta.title 为中文文案，这里按路由 name 把它替换为本地化键，
 * 让侧边栏、面包屑与页面标题随语言切换。
 */
function localizeMenuTitles(
  routes: RouteRecordStringComponent[],
): RouteRecordStringComponent[] {
  return routes.map((route) => {
    const titleKey = route.name
      ? `menu.dynamic.${String(route.name).replace(/^menu_/, '')}`
      : undefined;
    const name = typeof route.name === 'string' ? route.name : undefined;
    const title =
      titleKey && $te(titleKey)
        ? titleKey
        : (route.meta?.title ?? name ?? '');
    return {
      ...route,
      meta: { ...route.meta, title },
      children: route.children?.length
        ? localizeMenuTitles(route.children)
        : route.children,
    };
  });
}

async function generateAccess(options: GenerateMenuAndRoutesOptions) {
  const pageMap: ComponentRecordType = import.meta.glob('../views/**/*.vue');

  const layoutMap: ComponentRecordType = {
    BasicLayout,
    IFrameView,
  };

  return await generateAccessible(preferences.app.accessMode, {
    ...options,
    fetchMenuListAsync: async () => {
      message.loading(`${$t('common.loadingMenu')}...`, {
        duration: 1.5,
      });
      const routes = await getAllMenusApi();
      return normalizeMenuIcons(localizeMenuTitles(routes) as never);
    },
    // 可以指定没有权限跳转403页面
    forbiddenComponent,
    // 如果 route.meta.menuVisibleWithForbidden = true
    layoutMap,
    pageMap,
  });
}

export { generateAccess };
