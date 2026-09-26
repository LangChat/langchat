import type { RouteRecordRaw } from 'vue-router';

import { Info, LayoutDashboard, Store } from '@vben/icons';

const routes: RouteRecordRaw[] = [
  // ========================
  // Agent 构建
  // ========================
  {
    path: '/agents/:id/builder',
    name: 'AgentBuilder',
    component: () => import('#/views/agents/builder.vue'),
    meta: {
      activePath: '/agents',
      hideInBreadcrumb: false,
      hideInMenu: true,
      hideInTab: false,
      keepAlive: false,
      title: 'page.menu.agentBuilder',
    },
  },

  // ========================
  // 知识库文档 (扁平化路由, 知识库 ID 通过 query 传递)
  // ========================
  {
    path: '/knowledge/docs',
    name: 'KnowledgeDocs',
    component: () => import('#/views/docs/index.vue'),
    meta: {
      activePath: '/knowledges',
      hideInMenu: true,
      title: 'page.menu.docsList',
    },
  },
  {
    path: '/knowledge/docs/upload',
    name: 'KnowledgeUpload',
    component: () => import('#/views/docs/upload.vue'),
    meta: {
      activePath: '/knowledges',
      hideInMenu: true,
      title: 'page.menu.docsUpload',
    },
  },
  {
    path: '/knowledge/docs/:docsId/preview',
    name: 'KnowledgePreview',
    component: () => import('#/views/docs/preview.vue'),
    meta: {
      activePath: '/knowledges',
      hideInMenu: true,
      title: 'page.menu.docsPreview',
    },
  },

  // ========================
  // 应用市场
  // ========================
  {
    path: '/market',
    name: 'Market',
    component: () => import('#/views/market/index.vue'),
    meta: {
      icon: Store,
      keepAlive: true,
      order: 2,
      title: 'page.menu.market',
    },
  },
  {
    path: '/market/:agentId/chat',
    name: 'MarketChat',
    component: () => import('#/views/market/chat.vue'),
    meta: {
      activePath: '/market',
      hideInMenu: true,
      hideInTab: true,
      keepAlive: false,
      title: 'page.menu.marketChat',
    },
  },

  // ========================
  // 概览
  // ========================
  {
    path: '/explore',
    name: 'Explore',
    component: () => import('#/views/explore/index.vue'),
    meta: {
      icon: LayoutDashboard,
      keepAlive: true,
      order: 1,
      title: 'page.menu.overview',
    },
  },

  // ========================
  // 关于
  // ========================
  {
    path: '/about',
    name: 'About',
    component: () => import('#/views/about/index.vue'),
    meta: {
      hideInMenu: true,
      icon: Info,
      keepAlive: true,
      order: 999,
      title: 'page.menu.about',
    },
  },
];

export default routes;
