#!/usr/bin/env bash
# GitLab WebHook → 扫描平台 子进程入口：同步本地仓库并以 Cursor CLI 非交互模式跑 agent。
# 依赖：本机已安装 Cursor CLI（https://cursor.com/docs/cli/overview），且 `agent` 在 PATH 中。
# 环境变量由后端注入：WEBHOOK_REPO_PATH、WEBHOOK_BRANCH、WEBHOOK_COMMIT 等（见 README）。

set -euo pipefail

REPO="${WEBHOOK_REPO_PATH:-${SCAN_REPO_PATH:-}}"
if [[ -z "$REPO" ]]; then
  echo "ERROR: WEBHOOK_REPO_PATH or SCAN_REPO_PATH must be set" >&2
  exit 2
fi

if [[ ! -d "$REPO" ]]; then
  echo "ERROR: repository path does not exist: $REPO" >&2
  exit 2
fi

cd "$REPO"

# 若为 git 仓库，尽量与远端分支对齐（需已配置 remote；失败不阻断 agent，仅打印警告）
if [[ -d .git ]]; then
  if [[ -n "${WEBHOOK_BRANCH:-}" ]]; then
    if git remote | grep -q '^origin$'; then
      git fetch origin --prune 2>&1 || echo "WARN: git fetch failed, continuing" >&2
      if git show-ref --verify --quiet "refs/remotes/origin/${WEBHOOK_BRANCH}" 2>/dev/null; then
        git checkout -B "$WEBHOOK_BRANCH" "origin/${WEBHOOK_BRANCH}" 2>&1 || echo "WARN: git checkout failed, continuing" >&2
      elif git show-ref --verify --quiet "refs/heads/${WEBHOOK_BRANCH}" 2>/dev/null; then
        git checkout "$WEBHOOK_BRANCH" 2>&1 || true
        git pull --ff-only 2>&1 || echo "WARN: git pull failed, continuing" >&2
      else
        echo "WARN: branch origin/${WEBHOOK_BRANCH} not found after fetch" >&2
      fi
    fi
    if [[ -n "${WEBHOOK_COMMIT:-}" && "${WEBHOOK_COMMIT}" != 0000000000000000000000000000000000000000 ]]; then
      git cat-file -e "${WEBHOOK_COMMIT}^{commit}" 2>/dev/null && git checkout --detach "$WEBHOOK_COMMIT" 2>&1 \
        || echo "WARN: could not checkout commit ${WEBHOOK_COMMIT}, staying on branch" >&2
    fi
  fi
fi

AGENT_BIN="${CURSOR_AGENT_BIN:-agent}"
if ! command -v "$AGENT_BIN" >/dev/null 2>&1; then
  echo "ERROR: Cursor CLI not found: $AGENT_BIN (install from https://cursor.com/docs/cli/overview )" >&2
  exit 127
fi

PROMPT="${CURSOR_SCAN_PROMPT:-Review the latest changes for security issues and obvious bugs. Output a concise summary only; do not apply fixes unless clearly safe. Context: GitLab push branch ${WEBHOOK_BRANCH:-unknown} commit ${WEBHOOK_COMMIT:-unknown}.}"

MODEL_ARGS=()
if [[ -n "${CURSOR_AGENT_MODEL:-}" ]]; then
  MODEL_ARGS=(--model "$CURSOR_AGENT_MODEL")
fi

# 非交互打印模式；将完整输出交给扫描平台写入 exec_result
exec "$AGENT_BIN" -p "$PROMPT" --output-format text "${MODEL_ARGS[@]}" "$@"
