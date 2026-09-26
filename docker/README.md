# LangChat 一体化 Docker 部署

该目录用于构建和启动 LangChat 一体化环境：应用镜像内由 Nginx 托管前端、Supervisor 托管 Spring Boot 后端，同时编排 MySQL、PGVector 和 RustFS。

## 目录结构

```text
docker/
├── docker-compose.yml               # 应用与基础设施编排
├── .env.example                     # 端口及基础设施参数模板
├── application-docker.example.yml  # 后端配置模板
├── application-docker.yml          # 本地实际配置（Git 忽略）
├── entrypoint.sh                    # 镜像入口脚本
├── nginx/default.conf               # 前端与 API 反向代理
└── supervisor/langchat.conf         # Nginx 与后端进程托管

../Dockerfile                        # 一体化镜像构建文件
```

## 快速开始

```bash
cd docker
cp .env.example .env
cp application-docker.example.yml application-docker.yml
```

修改 `.env` 和 `application-docker.yml` 中的默认密码，然后启动：

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f langchat
```

也可以在项目根目录使用统一脚本：

```bash
bash build-docker.sh prepare
# 修改 docker/.env 和 docker/application-docker.yml
bash build-docker.sh deploy
```

`build-docker.sh` 同时兼容 `docker compose` 与 `docker-compose`，支持
`build`、`up`、`down`、`restart`、`logs`、`status` 和 `config` 等命令。
根目录的 `build-release.sh` 用于生成非 Docker 的 `release/` 交付目录。

默认访问地址：

- LangChat：`http://127.0.0.1`
- RustFS Console：`http://127.0.0.1:9001`

## 后端配置

数据库、初始管理员、Docling、技能工作区和 OSS 参数统一维护在 `application-docker.yml`。Compose 只保留 JVM、端口等运行参数，以及 MySQL、PGVector、RustFS 服务端自身所需参数。

`application-docker.yml` 可能包含明文密码，已加入 `.gitignore`。不要把本地实际配置提交到 Git。需要增加后端参数时，应优先写入该 YAML，而不是继续向 Compose 的 `langchat.environment` 增加变量。

## RustFS 对象存储

一体化编排使用 `rustfs/rustfs:1.0.0`，应用通过 S3 兼容协议访问 RustFS。首次连接时，OSS 模块会自动创建 `langchat` bucket。

以下凭据必须保持一致：

- `.env` 中的 `RUSTFS_ACCESS_KEY`、`RUSTFS_SECRET_KEY`：RustFS 服务端凭据。
- `application-docker.yml` 中 `langchat.storage.s3.access-key`、`secret-key`：LangChat 客户端凭据。

RustFS S3 API 和 Console 默认只绑定宿主机 `127.0.0.1`。生产环境不要直接暴露到公网，应使用强随机密钥，并通过 TLS 或受控反向代理提供访问。

> RustFS 的数据卷是对象存储内部格式，不能与原有本地 OSS 目录共享，也不能把其中的文件当普通目录直接读写。

## 数据持久化

| 卷 | 容器路径 | 内容 |
|---|---|---|
| `langchat-logs` | `/opt/langchat/logs` | 应用日志 |
| `langchat-workspace` | `/opt/langchat/workspace` | 技能解压工作区 |
| `langchat-mysql-data` | `/var/lib/mysql` | MySQL 数据 |
| `langchat-pgvector-data` | `/var/lib/postgresql/data` | PGVector 数据 |
| `langchat-rustfs-data` | `/data` | RustFS 对象数据 |

`docker compose down` 会保留数据卷；`docker compose down -v` 会删除全部数据，请谨慎执行。

## 单独构建镜像

```bash
docker build -f Dockerfile -t langchat:latest .
```

直接运行镜像时必须挂载外部配置文件：

```bash
docker run --rm -p 80:80 \
  -v "$PWD/docker/application-docker.yml:/opt/langchat/config/application-docker.yml:ro" \
  langchat:latest
```

外部 MySQL 或 S3 地址需要在该 YAML 中改成容器可访问的地址。

## 常见问题

**后端启动后立即退出**

先检查 `application-docker.yml` 是否存在，再查看 `docker compose logs langchat`。Compose 网络内的主机名应使用 `mysql` 和 `rustfs`，不要使用 `127.0.0.1`。

**RustFS 初始化失败或无权限**

编排中的 `rustfs-volume-init` 会把命名卷所有者设置为 RustFS 默认用户 `10001:10001`。若改为宿主机目录挂载，需要自行确保目录对该 UID/GID 可写。

**页面可打开但 API 返回 502**

通常是后端仍在启动或配置错误。等待健康检查完成，并查看 `docker compose logs -f langchat`。
