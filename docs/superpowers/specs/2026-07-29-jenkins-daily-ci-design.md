# Jenkins 每日 CI 检查设计

## 1. 目标

在本机 WSL 的 Docker 环境中运行一套可重复构建的 Jenkins，每天自动检查 `apartment-rental-system` 的 `main` 分支是否仍能通过后端测试、后端打包和前端生产构建。

本阶段只做持续集成（CI），不启动业务容器、不连接生产环境、不自动部署（CD）。流水线失败时，在 Jenkins 页面保留失败阶段、控制台日志和测试报告，供人工排查。

## 2. 已确认范围

- Jenkins 使用自定义 Docker 镜像运行。
- Jenkins 数据使用具名 Volume 持久化。
- 检查公开 GitHub 仓库 `https://github.com/hagiehja/apartment-rental-system.git` 的 `main` 分支。
- 每天凌晨 3 点所在小时执行一次，使用 Jenkins 哈希分钟 `H 3 * * *` 分散负载。
- 允许从 Jenkins 页面手动立即执行。
- 流水线只读访问源码仓库，不保存 GitHub 密码或令牌。
- 不挂载 Docker Socket，不授予 Jenkins 控制 WSL Docker 主机的权限。

## 3. 固定运行环境

自定义镜像固定以下主要版本，避免上游浮动标签造成构建结果漂移：

| 组件 | 版本 | 用途 |
|---|---:|---|
| Jenkins | 2.568.1 LTS | 流水线控制器 |
| Java | 21 | 运行 Jenkins 控制器 |
| Eclipse Temurin JDK | 17.0.19+10 | 编译和测试 Spring Boot 项目 |
| Apache Maven | 3.9.16 | 后端测试与打包 |
| Node.js | 24.18.0 LTS | 前端依赖安装与构建 |

Jenkins 基础镜像使用精确版本标签。JDK 17 从官方 Eclipse Temurin 镜像复制；Maven 和 Node.js 使用官方发行包，并在镜像构建时校验官方校验和。Dockerfile 最终使用非 root 的 `jenkins` 用户运行。

## 4. 文件与组件

计划新增以下文件：

- `Jenkinsfile`：声明每日触发器、检查阶段、超时、报告和日志保留策略。
- `deploy/jenkins/Dockerfile`：构建固定版本的 Jenkins、JDK、Maven、Node.js 环境。
- `deploy/jenkins/compose.yml`：映射 `8080` 端口，声明持久化 Volume、重启策略和健康检查。
- `deploy/jenkins/plugins.txt`：固定最小插件集合，包括声明式 Pipeline、Git 和 JUnit 支持。
- `docs/JENKINS-GUIDE.md`：中文安装、首次解锁、建任务、手动运行、看日志和停机说明。

不会修改或覆盖当前工作区里的 `.env.app`、业务部署 Compose 文件、数据库数据或已有未提交文件。

## 5. 流水线数据流

1. Jenkins 根据 `H 3 * * *` 定时触发，或者用户在页面点击“立即构建”。
2. Jenkins 从公开 GitHub 仓库检出 `main` 分支，并记录本次提交 SHA。
3. 后端测试阶段执行 `mvn --batch-mode test`。
4. 无论测试成功或失败，都收集各模块 `target/surefire-reports/*.xml`；没有报告时不把“无报告”误判为测试通过。
5. 测试通过后执行 `mvn --batch-mode package -DskipTests`，验证七个 Maven 模块可以完成打包。
6. 前端阶段在 `frontend` 中执行 `npm ci` 和 `npm run build`，确保锁文件依赖可复现并能生成生产包。
7. 全部成功则构建标记为绿色；任一命令非零退出则立即停止后续阶段并标记为红色。

Maven 本地仓库和 npm 缓存放在 Jenkins 持久化目录下，以缩短每日构建时间；工作区在流水线结束后清理，避免旧产物掩盖问题。

## 6. 可靠性与安全边界

- 单次流水线总超时为 45 分钟，避免依赖下载或测试永久卡住。
- Jenkins 只发布 JUnit 报告和必要的构建元数据，不长期归档全部 JAR、`node_modules` 或前端 `dist`，避免磁盘持续膨胀。
- 保留最近 14 次构建或 14 天记录，以先达到的限制为准。
- Jenkins 首页只通过 WSL 映射到本机 `8080`；不主动配置公网暴露。
- 首次启动继续使用 Jenkins 官方解锁流程，由用户创建本地管理员账号；Jenkins 本身免费，无需注册官方云账号。
- 不在仓库中提交管理员密码、初始解锁密码、令牌或其他凭据。
- 不挂载 `/var/run/docker.sock`，因此流水线无法启动、删除或修改其他业务容器。

## 7. 验证与验收

安装完成前必须取得以下运行证据：

1. 自定义镜像成功构建，容器健康且重启后仍能恢复。
2. `http://localhost:8080` 可以打开 Jenkins 页面。
3. 容器中 `java -version` 为 Java 21，构建用 `JAVA_HOME` 指向 JDK 17；Maven、Node.js 版本与设计一致。
4. Jenkins 能读取仓库内 `Jenkinsfile` 并建立 Pipeline 任务。
5. 手动触发一次完整流水线，真实执行 Maven 测试、Maven 打包和前端构建。
6. Jenkins 页面能展示本次提交 SHA、每个阶段状态、控制台日志及实际生成的 JUnit 报告。
7. 检查 Jenkins 任务配置，确认已加载 `H 3 * * *` 定时器。

若当前项目本身存在测试或构建失败，验收结果应如实记录为“Jenkins 安装正常、项目检查失败”，不得把流水线红灯描述为 Jenkins 安装失败，也不得跳过失败命令伪造绿灯。

## 8. 日常使用

- 打开 `http://localhost:8080` 查看首页和最近构建状态。
- 点击流水线名称，再点击“立即构建”执行即时检查。
- 红灯时进入对应构建，依次查看“阶段视图”“测试结果”和“控制台输出”。
- 修改仓库中的 `Jenkinsfile` 并推送后，下次构建会使用新版流程。
- Jenkins 容器停止期间不会执行定时任务；WSL 和 Docker 必须在计划时间保持运行。

