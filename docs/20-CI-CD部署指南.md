# CI/CD 部署指南

## CI

`.github/workflows/ci.yml` 会在所有分支推送、Pull Request 和手动触发时执行：

- 前端执行 `npm ci`、`npm test` 和 `npm run build`；
- 后端使用 JDK 17 执行 `mvn -B verify`，并保留 JAR 构件 7 天；
- 两端通过后构建后端 Docker 镜像，并校验 Compose 配置。

当 CI 在 main 分支成功时，后端镜像会发布到 GitHub Container Registry：
ghcr.io/bynekonay/online-shopping-mall-backend。每次发布都有不可变的
sha-<commit> 标签，同时更新 latest 标签；Pull Request 只验证构建，不会上传镜像。

## CD

`.github/workflows/deploy.yml` 面向测试服务器。只有 `main` 分支的 CI 成功完成后才会自动部署；也可以从 `main` 手动运行工作流。部署工作流检出并发布通过 CI 验证的精确提交，避免把其他提交误发到服务器。

工作流默认不会连接服务器。只有仓库变量 `DEPLOY_ENABLED` 被设置为 `true` 后才会实际发布。启用前，在 GitHub 仓库的 `Settings > Secrets and variables > Actions` 中配置：

| 类型 | 名称 | 用途 |
| --- | --- | --- |
| Secret | `DEPLOY_HOST` | 测试服务器主机名或 IP |
| Secret | `DEPLOY_USER` | SSH 登录用户 |
| Secret | `DEPLOY_SSH_KEY` | 专用于部署的 SSH 私钥 |
| Secret | `DEPLOY_KNOWN_HOSTS` | 测试服务器的 `known_hosts` 条目，用于严格校验主机身份 |
| Secret | `DEPLOY_PATH` | 服务器发布根目录，例如 `/opt/online-shopping-mall` |
| Variable | `DEPLOY_ENABLED` | 设置为 `true` 后允许发布 |

在 `Settings > Environments` 中创建 `test` 环境。需要人工确认时，可在此环境配置 required reviewers。

## 测试服务器准备

1. 安装 Docker Engine 和 Docker Compose 插件，并确保部署用户能够运行 `docker compose`。
2. 创建 `DEPLOY_PATH` 及其持久化配置文件：`DEPLOY_PATH/.env`。可从仓库中的 `.env.example` 创建，真实密码和密钥不能提交回仓库。
3. 将部署公钥加入目标用户的 `~/.ssh/authorized_keys`；把服务器主机密钥写入 GitHub Secret `DEPLOY_KNOWN_HOSTS`。
4. 配置完所有 Secret 后再将 `DEPLOY_ENABLED` 设为 `true`。

每次发布会解压到 DEPLOY_PATH/releases/<commit-sha>，再将 DEPLOY_PATH/current
切换到新版本。部署工作流会拉取与通过 CI 的提交完全对应的 GHCR 后端镜像，Compose
只重建后端和 Nginx，MySQL 与 Redis 数据卷保持不变。工作流会在 60 秒内检查四个服务及
Nginx HTTP 响应；失败时恢复到上一版本并重建后端与 Nginx。
