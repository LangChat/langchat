#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_DIR="${ROOT_DIR}/docker"
COMPOSE_FILE="${DOCKER_DIR}/docker-compose.yml"
DEFAULT_ENV_FILE="${DOCKER_DIR}/.env"
ENV_FILE="${LANGCHAT_DOCKER_ENV_FILE:-${DEFAULT_ENV_FILE}}"
DEFAULT_CONFIG_FILE="${DOCKER_DIR}/application-docker.yml"

usage() {
  cat <<'EOF'
用法：bash build-docker.sh <命令> [参数]

命令：
  prepare          创建本地 .env 和后端 YAML（已存在时不覆盖）
  build            构建 LangChat 应用镜像（默认）
  deploy           构建镜像并后台启动全部服务
  up               使用现有镜像后台启动全部服务
  down             停止并删除服务容器（保留数据卷）
  restart          重启 LangChat 应用容器
  logs [服务名]    查看日志，默认 langchat
  status           查看 Compose 服务状态
  config           展开并校验 Compose 配置
  release          构建非 Docker 交付目录

环境变量：
  LANGCHAT_DOCKER_ENV_FILE  指定 Compose 环境变量文件
  LANGCHAT_RELEASE_DIR      指定 build-release.sh 的输出目录

详细说明见 docker/README.md。
EOF
}

require_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "缺少 Docker，请先安装 Docker Engine。" >&2
    exit 1
  fi
  if ! docker compose version >/dev/null 2>&1 && ! command -v docker-compose >/dev/null 2>&1; then
    echo "缺少 Docker Compose，请安装 Compose 插件或 docker-compose 命令。" >&2
    exit 1
  fi
}

compose() {
  local compose_env_file="${ENV_FILE}"
  local compose_command=()

  if [[ ! -f "${compose_env_file}" ]]; then
    compose_env_file="${DOCKER_DIR}/.env.example"
  fi

  if docker compose version >/dev/null 2>&1; then
    compose_command=(docker compose)
  elif command -v docker-compose >/dev/null 2>&1; then
    compose_command=(docker-compose)
  else
    echo "缺少 Docker Compose，请安装 Compose 插件或 docker-compose 命令。" >&2
    exit 1
  fi

  "${compose_command[@]}" \
    --env-file "${compose_env_file}" \
    --file "${COMPOSE_FILE}" \
    "$@"
}

prepare() {
  if [[ -e "${DEFAULT_ENV_FILE}" ]]; then
    echo "保留现有环境变量文件：${DEFAULT_ENV_FILE}"
  else
    cp "${DOCKER_DIR}/.env.example" "${DEFAULT_ENV_FILE}"
    echo "已创建环境变量文件：${DEFAULT_ENV_FILE}"
  fi

  if [[ -e "${DEFAULT_CONFIG_FILE}" ]]; then
    echo "保留现有后端配置：${DEFAULT_CONFIG_FILE}"
  else
    cp "${DOCKER_DIR}/application-docker.example.yml" "${DEFAULT_CONFIG_FILE}"
    echo "已创建后端配置：${DEFAULT_CONFIG_FILE}"
  fi

  echo
  echo "下一步："
  echo "1. 修改 ${DEFAULT_ENV_FILE} 中的 MySQL、PGVector 和 RustFS 服务端参数。"
  echo "2. 修改 ${DEFAULT_CONFIG_FILE} 中的数据库、管理员和 S3 客户端参数。"
  echo "3. 保持两处 MySQL/RustFS 凭据一致，然后执行 bash build-docker.sh deploy。"
}

resolve_config_file() {
  local config_source=""

  config_source="${LANGCHAT_CONFIG_SOURCE:-$(sed -n 's/^LANGCHAT_CONFIG_SOURCE=//p' "${ENV_FILE}" | tail -n 1)}"
  config_source="${config_source#\"}"
  config_source="${config_source%\"}"
  config_source="${config_source#\'}"
  config_source="${config_source%\'}"
  if [[ -z "${config_source}" ]]; then
    config_source="./application-docker.yml"
  fi

  if [[ "${config_source}" = /* ]]; then
    printf '%s\n' "${config_source}"
  else
    printf '%s\n' "${DOCKER_DIR}/${config_source#./}"
  fi
}

ensure_runtime_files() {
  local config_file=""

  if [[ ! -f "${ENV_FILE}" ]]; then
    echo "缺少环境变量文件：${ENV_FILE}" >&2
    echo "请先执行：bash build-docker.sh prepare" >&2
    exit 1
  fi

  config_file="$(resolve_config_file)"
  if [[ ! -f "${config_file}" ]]; then
    echo "缺少后端配置文件：${config_file}" >&2
    echo "请检查 ${ENV_FILE} 中的 LANGCHAT_CONFIG_SOURCE。" >&2
    exit 1
  fi

  if grep -Fqi 'change-me' "${ENV_FILE}" "${config_file}"; then
    echo "本地配置仍包含 change-me，请先设置真实密码和密钥。" >&2
    exit 1
  fi

  if grep -Eq '^MYSQL_PASSWORD=123456([[:space:]]*)$' "${ENV_FILE}"; then
    echo "警告：MySQL 仍使用示例密码 123456，不建议用于生产环境。" >&2
  fi
}

build_image() {
  require_docker
  echo "构建 LangChat 应用镜像"
  compose build langchat
  echo
  echo "镜像构建完成。可执行 bash build-docker.sh up 启动。"
}

command_name="${1:-build}"
case "${command_name}" in
  prepare)
    prepare
    ;;
  build)
    build_image
    ;;
  deploy)
    require_docker
    ensure_runtime_files
    build_image
    compose up --detach --no-build
    compose ps
    ;;
  up)
    require_docker
    ensure_runtime_files
    compose up --detach --no-build
    compose ps
    ;;
  down)
    require_docker
    compose down
    ;;
  restart)
    require_docker
    ensure_runtime_files
    compose restart langchat
    compose ps
    ;;
  logs)
    require_docker
    compose logs --follow --tail=200 "${2:-langchat}"
    ;;
  status)
    require_docker
    compose ps
    ;;
  config)
    require_docker
    compose config
    ;;
  release)
    bash "${ROOT_DIR}/build-release.sh"
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    echo "未知命令：${command_name}" >&2
    usage >&2
    exit 1
    ;;
esac
