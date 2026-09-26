#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_DIR="${LANGCHAT_RELEASE_DIR:-${ROOT_DIR}/release}"
BACKEND_JAR="${ROOT_DIR}/langchat-server/target/langchat-server.jar"
FRONTEND_DIR="${ROOT_DIR}/langchat-ui"
FRONTEND_DIST="${FRONTEND_DIR}/apps/langchat/dist"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少构建工具：$1" >&2
    exit 1
  fi
}

for command_name in java mvn node pnpm; do
  require_command "${command_name}"
done

JAVA_MAJOR="$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"
if [[ -z "${JAVA_MAJOR}" || "${JAVA_MAJOR}" -lt 17 ]]; then
  echo "Java 版本过低，需要 Java 17 或更高版本。" >&2
  exit 1
fi

case "${RELEASE_DIR}" in
  ""|"/"|"${ROOT_DIR}")
    echo "不安全的交付目录：${RELEASE_DIR}" >&2
    exit 1
    ;;
esac

echo "[1/4] 构建后端可执行 JAR"
cd "${ROOT_DIR}"
mvn -B -DskipTests -pl langchat-server -am package

if [[ ! -s "${BACKEND_JAR}" ]]; then
  echo "后端构建完成，但未找到启动文件：${BACKEND_JAR}" >&2
  exit 1
fi

echo "[2/4] 构建前端静态资源"
cd "${FRONTEND_DIR}"
# release 构建只使用现有依赖，不执行依赖安装。关闭 Corepack 的项目版本自动切换，
# 避免离线服务器因尝试下载 packageManager 声明的 pnpm 版本而失败。
export COREPACK_ENABLE_PROJECT_SPEC=0
pnpm --filter @vben/web-naive build

if [[ ! -s "${FRONTEND_DIST}/index.html" ]]; then
  echo "前端构建完成，但未找到 index.html：${FRONTEND_DIST}" >&2
  exit 1
fi

echo "[3/4] 生成交付目录"
STAGING_DIR="$(mktemp -d "${ROOT_DIR}/.release.tmp.XXXXXX")"
cleanup() {
  if [[ -d "${STAGING_DIR}" ]]; then
    rm -rf "${STAGING_DIR}"
  fi
}
trap cleanup EXIT

mkdir -p \
  "${STAGING_DIR}/app" \
  "${STAGING_DIR}/config" \
  "${STAGING_DIR}/html" \
  "${STAGING_DIR}/logs" \
  "${STAGING_DIR}/nginx" \
  "${STAGING_DIR}/sql" \
  "${STAGING_DIR}/workspace/skills"

cp "${BACKEND_JAR}" "${STAGING_DIR}/app/langchat.jar"
cp -R "${FRONTEND_DIST}/." "${STAGING_DIR}/html/"
cp "${ROOT_DIR}/langchat-server/src/main/resources/application.yml" "${STAGING_DIR}/config/application.yml"
cp "${ROOT_DIR}/langchat-server/src/main/resources/application-prod.yml" "${STAGING_DIR}/config/application-prod.yml"
cp "${ROOT_DIR}/docker/application-docker.example.yml" "${STAGING_DIR}/config/application-runtime.example.yml"
cp "${ROOT_DIR}/docker/nginx/default.conf" "${STAGING_DIR}/nginx/default.conf"
cp "${ROOT_DIR}/docs/db/langchat.sql" "${STAGING_DIR}/sql/01-langchat.sql"
cp "${ROOT_DIR}/README.md" "${STAGING_DIR}/README.md"
cp "${ROOT_DIR}/LICENSE" "${STAGING_DIR}/LICENSE"
if [[ -f "${ROOT_DIR}/NOTICE" ]]; then
  cp "${ROOT_DIR}/NOTICE" "${STAGING_DIR}/NOTICE"
fi

echo "[4/4] 检查交付内容"
for required_file in \
  app/langchat.jar \
  config/application.yml \
  config/application-prod.yml \
  config/application-runtime.example.yml \
  html/index.html \
  nginx/default.conf \
  sql/01-langchat.sql \
  README.md \
  LICENSE; do
  if [[ ! -s "${STAGING_DIR}/${required_file}" ]]; then
    echo "交付文件缺失或为空：${required_file}" >&2
    exit 1
  fi
done

if [[ -e "${RELEASE_DIR}" ]]; then
  rm -rf "${RELEASE_DIR}"
fi
mv "${STAGING_DIR}" "${RELEASE_DIR}"
trap - EXIT

echo
echo "交付目录已生成：${RELEASE_DIR}"
echo "后端启动时请复制并修改 config/application-runtime.example.yml，"
echo "然后通过 --spring.config.additional-location=file:<配置路径> 加载。"
