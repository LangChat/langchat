# ==============================================================================
# LangChat 一体化镜像
#
# 将前端静态资源与后端服务打包进同一个镜像，由 Supervisor 统一托管：
#   - Nginx  : 对外暴露 80 端口，托管前端静态资源并反向代理 /api 到后端
#   - 后端   : Spring Boot（内嵌 Tomcat，仅监听 127.0.0.1:8080，不对外暴露）
#
# 同时内置运维排查所需工具（curl / vim / netcat / procps / mysql-client /
# postgresql-client / redis-tools），便于在容器内直接连库排查问题。
#
# 构建（在项目根目录执行）：
#   docker build -f Dockerfile -t langchat:latest .
#
# 运行：
#   docker run -d --name langchat -p 80:80 \
#     -v "$PWD/docker/application-docker.yml:/opt/langchat/config/application-docker.yml:ro" \
#     langchat:latest
#
# 更推荐使用 docker 目录下的 compose 编排（会拉起 MySQL / PGVector / RustFS）：
#   cd docker && docker compose up -d
# ==============================================================================

# ------------------------------------------------------------------------------
# 阶段 1：构建前端静态资源
# ------------------------------------------------------------------------------
FROM node:22-alpine AS frontend-builder

WORKDIR /build/langchat-ui

# 启用与项目 packageManager 字段一致的 pnpm 版本
RUN corepack enable && corepack prepare pnpm@10.32.1 --activate

# 整体拷贝（node_modules / dist 等已由 .dockerignore 排除）。
# workspace 声明了 packages/@core/* 等多层路径，逐层 COPY 易漏且难维护，
# 这里直接拷全量，依赖安装层仍可被 Docker 缓存复用。
COPY langchat-ui/ ./

# --ignore-scripts：跳过根 package.json 的 prepare（pnpm exec lefthook install）。
# lefthook 是本地开发用的 git hook 工具，镜像构建中既无 .git 也不需要它。
# 依赖自身的构建脚本不受影响——pnpm 10 默认本就不执行依赖的安装脚本
# （项目未配置 onlyBuiltDependencies），平台二进制由 optionalDependencies 提供。
ENV CI=true
RUN pnpm install --frozen-lockfile --ignore-scripts \
    || pnpm install --ignore-scripts
RUN pnpm --filter @vben/web-naive build

# ------------------------------------------------------------------------------
# 阶段 2：构建后端可执行 jar
# ------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /build

# 整体拷贝后端模块（target 等已由 .dockerignore 排除）。
# 依赖下载与编译在 Maven 本地仓库卷 / 分层缓存下仍可复用。
COPY pom.xml ./
COPY langchat-common/ langchat-common/
COPY langchat-auth/ langchat-auth/
COPY langchat-monitor/ langchat-monitor/
COPY langchat-core/ langchat-core/
COPY langchat-aigc/ langchat-aigc/
COPY langchat-datasource/ langchat-datasource/
COPY langchat-server/ langchat-server/

RUN mvn -B -DskipTests clean package

# ------------------------------------------------------------------------------
# 阶段 3：运行镜像（前后端合一）
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy

ENV TZ=Asia/Shanghai \
    DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    JAVA_OPTS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai" \
    LANGCHAT_HOME=/opt/langchat

# Nginx + Supervisor + 常用排查工具（含 MySQL / PostgreSQL / Redis 客户端）
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        nginx \
        supervisor \
        curl \
        wget \
        vim \
        less \
        netcat-openbsd \
        procps \
        tzdata \
        ca-certificates \
        default-mysql-client \
        postgresql-client \
        redis-tools \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /var/log/supervisor /var/log/nginx /run/nginx "$LANGCHAT_HOME" \
    && rm -f /etc/nginx/sites-enabled/default

WORKDIR $LANGCHAT_HOME

# 前端静态资源
COPY --from=frontend-builder /build/langchat-ui/apps/langchat/dist/ /usr/share/nginx/html/
# 后端可执行 jar
COPY --from=backend-builder /build/langchat-server/target/langchat-server.jar $LANGCHAT_HOME/app.jar
# Nginx / Supervisor 配置与入口脚本
COPY docker/nginx/default.conf /etc/nginx/conf.d/langchat.conf
COPY docker/supervisor/langchat.conf /etc/supervisor/conf.d/langchat.conf
COPY docker/entrypoint.sh /usr/local/bin/langchat-entrypoint
RUN chmod +x /usr/local/bin/langchat-entrypoint

# 后端工作目录与持久化目录：
#   logs      —— logback 的 log.path 为相对路径 logs/<appName>，随工作目录落在此处
#   langchat-workspace —— 技能解压工作区（langchat.skill.workspace-dir 指向该目录）
# 后端工作目录固定为 $LANGCHAT_HOME，确保上述相对路径稳定
RUN mkdir -p "$LANGCHAT_HOME/logs" "$LANGCHAT_HOME/config" "$LANGCHAT_HOME/langchat-workspace"

VOLUME ["$LANGCHAT_HOME/logs", "$LANGCHAT_HOME/langchat-workspace"]

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://127.0.0.1/healthz || exit 1

ENTRYPOINT ["/usr/local/bin/langchat-entrypoint"]
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/langchat.conf"]
