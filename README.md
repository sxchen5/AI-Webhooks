# 代码扫描平台

Spring Boot 3 + Vue3 + Element Plus + MySQL。支持 **主动 Git 扫描**（多仓库、定时/手动、`git clone` + agent/技能）、**Git 项目 AI 问答**（按问题执行 agent）、**平台技能**、**邮件配置**；扫描日志写入 `active_scan_log`。

## 目录

- `scan-platform-backend`：后端（Maven）
- `scan-platform-frontend`：前端（Vite）

## 数据库

1. 创建库：`CREATE DATABASE scan_platform DEFAULT CHARACTER SET utf8mb4;`
2. 修改 `scan-platform-backend/src/main/resources/application.yml` 中的数据源账号密码（或通过环境变量覆盖）。
3. 首次启动会自动执行 `schema.sql` 与 `data.sql`（建表 + 默认管理员）。

**增加 Git 项目管理**：已有库在备份后执行 `migration-git-project.sql`（新建 `git_project` 表并为 `active_scan_repo` 增加 `git_project_id`）。若仍含旧版 WebHook 表，可先执行 `migration-remove-webhook.sql`。

**移除 display_commit 列**：若库由旧版升级而来且仍存在该列，执行 `migration-drop-display-commit.sql`。

**Git 项目 AI 问答**：已有库在备份后执行 `migration-git-qa-project.sql`（新建 `git_qa_project` 表）。删除「Git项目管理」中的主数据前，需先解除本功能或「Git 项目配置」中的关联。对话接口 `POST .../chat` 请求体可带可选字段 `model`（白名单：`auto`、`composer-2-fast`、`composer-2`、`composer-1.5`、`grok-4-20`、`grok-4-20-thinking`、`kimi-k2.5`），服务端在 agent 命令末尾追加 `--model <值>`；不传或清空则不追加。

**Git 问答 agent_command 默认值**：若表由旧脚本创建且 `agent_command` 无默认值，可执行 `migration-git-qa-agent-default.sql`（允许空串表示使用默认 stream-json 问答命令）。

**Git 问答聊天记录**：已有库执行 `migration-git-qa-chat-message.sql`（新建 `git_qa_chat_message` 表，用于会话持久化与单条删除）。`DELETE /api/ai-git-qa/projects/{id}/messages/{messageId}` 删除单条；`DELETE /api/ai-git-qa/projects/{id}/messages`（无子路径）清空该配置下全部记录。重新生成时 `POST .../chat` 请求体可带 `regenerate: true` 与 `userMessageId`，`question` 须与该用户消息一致，服务端不重复插入用户行。

默认登录：`admin` / `admin123`（BCrypt 存储在 `data.sql`）。

## 运行后端

```bash
cd scan-platform-backend
mvn spring-boot:run
```

服务默认 `http://localhost:8080`。

- 登录：`POST /api/auth/login`
- 管理接口：需在 Header 携带 `Authorization: Bearer <JWT>`

## 运行前端

```bash
cd scan-platform-frontend
npm install
npm run dev
```

开发环境通过 Vite 代理将 `/api` 转发到 `8080`。生产环境推荐 **单 jar 内嵌前端**（`mvn package` 会自动构建前端并打进 jar）。

## 生产打包

```bash
cd scan-platform-backend
mvn clean package -DskipTests
```

- 若只想打后端、跳过前端：`-Dfrontend.skip=true`。

## 功能说明（摘要）

- **系统配置管理 → 邮件配置**：SMTP 与邮件标题前缀，供主动扫描任务通知使用。
- **Git 仓库扫描管理**：仓库、下发任务、下发任务日志；子进程可注入 `ACTIVE_SCAN_*` 与 `WEBHOOK_*` 环境变量（与常见脚本变量名兼容）。
- **技能管理 → 平台技能**：平台侧 `SKILL.md`，扫描前写入工作区覆盖同名仓库技能。

详见各页面内提示与 `schema.sql` 表结构注释。
