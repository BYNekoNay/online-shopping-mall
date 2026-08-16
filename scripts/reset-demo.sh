#!/usr/bin/env bash
# =============================================================================
# 演示环境一键重置脚本（E-3，docs/35 阶段 E）
# -----------------------------------------------------------------------------
# 用法：
#   ./scripts/reset-demo.sh            # Docker 方式（默认，容器 mall-mysql）
#   ./scripts/reset-demo.sh --local    # 本地 MySQL（需 mysql 客户端）
#
# 功能：drop 重建 mall 库 → 导入 V1__init_schema.sql（30 表）→
#       init-data.sql（三端演示账号/分类/商品/促销/积分商品/物流公司）→
#       触发一次推荐预热（可选 --warmup，调用后端刷新接口）。
# 幂等：可重复执行；演示前执行恢复到干净可演示状态。
# =============================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQL_DIR="$ROOT_DIR/backend/src/main/resources/sql"
DATA_SQL="$ROOT_DIR/scripts/init-data.sql"
SCHEMA_SQL="$SQL_DIR/V1__init_schema.sql"
MODE="docker"
WARMUP=""
for arg in "$@"; do
  case "$arg" in
    --local) MODE="local" ;;
    --warmup) WARMUP="warmup" ;;
  esac
done
# 密码允许为空（本地 root 常无密码）；docker 模式未显式指定时默认 root123
MYSQL_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
if [[ "$MODE" == "docker" && -z "$MYSQL_PASSWORD" ]]; then
  MYSQL_PASSWORD="root123"
fi
MYSQL_DATABASE="${MYSQL_DATABASE:-mall}"

if [[ ! -f "$SCHEMA_SQL" || ! -f "$DATA_SQL" ]]; then
  echo "[reset-demo] 错误：找不到 SQL 文件"
  echo "  schema: $SCHEMA_SQL"
  echo "  data  : $DATA_SQL"
  exit 1
fi

echo "======================================================"
echo " 演示环境重置（mode=$MODE, db=$MYSQL_DATABASE）"
echo "======================================================"

# 密码为空时不传 -p（避免空密码被当作错误密码）
MYSQL_AUTH=()
if [[ -n "$MYSQL_PASSWORD" ]]; then
  MYSQL_AUTH=(-p"$MYSQL_PASSWORD")
fi

run_mysql() {
  # $@ 为要执行的 SQL（管道输入）；--default-character-set=utf8mb4 防止中文乱码/超长
  if [[ "$MODE" == "local" ]]; then
    mysql --default-character-set=utf8mb4 -uroot "${MYSQL_AUTH[@]}" "$@"
  else
    docker exec -i mall-mysql mysql --default-character-set=utf8mb4 -uroot "${MYSQL_AUTH[@]}" "$@"
  fi
}

echo "[1/4] 重建数据库 $MYSQL_DATABASE ..."
run_mysql <<SQL
DROP DATABASE IF EXISTS \`$MYSQL_DATABASE\`;
CREATE DATABASE \`$MYSQL_DATABASE\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SQL

echo "[2/4] 导入表结构（V1__init_schema.sql，30 张表）..."
run_mysql "$MYSQL_DATABASE" < "$SCHEMA_SQL"

echo "[3/4] 导入初始化数据（init-data.sql）..."
run_mysql "$MYSQL_DATABASE" < "$DATA_SQL"

echo "[4/4] 校验表数量..."
TABLE_COUNT="$(run_mysql "$MYSQL_DATABASE" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$MYSQL_DATABASE';")"
echo "  当前表数量: $TABLE_COUNT（预期 30）"

# 可选：推荐预热（调用后端刷新接口，需容器启动）
if [[ "$WARMUP" == "warmup" ]]; then
  echo "[可选] 触发推荐预热..."
  BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
  curl -sf -X POST "$BACKEND_URL/api/admin/recommend/refresh" -H "Authorization: Bearer $DEMO_ADMIN_TOKEN" \
    >/dev/null 2>&1 && echo "  推荐预热完成" || echo "  预热跳过（后端未启动或未提供 token，不影响演示）"
fi

echo ""
echo "✅ 演示环境重置完成！"
echo "  管理员: admin / Admin@2026（V1 建表时创建）"
echo "  消费者: testuser / Mall@2026（init-data 创建）"
echo "  商家：演示时由管理员在【用户管理】将 testuser 分配为商家角色 → 商家端入驻申请 → 管理员审核通过"
echo "  浏览器访问: http://localhost（或 docker-compose 配置的端口）"
