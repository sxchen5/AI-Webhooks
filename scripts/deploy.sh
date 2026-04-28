#!/usr/bin/env bash
#
# 一键打包并部署为「单 jar + 外置配置」形态（适合 Maven 3.3.9 + java -jar）。
# 用法：
#   ./scripts/deploy.sh                    # 打包到 ./deploy，jar 名带版本
#   ./scripts/deploy.sh /opt/scan-platform # 指定部署目录
# 环境变量：
#   MVN         Maven 可执行文件（默认 mvn）
#   JAVA_HOME   可选，用于运行 java
#   SKIP_TESTS  设为 0 则运行测试（默认 1，加快部署）
#   FRONTEND_SKIP 设为 1 则 -Dfrontend.skip=true（仅后端，不 npm）
#

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY_DIR="${1:-${ROOT}/deploy}"
MVN="${MVN:-mvn}"

echo "[deploy] repo root: $ROOT"
echo "[deploy] target dir: $DEPLOY_DIR"

SKIP_FLAG=(-DskipTests)
if [[ "${SKIP_TESTS:-1}" == "0" ]]; then
  SKIP_FLAG=()
fi

FRONT_FLAG=()
if [[ "${FRONTEND_SKIP:-0}" == "1" ]]; then
  FRONT_FLAG=(-Dfrontend.skip=true)
fi

mkdir -p "$DEPLOY_DIR"

echo "[deploy] running Maven package (embeds frontend dist into jar)..."
(cd "$ROOT/scan-platform-backend" && "$MVN" -q clean package "${SKIP_FLAG[@]}" "${FRONT_FLAG[@]}")

JAR_SRC="$ROOT/scan-platform-backend/target/scan-platform-backend-1.0.0.jar"
if [[ ! -f "$JAR_SRC" ]]; then
  echo "[deploy] ERROR: jar not found: $JAR_SRC" >&2
  exit 1
fi

cp -f "$JAR_SRC" "$DEPLOY_DIR/scan-platform.jar"

# 外置配置模板（首次可复制为 application.yml 再改库地址等）
if [[ ! -f "$DEPLOY_DIR/application.yml" ]]; then
  cat >"$DEPLOY_DIR/application.yml.example" <<'YAML'
# 复制为 application.yml 后按需修改；启动时使用：
#   java -jar scan-platform.jar --spring.config.additional-location=file:./application.yml

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/scan_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: your_password

# 首次建表后可关闭自动执行 SQL，避免重复插入种子数据报错：
# spring:
#   sql:
#     init:
#       mode: never
YAML
  echo "[deploy] wrote $DEPLOY_DIR/application.yml.example (copy to application.yml)"
fi

mkdir -p "$DEPLOY_DIR/logs"

cat >"$DEPLOY_DIR/run.sh" <<'EOS'
#!/usr/bin/env bash
# 在 deploy 目录下执行：同一目录放 scan-platform.jar 与可选 application.yml
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"
export LOG_DIR="${LOG_DIR:-$DIR/logs}"
exec java ${JAVA_OPTS:-} -jar scan-platform.jar \
  --spring.config.additional-location=optional:file:./application.yml \
  "$@"
EOS
chmod +x "$DEPLOY_DIR/run.sh"

echo "[deploy] done."
echo "  jar:  $DEPLOY_DIR/scan-platform.jar"
echo "  run:  $DEPLOY_DIR/run.sh"
echo "  logs: \$LOG_DIR (default $DEPLOY_DIR/logs)"
echo ""
echo "生产启动示例："
echo "  cd $DEPLOY_DIR && ./run.sh"
