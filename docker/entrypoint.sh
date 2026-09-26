#!/usr/bin/env bash
# ==============================================================================
# LangChat 一体化镜像入口脚本
#
# 职责：
#   1. 校验外置 Spring YAML 配置文件
#   2. 按需渲染 Nginx 的后端上游地址（默认同容器 127.0.0.1:8080）
#   3. 确保日志与工作区目录存在且可写
#   4. 交出控制权给 Supervisor
# ==============================================================================
set -euo pipefail

LANGCHAT_HOME="${LANGCHAT_HOME:-/opt/langchat}"
NGINX_CONF="${NGINX_CONF:-/etc/nginx/conf.d/langchat.conf}"
BACKEND_HOST="${LANGCHAT_BACKEND_HOST:-127.0.0.1}"
BACKEND_PORT="${LANGCHAT_BACKEND_PORT:-8080}"
CONFIG_FILE="${LANGCHAT_CONFIG_FILE:-$LANGCHAT_HOME/config/application-docker.yml}"

log()  { printf '[langchat-entrypoint] %s\n' "$*"; }
warn() { printf '[langchat-entrypoint] WARN: %s\n' "$*" >&2; }

# ------------------------------------------------------------------------------
# 1. 外置应用配置校验
# ------------------------------------------------------------------------------
if [ ! -f "$CONFIG_FILE" ]; then
    warn "未找到后端配置文件：$CONFIG_FILE"
    warn "Compose 部署请先复制 application-docker.example.yml 为 application-docker.yml。"
    exit 1
fi
log "加载后端配置：$CONFIG_FILE"

# ------------------------------------------------------------------------------
# 2. 按需改写 Nginx 上游（后端与其他服务分容器部署时使用）
# ------------------------------------------------------------------------------
if [ "$BACKEND_HOST" != "127.0.0.1" ] || [ "$BACKEND_PORT" != "8080" ]; then
    if [ -f "$NGINX_CONF" ]; then
        log "改写 Nginx 后端上游为 ${BACKEND_HOST}:${BACKEND_PORT}"
        sed -i "s#server 127\.0\.0\.1:8080;#server ${BACKEND_HOST}:${BACKEND_PORT};#" "$NGINX_CONF"
    fi
fi

# ------------------------------------------------------------------------------
# 3. 目录准备（挂载卷覆盖时可能出现权限/缺失问题）
# ------------------------------------------------------------------------------
mkdir -p \
    "$LANGCHAT_HOME/logs" \
    "$LANGCHAT_HOME/workspace/skills" \
    /var/log/nginx \
    /var/log/supervisor

# ------------------------------------------------------------------------------
# 4. 启动 Supervisor（接管 Nginx 与后端两个进程）
# ------------------------------------------------------------------------------
log "启动 LangChat（Nginx 80 + 后端 ${BACKEND_PORT}）"
exec "$@"
