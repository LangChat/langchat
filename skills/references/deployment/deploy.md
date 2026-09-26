# 构建与部署

## 构建

### 构建命令

```bash
# 构建所有应用
pnpm build

# 构建指定应用
pnpm build:antd
pnpm build:ele
pnpm build:naive
```

### 环境变量

```bash
# .env.production
VITE_APP_TITLE=Vben Admin
VITE_APP_NAMESPACE=vben-web-antd
VITE_BASE=/
VITE_GLOB_API_URL=https://api.example.com
VITE_COMPRESS=gzip           # 压缩方式: none, brotli, gzip
VITE_PWA=false               # PWA支持
VITE_ROUTER_HISTORY=hash     # 路由模式: hash, history
VITE_INJECT_APP_LOADING=true # 注入全局loading
VITE_ARCHIVER=true           # 生成dist.zip
```

## 预览

```bash
# 预览构建结果
pnpm preview
```

## 压缩

```bash
# gzip压缩
VITE_COMPRESS=gzip

# brotli压缩
VITE_COMPRESS=brotli

# 不压缩
VITE_COMPRESS=none
```

## 分析构建

```bash
# 分析构建产物
pnpm analyze
```

## 部署

### Nginx 配置

```nginx
server {
    listen 80;
    server_name example.com;
    root /usr/share/nginx/html;
    index index.html;

    # 开启gzip压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    gzip_min_length 1024;

    # hash路由配置
    location / {
        try_files $uri $uri/ /index.html;
    }

    # history路由配置
    # location / {
    #     try_files $uri $uri/ /index.html;
    # }

    # API代理
    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

### Docker 部署

```dockerfile
# Dockerfile
FROM node:20-alpine as builder
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN npm install -g pnpm && pnpm install --frozen-lockfile
COPY . .
RUN pnpm build:antd

FROM nginx:alpine
COPY --from=builder /app/apps/web-antd/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

```yaml
# docker-compose.yml
version: '3'
services:
  web:
    build: .
    ports:
      - "80:80"
    depends_on:
      - backend
  backend:
    image: your-backend-image
    ports:
      - "8080:8080"
```

## 动态配置

打包后可通过修改 `_app.config.js` 动态修改配置：

```js
// dist/_app.config.js
window._VBEN_ADMIN_PRO_APP_CONF_ = {
  VITE_GLOB_API_URL: 'https://api.example.com',
};
```

## PWA 支持

```bash
# 开启PWA
VITE_PWA=true
```

## 路由模式

```bash
# hash模式（默认）
VITE_ROUTER_HISTORY=hash

# history模式（需要服务器配置）
VITE_ROUTER_HISTORY=history
```

## CDN 部署

```bash
# 设置公共资源路径
VITE_BASE=https://cdn.example.com/
```

## 常见问题

### 1. 构建内存溢出

```bash
# 增加Node内存
NODE_OPTIONS=--max_old_space_size=4096 pnpm build
```

### 2. 静态资源404

- 检查 `VITE_BASE` 配置是否正确
- 确保Nginx配置了正确的root路径

### 3. 跨域问题

- 开发环境：配置vite proxy
- 生产环境：配置Nginx代理或后端开启CORS
