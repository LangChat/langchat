import { defineConfig } from '@vben/vite-config';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      server: {
        proxy: {
          '/api': {
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api/, ''),
            // 本地开发统一代理到 LangChat 后端服务
            target: 'http://localhost:8080/api',
            ws: true,
          },
        },
      },
    },
  };
});
