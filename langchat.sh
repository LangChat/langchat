#!/usr/bin/env bash
#
# LangChat 前后端一键管理脚本
#
# 用法:
#   ./langchat.sh                 # 一键启动前后端并实时打印全部日志（Ctrl+C / Ctrl+D 停止服务并退出）
#   ./langchat.sh start [-d]      # 同上；-d 表示后台运行（关闭窗口服务不停）
#   ./langchat.sh stop            # 停止前后端
#   ./langchat.sh restart [-d]    # 重启前后端
#   ./langchat.sh status          # 查看前后端运行状态
#   ./langchat.sh logs [all|backend|frontend]  # 跟随查看日志
#   ./langchat.sh build           # 编译安装后端模块
#   ./langchat.sh help            # 查看帮助

set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT/.run"
LOG_DIR="$ROOT/logs"
BACKEND_PID_FILE="$RUN_DIR/backend.pid"
FRONTEND_PID_FILE="$RUN_DIR/frontend.pid"
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"
LOG_TAIL_PID=""

BACKEND_PORT="${LANGCHAT_BACKEND_PORT:-8080}"
FRONTEND_PORT="${LANGCHAT_FRONTEND_PORT:-5888}"

BACKEND_MATCH="cn.langchat.server.LangchatServerApplication|langchat-server spring-boot:run"

C_G="\033[32m"; C_Y="\033[33m"; C_R="\033[31m"; C_C="\033[36m"; C_0="\033[0m"
info() { printf "${C_C}[langchat]${C_0} %s\n" "$*"; }
ok()   { printf "${C_G}[langchat]${C_0} %s\n" "$*"; }
warn() { printf "${C_Y}[langchat]${C_0} %s\n" "$*"; }
err()  { printf "${C_R}[langchat]${C_0} %s\n" "$*" >&2; }

mkdir -p "$LOG_DIR" "$RUN_DIR"

port_pids() { lsof -t -i "tcp:$1" -sTCP:LISTEN 2>/dev/null | sort -u; }
alive() { [ -n "${1:-}" ] && kill -0 "$1" 2>/dev/null; }
read_pid() { cat "$1" 2>/dev/null | head -1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || { err "未找到命令 ${1}，请先安装并确保在 PATH 中"; exit 1; }
}

# 收集前后端相关的所有进程（pid 文件 + 端口监听 + 后端命令行特征兜底）
service_pids() {
  {
    cat "$BACKEND_PID_FILE" 2>/dev/null
    cat "$FRONTEND_PID_FILE" 2>/dev/null
    port_pids "$BACKEND_PORT"
    port_pids "$FRONTEND_PORT"
    pgrep -f "$BACKEND_MATCH" 2>/dev/null
  } | sort -u
}

empty_list() { [ -z "${1// /}" ]; }

stop_all() {
  local pids
  pids="$(service_pids | tr '\n' ' ')"
  if empty_list "$pids"; then
    ok "当前没有运行中的服务"
    return 0
  fi
  info "停止进程: $pids"
  kill -TERM $pids 2>/dev/null
  local i=0
  while [ "$i" -lt 15 ]; do
    pids="$(service_pids | tr '\n' ' ')"
    empty_list "$pids" && break
    sleep 1
    i=$((i + 1))
  done
  pids="$(service_pids | tr '\n' ' ')"
  if ! empty_list "$pids"; then
    warn "强制结束: $pids"
    kill -9 $pids 2>/dev/null
    sleep 1
  fi
  rm -f "$BACKEND_PID_FILE" "$FRONTEND_PID_FILE"
  ok "前后端服务已停止"
}

wait_port_up() { # $1=端口 $2=超时秒 $3=pid 文件（可选）
  local waited=0
  local pid_file="${3:-}"
  local process_pid=""
  while [ "$waited" -lt "$2" ]; do
    [ -n "$(port_pids "$1")" ] && return 0
    if [ -n "$pid_file" ]; then
      process_pid="$(read_pid "$pid_file")"
      if ! alive "$process_pid"; then
        return 2
      fi
    fi
    sleep 2
    waited=$((waited + 2))
  done
  return 1
}

