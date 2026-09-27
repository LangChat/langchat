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
      title: 'menu.agentBuilder',
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
      title: 'menu.docsList',
    },
  },
  {
    path: '/knowledge/docs/upload',
    name: 'KnowledgeUpload',
    component: () => import('#/views/docs/upload.vue'),
    meta: {
      activePath: '/knowledges',
      hideInMenu: true,
      title: 'menu.docsUpload',
    },
  },
  {
    path: '/knowledge/docs/:docsId/preview',
    name: 'KnowledgePreview',
    component: () => import('#/views/docs/preview.vue'),
    meta: {
      activePath: '/knowledges',
      hideInMenu: true,
      title: 'menu.docsPreview',
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
      title: 'menu.market',
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
      title: 'menu.marketChat',
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
      title: 'menu.overview',
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
      title: 'menu.about',
    },
  },
];

export default routes;
