# 代码扫描平台

Spring Boot 3 + Vue3 + Element Plus + MySQL。支持 **主动 Git 扫描**（多仓库、定时/手动、`git clone` + agent/技能）、**平台技能**、**邮件配置**；扫描日志写入 `active_scan_log`。

## 目录

- `scan-platform-backend`：后端（Maven）
- `scan-platform-frontend`：前端（Vite）

## 数据库

1. 创建库：`CREATE DATABASE scan_platform DEFAULT CHARACTER SET utf8mb4;`
2. 修改 `scan-platform-backend/src/main/resources/application.yml` 中的数据源账号密码（或通过环境变量覆盖）。
3. 首次启动会自动执行 `schema.sql` 与 `data.sql`（建表 + 默认管理员）。

**从含 GitLab WebHook / `project_info` / `scan_task_log` 的旧库升级**：在备份后手工执行 `scan-platform-backend/src/main/resources/migration-remove-webhook.sql`（删除旧表与 `sys_config` 中 WebHook 相关列）。新装库直接使用当前 `schema.sql` 即可。

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