start_backend() {
  require_cmd mvn
  info "启动后端..."
  : > "$BACKEND_LOG"
  (
    cd "$ROOT" || exit 1
    nohup mvn -pl langchat-server spring-boot:run -DskipTests >>"$BACKEND_LOG" 2>&1 &
    echo $! >"$BACKEND_PID_FILE"
  )
  ok "后端进程已拉起 (pid $(read_pid "$BACKEND_PID_FILE"))，日志: $BACKEND_LOG"
}

start_frontend() {
  require_cmd pnpm
  info "启动前端..."
  : > "$FRONTEND_LOG"
  (
    cd "$ROOT/langchat-ui/apps/langchat" || exit 1
    nohup env COREPACK_ENABLE_PROJECT_SPEC=0 pnpm dev >>"$FRONTEND_LOG" 2>&1 &
    echo $! >"$FRONTEND_PID_FILE"
  )
  ok "前端进程已拉起 (pid $(read_pid "$FRONTEND_PID_FILE"))，日志: $FRONTEND_LOG"
}

start_log_stream() {
  if alive "$LOG_TAIL_PID"; then
    return 0
  fi
  info "开始实时输出前后端日志（从本次启动的第一行开始）"
  printf "%s\n" "==================== LangChat 前后端日志 ===================="
  tail -n +1 -F "$BACKEND_LOG" "$FRONTEND_LOG" &
  LOG_TAIL_PID=$!
}

stop_log_stream() {
  if alive "$LOG_TAIL_PID"; then
    kill "$LOG_TAIL_PID" 2>/dev/null
    wait "$LOG_TAIL_PID" 2>/dev/null
  fi
  LOG_TAIL_PID=""
}

cleanup_attached() {
    local exit_code="${1:-0}"
    trap '' INT TERM HUP
    printf "\n"
    info "检测到退出，正在停止前后端服务..."
    stop_log_stream
    stop_all
    ok "已全部停止，再见"
    exit "$exit_code"
}

enable_attached_mode() {
  trap 'cleanup_attached 130' INT
  trap 'cleanup_attached 143' TERM HUP
  start_log_stream
}

run_attached() {
  info "实时输出前后端日志，按 Ctrl+C 或 Ctrl+D 停止服务并退出"
  if [ -t 0 ]; then
    # 终端里运行：读到 EOF（Ctrl+D）时退出
    while alive "$LOG_TAIL_PID"; do
      read -r -t 5
      rc=$?
      [ "$rc" -eq 1 ] && break
    done
  else
    # 非终端运行（如后台脚本）：轮询代替 wait，确保 trap 信号能被处理
    while alive "$LOG_TAIL_PID"; do
      sleep 1
    done
  fi
  cleanup_attached 0
}

cmd_start() {
  local daemon=0 nobuild=0
  while [ $# -gt 0 ]; do
    case "$1" in
      -d | --daemon) daemon=1 ;;
      -n | --no-build) nobuild=1 ;;
      *) err "未知参数: $1"; usage; exit 1 ;;
    esac
    shift
  done

  local skip_backend=0 skip_frontend=0
  [ -n "$(port_pids "$BACKEND_PORT")" ] && { warn "端口 $BACKEND_PORT 已被占用，跳过后端启动"; skip_backend=1; }
  [ -n "$(port_pids "$FRONTEND_PORT")" ] && { warn "端口 $FRONTEND_PORT 已被占用，跳过前端启动"; skip_frontend=1; }

  if [ "$skip_backend" = 1 ] && [ "$skip_frontend" = 1 ]; then
    ok "前后端均已在运行"
    if [ "$daemon" = 1 ]; then
      show_status
      return 0
    fi
    enable_attached_mode
    run_attached
    return 0
  fi

  if [ "$skip_backend" = 0 ]; then
    if [ "$nobuild" = 0 ]; then
      info "编译后端模块（增量编译，可用 --no-build 跳过）..."
      (cd "$ROOT" && mvn -pl langchat-server -am install -DskipTests) || { err "后端编译失败"; exit 1; }
    fi
    start_backend
  fi
  if [ "$skip_frontend" = 0 ]; then
    start_frontend
  fi

  if [ "$daemon" = 0 ]; then
    enable_attached_mode
  fi

  info "等待服务就绪..."
  if [ "$skip_backend" = 0 ]; then
    wait_port_up "$BACKEND_PORT" 180 "$BACKEND_PID_FILE"
    wait_status=$?
    if [ "$wait_status" -ne 0 ]; then
      if [ "$wait_status" -eq 2 ]; then
        err "后端进程已提前退出，最近日志:"
      else
        err "后端未在 180 秒内监听 $BACKEND_PORT 端口，最近日志:"
      fi
      tail -n 40 "$BACKEND_LOG"
      stop_log_stream
      stop_all
      return 1
    fi
    ok "后端已就绪: http://localhost:$BACKEND_PORT"
  fi
  if [ "$skip_frontend" = 0 ]; then
    wait_port_up "$FRONTEND_PORT" 60 "$FRONTEND_PID_FILE"
    wait_status=$?
    if [ "$wait_status" -ne 0 ]; then
      if [ "$wait_status" -eq 2 ]; then
        err "前端进程已提前退出，最近日志:"
      else
        err "前端未在 60 秒内监听 $FRONTEND_PORT 端口，最近日志:"
      fi
      tail -n 40 "$FRONTEND_LOG"
      stop_log_stream
      stop_all
      return 1
    fi
    ok "前端已就绪: http://localhost:$FRONTEND_PORT"
  fi

  if [ "$daemon" = 1 ]; then
    ok "已在后台运行，日志: $BACKEND_LOG 与 ${FRONTEND_LOG}（停止: ./langchat.sh stop）"
    return 0
  fi
  run_attached
}

