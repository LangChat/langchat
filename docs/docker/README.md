# 中间件编排脚本

LangChat 依赖的中间件独立编排，按需选择启动。每个中间件一个目录、一份 compose，
可单独启停，互不干扰。

> 若只想一键拉起完整环境（应用 + MySQL + PGVector），请使用项目根目录的
> [`docker/docker-compose.yml`](../../docker/docker-compose.yml)，无需本目录。

## 中间件清单

| 中间件 | 目录 | 端口 | 是否必需 | 用途 |
|--------|------|------|---------|------|
| MySQL | [mysql/](./mysql/) | 3306 | 必需 | 业务数据库（用户、知识库、会话、模型配置等） |
| PGVector | [pgvector/](./pgvector/) | 5432 | 可选 | 知识库向量检索存储（使用 Milvus 时可不启） |
| Redis | [redis/](./redis/) | 6379 | 可选 | 缓存 / 会话共享（当前版本后端未直接依赖） |
| Nginx | [nginx/](./nginx/) | 8081 / 8443 | 可选 | 独立入口网关（域名、HTTPS、多应用共用） |

## 启动方式

在**项目根目录**执行（路径以 root 为基准，避免相对路径歧义）：

```bash
# MySQL
docker compose -f docs/docker/mysql/docker-compose.yml up -d

# PGVector
docker compose -f docs/docker/pgvector/docker-compose.yml up -d

# Redis
docker compose -f docs/docker/redis/docker-compose.yml up -d

# Nginx（需先准备配置，见下文）
docker compose -f docs/docker/nginx/docker-compose.yml up -d
```

停止与清理：

```bash
docker compose -f docs/docker/mysql/docker-compose.yml down      # 保留数据
docker compose -f docs/docker/mysql/docker-compose.yml down -v   # 删除数据卷
```

## 各中间件说明

### MySQL（必需）

首次启动自动导入 `docs/db/langchat.sql`（建表 + 菜单 + 初始管理员）。
导入仅在数据卷为空时执行，重置需 `down -v` 后重新 `up`。

```bash
# 进入容器执行 SQL
docker exec -it langchat-mysql mysql -uroot -p123456 langchat
```

连接串模板：

```
jdbc:mysql://<host>:3306/langchat?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
```

### PGVector（可选）

镜像内置向量扩展，`initdb/01-enable-vector.sql` 会在首次启动时自动执行
`CREATE EXTENSION vector`，无需手工操作。

应用侧连接信息在 **模型与能力 → 向量存储** 中配置：

| 字段 | 值 |
|------|-----|
| 类型 | PgVector |
| host | 宿主机 IP（容器间通信填 `pgvector`） |
| port | 5432 |
| database | langchat |
| username | postgres |
| password | postgres |
| 表名 | 自定义（如 `langchat_embedding`） |
| 维度 | 需与向量模型输出维度一致（如 `text-embedding-v3` 为 1024） |

> 维度必须与所选向量模型匹配，否则写入会报维度不匹配；建表由应用在首次写入时创建。

### Redis（可选）

当前后端未直接依赖 Redis，此编排供后续扩展或与其他服务共用。
启用后通过标准 Spring 配置接入：

```
SPRING_DATA_REDIS_HOST=<host>
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=123456
```

### Nginx（可选）

作为独立入口网关，前置应用容器。使用步骤：

```bash
cd docs/docker/nginx
cp nginx.conf.example nginx.conf    # 按需修改域名与上游
mkdir -p certs                       # 放置 fullchain.pem / privkey.pem
cd - && docker compose -f docs/docker/nginx/docker-compose.yml up -d
```

默认上游是 `host.docker.internal:80`（指向宿主机上运行的应用，Docker Desktop 可直接用；
Linux 需替换为宿主机 IP 或改为容器网络）。

> 注意：一体化应用镜像内部已自带 Nginx。若同时启用本网关，请把应用的对外端口
> 改为仅本机映射（如 `127.0.0.1:8080:80`），由本网关统一对外，避免重复代理。

## 常见组合

**本地快速体验** —— 使用根目录 `docker/docker-compose.yml`，一键拉起全部。

**应用容器化 + 中间件独立** —— 本目录启动 MySQL 与 PGVector，应用镜像单独运行，
通过宿主机 IP 或自定义网络互通。

**对接已有数据库** —— 无需启动中间件，直接在应用侧配置现有 MySQL / PGVector 连接信息。

## 数据卷

各中间件数据存于具名卷，删除容器不会丢数据：

```bash
docker volume ls | grep langchat
```
