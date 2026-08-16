#!/usr/bin/env bash
# =============================================================================
# 纯本地一键启动脚本（G 阶段，无 Docker）
# -----------------------------------------------------------------------------
# 前置：本地 MySQL(3306) + Redis(6379) 已启动；JDK17 + Node18+
# 用法：
#   ./scripts/start-local.sh          # 仅启动后端(8080) + 前端预览(4173)
#   ./scripts/start-local.sh --reset  # 先重建数据库（reset-demo.sh --local）再启动
#   ./scripts/start-local.sh --stop   # 停止本次启动的进程
# -----------------------------------------------------------------------------
# 环境变量：DB_PASSWORD（本地 MySQL root 密码，默认 123456）
# =============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_PASSWORD="${DB_PASSWORD:-123456}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-4173}"
LOG_DIR="${TMPDIR:-/tmp}/mall-local"
mkdir -p "$LOG_DIR"

PID_FILE="$LOG_DIR/pids.txt"

stop_all() {
  if [[ -f "$PID_FILE" ]]; then
    while read -r pid; do
      kill "$pid" 2>/dev/null || true
    done < "$PID_FILE"
    rm -f "$PID_FILE"
  fi
  echo "[start-local] 已停止本地进程"
}

if [[ "${1:-}" == "--stop" ]]; then
  stop_all
  exit 0
fi

if [[ "${1:-}" == "--reset" ]]; then
  echo "[start-local] 重建数据库..."
  MYSQL_ROOT_PASSWORD="$DB_PASSWORD" bash "$ROOT_DIR/scripts/reset-demo.sh" --local
fi

# 1. 打包后端（若 jar 不存在或源码更新）
JAR_FILE="$ROOT_DIR/backend/target/mall-1.0.0.jar"
if [[ ! -f "$JAR_FILE" ]]; then
  echo "[start-local] 打包后端..."
  (cd "$ROOT_DIR/backend" && ./mvnw package -q -DskipTests)
fi

# 2. 启动后端（dev profile，固定端口）
JAVA_BIN="$(command -v java || echo /d/java/bin/java)"
echo "[start-local] 启动后端 :$BACKEND_PORT ..."
DB_PASSWORD="$DB_PASSWORD" "$JAVA_BIN" -jar "$JAR_FILE" \
  --spring.profiles.active=dev --server.port="$BACKEND_PORT" \
  > "$LOG_DIR/backend.log" 2>&1 &
echo $! >> "$PID_FILE"

# 3. 构建前端（若 dist 缺失）
if [[ ! -d "$ROOT_DIR/frontend/dist" ]]; then
  echo "[start-local] 构建前端..."
  (cd "$ROOT_DIR/frontend" && npx vite build)
fi

# 4. 启动前端预览（代理 /api、/uploads → 后端）
echo "[start-local] 启动前端预览 :$FRONTEND_PORT ..."
(cd "$ROOT_DIR/frontend" && npx vite preview --host --port "$FRONTEND_PORT") \
  > "$LOG_DIR/frontend.log" 2>&1 &
echo $! >> "$PID_FILE"

echo ""
echo "[start-local] 启动完成，等待就绪..."
sleep 12
curl -s -o /dev/null -w "  后端 /api/products → %{http_code}\n" "http://localhost:$BACKEND_PORT/api/products?pageNum=1" || true
curl -s -o /dev/null -w "  前端首页 → %{http_code}\n" "http://localhost:$FRONTEND_PORT/" || true
echo ""
echo "  ✅ 访问：http://localhost:$FRONTEND_PORT"
echo "  ✅ 管理员 admin / Admin@2026    消费者 testuser / Mall@2026"
echo "  停止：./scripts/start-local.sh --stop"
