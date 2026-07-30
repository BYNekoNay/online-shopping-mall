# CI/CD 部署指南

## CI

`.github/workflows/ci.yml` 会在所有分支推送、Pull Request 和手动触发时执行：

- 前端依次执行依赖安装、Vitest 和生产构建；
- 后端使用 JDK 17 执行 Maven `verify`，并保留 JAR 构件 7 天；
- 两端均通过后，构建后端 Docker 镜像并校验 Compose 配置。

## CD

`.github/workflows/deploy.yml` 只会在 `main` 分支推送或手动运行时部署。它在 GitHub Actions 中构建前端静态文件，将源码和产物压缩传输到目标服务器，并由服务器上的 Docker Compose 重新构建、启动服务。

部署工作流只有在仓库变量 `DEPLOY_ENABLED` 被设为 `true` 时才会执行。启用前必须配置下列仓库 Secrets；缺少任意一项会使工作流在发布前失败：

| Secret | 用途 |
| --- | --- |
| `DEPLOY_HOST` | 目标服务器主机名或 IP |
| `DEPLOY_USER` | SSH 登录用户名 |
| `DEPLOY_SSH_KEY` | 仅用于部署的私钥 |
| `DEPLOY_KNOWN_HOSTS` | 目标服务器的 `known_hosts` 条目，用于严格校验主机身份 |
| `DEPLOY_PATH` | 服务器上的发布根目录，例如 `/opt/online-shopping-mall` |

## 服务器准备

1. 安装 Docker Engine 和 Docker Compose 插件，并确保部署用户可运行 `docker compose`。
2. 创建 `DEPLOY_PATH` 及其持久化配置文件 `DEPLOY_PATH/.env`。该文件不由工作流覆盖。
3. 在 `.env` 中至少设置 `MYSQL_ROOT_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET`；生产密码必须替换 Compose 中的开发默认值。
4. 将部署公钥加入目标用户的 `~/.ssh/authorized_keys`，并将服务器主机密钥写入 GitHub Secret `DEPLOY_KNOWN_HOSTS`。
5. 在 GitHub 仓库的 Settings > Environments 中创建 `production`。需要人工审核时，在该环境中设置 required reviewers。
6. 在 GitHub 仓库的 Settings > Variables 中创建 `DEPLOY_ENABLED=true`，仅在服务器与全部 Secrets 配置完成后再启用。

每次部署会解压到 `DEPLOY_PATH/releases/<commit-sha>`，切换 `DEPLOY_PATH/current` 后重建后端与 Nginx，并在 60 秒内校验四个服务状态和 Nginx HTTP 响应。任何失败都会恢复到上一版本并重建后端与 Nginx。MySQL 与 Redis 的 Docker 卷由 Compose 保持，不随发布目录切换而删除。