cmd_build() {
  require_cmd mvn
  info "编译并安装后端模块..."
  (cd "$ROOT" && mvn -pl langchat-server -am install -DskipTests)
}

show_logs() {
  case "${1:-all}" in
    backend) tail -n 100 -F "$BACKEND_LOG" ;;
    frontend) tail -n 100 -F "$FRONTEND_LOG" ;;
    all) tail -n 30 -F "$BACKEND_LOG" "$FRONTEND_LOG" ;;
    *) err "用法: $0 logs [all|backend|frontend]"; exit 1 ;;
  esac
}

show_status() {
  local bpid fpid bstate fstate
  bpid="$(port_pids "$BACKEND_PORT" | head -1)"
  [ -z "$bpid" ] && bpid="$(read_pid "$BACKEND_PID_FILE")"
  if alive "$bpid"; then bstate="运行中"; else bstate="已停止"; bpid="-"; fi
  fpid="$(port_pids "$FRONTEND_PORT" | head -1)"
  [ -z "$fpid" ] && fpid="$(read_pid "$FRONTEND_PID_FILE")"
  if alive "$fpid"; then fstate="运行中"; else fstate="已停止"; fpid="-"; fi

  printf "\n"
  echo "  后端: $bstate  pid $bpid  端口 $BACKEND_PORT  日志 $BACKEND_LOG"
  echo "  前端: $fstate  pid $fpid  端口 $FRONTEND_PORT  日志 $FRONTEND_LOG"
  printf "\n"
}

usage() {
  cat <<'EOF'
用法: ./langchat.sh [命令] [参数]

命令:
  start [-d] [--no-build]      一键启动前后端并实时打印全部日志（默认命令）
                               -d          后台运行，关闭窗口服务不停
                               --no-build  跳过后端增量编译
  stop                         停止前后端服务
  restart [-d] [--no-build]    重启前后端
  status                       查看前后端运行状态
  logs [all|backend|frontend]  跟随查看日志（默认全部）
  build                        编译安装后端模块（后端代码变更后可手动执行）
  help                         显示本帮助

说明:
  默认（不带参数）启动后，终端会持续打印前后端日志；
  按 Ctrl+C 或 Ctrl+D（或直接关闭窗口）会自动停止前后端服务。
  日志保存在 logs/ 目录下，pid 文件保存在 .run/ 目录下。
EOF
}

main() {
  local cmd="${1:-start}"
  [ $# -gt 0 ] && shift
  case "$cmd" in
    start) cmd_start "$@" ;;
    stop) stop_all ;;
    restart) stop_all
      sleep 1
      cmd_start "$@" ;;
    status) show_status ;;
    logs) show_logs "${1:-all}" ;;
    build) cmd_build ;;
    help | -h | --help) usage ;;
    *) err "未知命令: $cmd"
      usage
      exit 1 ;;
  esac
}

main "$@"
