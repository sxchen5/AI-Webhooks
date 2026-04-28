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

## GitLab 配置

在仓库 **Settings → Webhooks** 添加 URL：`https://<你的域名>/api/webhook/gitlab`，Secret Token 与「系统配置」中的 Webhook Token 一致，触发事件勾选 **Push events**。
