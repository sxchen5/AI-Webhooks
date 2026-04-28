# GitLab 代码提交扫描平台

Spring Boot 3 + Vue3 + Element Plus + MySQL。GitLab `push` WebHook 触发异步 Shell 扫描，结果写入 `scan_task_log`，失败时按项目配置发邮件告警。

## 目录

- `scan-platform-backend`：后端（Maven）
- `scan-platform-frontend`：前端（Vite）

## 数据库

1. 创建库：`CREATE DATABASE scan_platform DEFAULT CHARACTER SET utf8mb4;`
2. 修改 `scan-platform-backend/src/main/resources/application.yml` 中的数据源账号密码（或通过环境变量覆盖）。
3. 首次启动会自动执行 `schema.sql` 与 `data.sql`（建表 + 默认管理员）。

默认登录：`admin` / `admin123`（BCrypt 存储在 `data.sql`）。

## 运行后端

```bash
cd scan-platform-backend
mvn spring-boot:run
```

服务默认 `http://localhost:8080`。

- 登录：`POST /api/auth/login`
- WebHook：`POST /api/webhook/gitlab`（Header `X-Gitlab-Token` 与系统配置中的 Token 一致；可选 IP 白名单）
- 管理接口：需在 Header 携带 `Authorization: Bearer <JWT>`

## 运行前端

```bash
cd scan-platform-frontend
npm install
npm run dev
```

开发环境通过 Vite 代理将 `/api` 转发到 `8080`。生产构建可将 `dist` 交由 Nginx 托管，并反向代理 `/api` 到后端。

## Cursor CLI（非交互）与 `agent_command` 怎么写

平台在子进程里执行你在 **项目管理 → Agent 命令** 里配置的整条 Shell（工作目录为 **本地代码目录**），并支持占位符：

- `{{path}}`：与 `local_code_path` 相同  
- `{{branch}}`、`{{commit}}`：来自 GitLab push 负载  

此外会向子进程注入环境变量（便于脚本不写占位符）：`WEBHOOK_REPO_PATH`、`WEBHOOK_BRANCH`、`WEBHOOK_COMMIT`、`WEBHOOK_PROJECT_NAME`、`WEBHOOK_GITLAB_PROJECT_ID`、`WEBHOOK_COMMIT_USER`、`WEBHOOK_GIT_URL`。

### 推荐：使用仓库内包装脚本

1. 在扫描服务器安装 [Cursor CLI](https://cursor.com/docs/cli/overview)，保证 `agent` 在 PATH 中（或设置 `CURSOR_AGENT_BIN`）。
2. 将 `local_code_path` 指向该 GitLab 项目在机器上的 **clone 目录**（需已配置 `origin`，脚本会尝试 `fetch` / `checkout` 到对应分支与 commit）。
3. **agent_command** 填脚本绝对路径，例如：

```bash
/bin/bash /path/to/AI-Webhooks/scripts/cursor-gitlab-webhook-scan.sh
```

（把 `/path/to/AI-Webhooks` 换成本仓库在服务器上的真实路径，例如 `/opt/scan-platform`。）

4. 可选：为运行后端的进程设置 `CURSOR_AGENT_MODEL`（如 `gpt-5.2`）、`CURSOR_SCAN_PROMPT`（自定义 `-p` 提示词）。

### 是否要写 Cursor Skill

- **Skill 不由 WebHook 直接触发**；Skill 供 Cursor `agent` 加载，用于统一扫描口径。  
- 本仓库已提供示例 Skill：`.cursor/skills/gitlab-webhook-cursor-scan/SKILL.md`。请将其**复制到被扫描的业务仓库**的 `.cursor/skills/gitlab-webhook-cursor-scan/SKILL.md`，或安装到用户级 `~/.cursor/skills/`，这样在该仓库里跑 `agent` 时更容易遵循同一规范。

### 最小手写示例（不依赖脚本）

若已同步好代码目录，可直接写（模型名按本机 CLI 支持调整）：

```bash
agent -p "review these changes for security issues" --output-format text --model "gpt-5.2"
```

## GitLab 配置

在仓库 **Settings → Webhooks** 添加 URL：`https://<你的域名>/api/webhook/gitlab`，Secret Token 与「系统配置」中的 Webhook Token 一致，触发事件勾选 **Push events**。
