# Jenkins Daily CI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 WSL Docker 中批量构建并启动自定义 Jenkins，每天检查远程 `main` 分支的 Maven 测试、Maven 打包和前端生产构建。

**Architecture:** 使用固定 Jenkins LTS/JDK 21 基础镜像，并从固定 Temurin JDK 17 镜像复制构建 JDK，再安装固定 Maven、Node.js 和 Jenkins 插件。Compose 只把 Jenkins 暴露到本机 `127.0.0.1:8080`，不挂载 Docker Socket；启动脚本自动创建一个内联 Pipeline 任务，该任务每次构建都从公开 GitHub 仓库拉取远程 `main`，因此本地配置尚未推送时也能工作。

**Tech Stack:** Jenkins 2.568.1 LTS、Java 21、Eclipse Temurin 17.0.19+10、Maven 3.9.16、Node.js 24.18.0 LTS、Docker Compose、Groovy、Python 3。

---

## 文件结构

- Create: `scripts/verify-jenkins-config.py` — 对 Jenkinsfile、Dockerfile、插件、Compose 和引导任务执行静态回归检查。
- Create: `Jenkinsfile` — 声明远程源码检出、每日触发、测试、打包、前端构建和报告发布。
- Create: `deploy/jenkins/Dockerfile` — 构建固定工具链和插件的自定义 Jenkins 镜像。
- Create: `deploy/jenkins/plugins.txt` — 固定四个顶层 Jenkins 插件版本。
- Create: `deploy/jenkins/init.groovy.d/create-pipeline.groovy` — 首次启动时自动创建每日检查任务。
- Create: `deploy/jenkins/compose.yml` — 声明本机端口、Volume、时区、重启策略和健康检查。
- Create: `docs/JENKINS-GUIDE.md` — 提供批量安装、首次登录、运行、排错、停止和卸载步骤。

### Task 1: 建立配置回归检查

**Files:**
- Create: `scripts/verify-jenkins-config.py`
- Test: `scripts/verify-jenkins-config.py`

- [ ] **Step 1: 写入当前必然失败的配置验证器**

```python
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []
checks = 0


def require_file(relative: str) -> str:
    global checks
    checks += 1
    path = ROOT / relative
    if not path.is_file():
        errors.append(f"missing file: {relative}")
        return ""
    return path.read_text(encoding="utf-8")


def require_text(relative: str, text: str, needle: str) -> None:
    global checks
    checks += 1
    if needle not in text:
        errors.append(f"{relative}: missing {needle!r}")


dockerfile = require_file("deploy/jenkins/Dockerfile")
plugins = require_file("deploy/jenkins/plugins.txt")
compose = require_file("deploy/jenkins/compose.yml")
jenkinsfile = require_file("Jenkinsfile")
bootstrap = require_file("deploy/jenkins/init.groovy.d/create-pipeline.groovy")
guide = require_file("docs/JENKINS-GUIDE.md")

for needle in (
    "jenkins/jenkins:2.568.1-jdk21",
    "eclipse-temurin:17.0.19_10-jdk-jammy",
    "MAVEN_VERSION=3.9.16",
    "NODE_VERSION=24.18.0",
    "USER jenkins",
):
    require_text("deploy/jenkins/Dockerfile", dockerfile, needle)

for needle in (
    "workflow-aggregator:608.v67378e9d3db_1",
    "git:5.10.1",
    "junit:1416.vd753e036de5e",
    "ws-cleanup:0.49",
):
    require_text("deploy/jenkins/plugins.txt", plugins, needle)

for needle in (
    "127.0.0.1:8080:8080",
    "jenkins_home:/var/jenkins_home",
    "-Duser.timezone=Asia/Shanghai",
    "restart: unless-stopped",
):
    require_text("deploy/jenkins/compose.yml", compose, needle)

checks += 1
if "/var/run/docker.sock" in compose:
    errors.append("deploy/jenkins/compose.yml: Docker socket must not be mounted")

for needle in (
    "cron('H 3 * * *')",
    "mvn --batch-mode test",
    "mvn --batch-mode package -DskipTests",
    "npm ci",
    "npm run build",
    "https://github.com/hagiehja/apartment-rental-system.git",
):
    require_text("Jenkinsfile", jenkinsfile, needle)

for needle in ("apartment-rental-system-daily", "CpsFlowDefinition", "/usr/share/jenkins/ref/Jenkinsfile"):
    require_text("deploy/jenkins/init.groovy.d/create-pipeline.groovy", bootstrap, needle)

for needle in ("docker compose", "initialAdminPassword", "http://localhost:8080", "立即构建"):
    require_text("docs/JENKINS-GUIDE.md", guide, needle)

if errors:
    print("Jenkins configuration verification failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print(f"Jenkins configuration verification passed: {checks} checks")
```

- [ ] **Step 2: 运行验证器并确认先失败**

Run: `python scripts/verify-jenkins-config.py`

Expected: `FAIL`，至少报告 `missing file: deploy/jenkins/Dockerfile` 和 `missing file: Jenkinsfile`。

- [ ] **Step 3: 单独提交验证器**

```powershell
git add scripts/verify-jenkins-config.py
git commit -m "test: add Jenkins configuration checks"
```

### Task 2: 构建固定版本 Jenkins 镜像

**Files:**
- Create: `deploy/jenkins/plugins.txt`
- Create: `deploy/jenkins/Dockerfile`
- Test: `scripts/verify-jenkins-config.py`

- [ ] **Step 1: 写入固定插件清单**

```text
workflow-aggregator:608.v67378e9d3db_1
git:5.10.1
junit:1416.vd753e036de5e
ws-cleanup:0.49
```

- [ ] **Step 2: 写入自定义镜像定义**

```dockerfile
# syntax=docker/dockerfile:1
FROM eclipse-temurin:17.0.19_10-jdk-jammy AS jdk17
FROM jenkins/jenkins:2.568.1-jdk21

USER root

ARG MAVEN_VERSION=3.9.16
ARG NODE_VERSION=24.18.0

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
       ca-certificates curl git tzdata xz-utils \
    && rm -rf /var/lib/apt/lists/*

COPY --from=jdk17 /opt/java/openjdk /opt/java/jdk17

RUN set -eux; \
    maven_archive="apache-maven-${MAVEN_VERSION}-bin.tar.gz"; \
    curl -fsSLo "/tmp/${maven_archive}" \
      "https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/${maven_archive}"; \
    curl -fsSLo "/tmp/${maven_archive}.sha512" \
      "https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/${maven_archive}.sha512"; \
    echo "$(cat "/tmp/${maven_archive}.sha512")  /tmp/${maven_archive}" | sha512sum -c -; \
    tar -xzf "/tmp/${maven_archive}" -C /opt; \
    ln -s "/opt/apache-maven-${MAVEN_VERSION}" /opt/maven; \
    rm -f "/tmp/${maven_archive}" "/tmp/${maven_archive}.sha512"

RUN set -eux; \
    node_archive="node-v${NODE_VERSION}-linux-x64.tar.xz"; \
    curl -fsSLo "/tmp/${node_archive}" "https://nodejs.org/dist/v${NODE_VERSION}/${node_archive}"; \
    curl -fsSLo /tmp/SHASUMS256.txt "https://nodejs.org/dist/v${NODE_VERSION}/SHASUMS256.txt"; \
    cd /tmp; \
    grep " ${node_archive}$" SHASUMS256.txt | sha256sum -c -; \
    tar -xJf "/tmp/${node_archive}" -C /opt; \
    ln -s "/opt/node-v${NODE_VERSION}-linux-x64" /opt/node; \
    rm -f "/tmp/${node_archive}" /tmp/SHASUMS256.txt

ENV JAVA17_HOME=/opt/java/jdk17 \
    MAVEN_HOME=/opt/maven \
    NODE_HOME=/opt/node \
    PATH=/opt/maven/bin:/opt/node/bin:$PATH

RUN mkdir -p /var/jenkins_home/.m2/repository /var/jenkins_home/.npm \
    && chown -R jenkins:jenkins /var/jenkins_home

COPY --chown=jenkins:jenkins deploy/jenkins/plugins.txt /usr/share/jenkins/ref/plugins.txt
COPY --chown=jenkins:jenkins Jenkinsfile /usr/share/jenkins/ref/Jenkinsfile
COPY --chown=jenkins:jenkins deploy/jenkins/init.groovy.d/ /usr/share/jenkins/ref/init.groovy.d/

USER jenkins

RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
```

- [ ] **Step 3: 运行静态验证并确认只剩 Compose、Jenkinsfile、引导脚本和指南错误**

Run: `python scripts/verify-jenkins-config.py`

Expected: `FAIL`，不再报告 Dockerfile 版本或插件版本错误。

- [ ] **Step 4: 提交镜像文件**

```powershell
git add deploy/jenkins/Dockerfile deploy/jenkins/plugins.txt
git commit -m "build: add reproducible Jenkins image"
```

### Task 3: 创建每日检查流水线与自动任务

**Files:**
- Create: `Jenkinsfile`
- Create: `deploy/jenkins/init.groovy.d/create-pipeline.groovy`
- Test: `scripts/verify-jenkins-config.py`

- [ ] **Step 1: 写入声明式 Pipeline**

```groovy
pipeline {
    agent any

    triggers {
        cron('H 3 * * *')
    }

    options {
        buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '14'))
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
        timeout(time: 45, unit: 'MINUTES')
        timestamps()
    }

    environment {
        JAVA_HOME = '/opt/java/jdk17'
        MAVEN_HOME = '/opt/maven'
        NODE_HOME = '/opt/node'
        MAVEN_REPO = '/var/jenkins_home/.m2/repository'
        NPM_CACHE = '/var/jenkins_home/.npm'
        PATH = "/opt/java/jdk17/bin:/opt/maven/bin:/opt/node/bin:${env.PATH}"
    }

    stages {
        stage('Checkout') {
            steps {
                deleteDir()
                git branch: 'main', url: 'https://github.com/hagiehja/apartment-rental-system.git'
                script {
                    env.CHECKED_COMMIT = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                }
                echo "Checking commit ${env.CHECKED_COMMIT}"
            }
        }

        stage('Toolchain') {
            steps {
                sh '''
                    java -version
                    mvn -version
                    node --version
                    npm --version
                '''
            }
        }

        stage('Backend Tests') {
            steps {
                sh 'mvn --batch-mode -Dmaven.repo.local="$MAVEN_REPO" test'
                sh '''
                    find . -path '*/target/surefire-reports/*.xml' -print -quit | grep -q .
                '''
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Backend Package') {
            steps {
                sh 'mvn --batch-mode -Dmaven.repo.local="$MAVEN_REPO" package -DskipTests'
            }
        }

        stage('Frontend Build') {
            steps {
                dir('frontend') {
                    sh 'npm ci --cache "$NPM_CACHE" --prefer-offline'
                    sh 'npm run build'
                }
            }
        }
    }

    post {
        success {
            echo "Daily CI passed for ${env.CHECKED_COMMIT}"
        }
        failure {
            echo "Daily CI failed for ${env.CHECKED_COMMIT ?: 'unknown commit'}"
        }
        always {
            cleanWs(deleteDirs: true, disableDeferredWipeout: true)
        }
    }
}
```

- [ ] **Step 2: 写入首次启动自动建任务脚本**

```groovy
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition
import org.jenkinsci.plugins.workflow.job.WorkflowJob

def jenkins = Jenkins.get()
def jobName = 'apartment-rental-system-daily'

if (jenkins.getItem(jobName) == null) {
    def pipelineScript = new File('/usr/share/jenkins/ref/Jenkinsfile').getText('UTF-8')
    def job = jenkins.createProject(WorkflowJob, jobName)
    job.setDescription('每天检查 apartment-rental-system 的后端测试、后端打包和前端构建。')
    job.setDefinition(new CpsFlowDefinition(pipelineScript, true))
    job.save()
    println("Created Jenkins pipeline job: ${jobName}")
}
```

- [ ] **Step 3: 运行静态验证并确认流水线相关检查通过**

Run: `python scripts/verify-jenkins-config.py`

Expected: `FAIL`，但不再报告 Jenkinsfile 或引导脚本缺失。

- [ ] **Step 4: 提交流水线**

```powershell
git add Jenkinsfile deploy/jenkins/init.groovy.d/create-pipeline.groovy
git commit -m "ci: add daily Jenkins checks"
```

### Task 4: 添加安全的 Compose 运行配置

**Files:**
- Create: `deploy/jenkins/compose.yml`
- Test: `scripts/verify-jenkins-config.py`

- [ ] **Step 1: 写入 Compose 文件**

```yaml
services:
  jenkins:
    build:
      context: ../..
      dockerfile: deploy/jenkins/Dockerfile
    image: apartment-rental-jenkins:2.568.1
    container_name: apartment-rental-jenkins
    restart: unless-stopped
    environment:
      TZ: Asia/Shanghai
      JAVA_OPTS: -Duser.timezone=Asia/Shanghai
    ports:
      - "127.0.0.1:8080:8080"
    volumes:
      - jenkins_home:/var/jenkins_home
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://localhost:8080/login >/dev/null || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 20
      start_period: 60s

volumes:
  jenkins_home:
    name: apartment-rental-jenkins-home
```

- [ ] **Step 2: 运行静态验证并确认只剩指南错误**

Run: `python scripts/verify-jenkins-config.py`

Expected: `FAIL`，只报告 `docs/JENKINS-GUIDE.md` 缺失或缺少指南关键字。

- [ ] **Step 3: 提交 Compose 配置**

```powershell
git add deploy/jenkins/compose.yml
git commit -m "ops: add Jenkins compose service"
```

### Task 5: 写入中文使用指南并通过静态检查

**Files:**
- Create: `docs/JENKINS-GUIDE.md`
- Test: `scripts/verify-jenkins-config.py`

- [ ] **Step 1: 写入中文指南**

指南必须包含以下可直接执行的命令和说明：

````markdown
# Jenkins 每日 CI 使用指南

Jenkins 是免费开源软件，不需要注册官方账号。首次启动时只需在本机 Jenkins 中创建管理员账号。

## 批量安装

在 WSL 中执行：

```bash
cd /mnt/c/Users/Zz/IdeaProjects/apartment-rental-system
docker compose -f deploy/jenkins/compose.yml up -d --build
docker compose -f deploy/jenkins/compose.yml ps
```

访问 `http://localhost:8080`。取得首次解锁密码：

```bash
docker compose -f deploy/jenkins/compose.yml exec jenkins \
  cat /var/jenkins_home/secrets/initialAdminPassword
```

粘贴密码，按向导创建本地管理员账号。插件已在镜像中预装，无需注册 Jenkins 官方账号。

## 使用

首页会自动出现 `apartment-rental-system-daily`。点击任务后选择“立即构建”即可马上检查；每天凌晨 3 点所在小时也会自动执行。红灯时打开“阶段视图”“测试结果”和“控制台输出”。

## 常用命令

```bash
docker compose -f deploy/jenkins/compose.yml logs -f jenkins
docker compose -f deploy/jenkins/compose.yml restart jenkins
docker compose -f deploy/jenkins/compose.yml stop
docker compose -f deploy/jenkins/compose.yml start
```

停止或删除容器不会删除 Jenkins 数据。只有明确需要清空所有 Jenkins 账号、任务和历史记录时，才执行：

```bash
docker compose -f deploy/jenkins/compose.yml down -v
```

该命令会删除 `apartment-rental-jenkins-home` Volume，数据不可通过普通重启恢复。
````

- [ ] **Step 2: 运行完整静态检查**

Run: `python scripts/verify-jenkins-config.py`

Expected: `Jenkins configuration verification passed: 33 checks`。

- [ ] **Step 3: 检查本次范围并提交指南**

```powershell
git diff --check
git status --short
git add docs/JENKINS-GUIDE.md
git commit -m "docs: add Jenkins usage guide"
```

Expected: 只提交本计划列出的 Jenkins 文件，不暂存用户原有的 `docs/STARTUP-GUIDE.md`、截图和 `experiments/`。

### Task 6: 在 WSL Docker 中批量安装并取得运行证据

**Files:**
- Verify: `deploy/jenkins/compose.yml`
- Verify: `Jenkinsfile`

- [ ] **Step 1: 确认 WSL Docker 可用且为支持的架构**

Run:

```powershell
wsl.exe sh -lc 'docker version; docker compose version; uname -m'
```

Expected: Docker Client/Server 和 Compose 都输出版本；架构为 `x86_64`。若不是 `x86_64`，先把 Dockerfile 的 Node 发行包改为对应架构并补验证，不得继续使用错误二进制。

- [ ] **Step 2: 构建并启动 Jenkins**

Run:

```powershell
wsl.exe sh -lc 'cd /mnt/c/Users/Zz/IdeaProjects/apartment-rental-system && docker compose -f deploy/jenkins/compose.yml up -d --build'
```

Expected: 自定义镜像构建成功，`apartment-rental-jenkins` 容器启动。

- [ ] **Step 3: 等待并验证健康状态**

Run:

```powershell
wsl.exe sh -lc 'cd /mnt/c/Users/Zz/IdeaProjects/apartment-rental-system && docker compose -f deploy/jenkins/compose.yml ps && docker inspect --format "{{.State.Health.Status}}" apartment-rental-jenkins'
```

Expected: 服务状态为 `Up`，健康状态最终为 `healthy`。

- [ ] **Step 4: 验证固定工具版本和非 root 身份**

Run:

```powershell
wsl.exe sh -lc 'docker exec apartment-rental-jenkins sh -lc "id; java -version; /opt/java/jdk17/bin/java -version; mvn -version; node --version; npm --version"'
```

Expected: 用户为 `jenkins`；控制器 Java 为 21；构建 JDK 为 17.0.19；Maven 为 3.9.16；Node 为 24.18.0。

- [ ] **Step 5: 验证页面、自动任务和定时器**

Run:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/login | Select-Object StatusCode
wsl.exe sh -lc 'docker logs apartment-rental-jenkins 2>&1 | grep "Created Jenkins pipeline job: apartment-rental-system-daily"'
```

Expected: HTTP 状态码 `200`，日志包含自动建任务消息。首次解锁后在页面中打开任务，确认触发器显示 `H 3 * * *`。

- [ ] **Step 6: 首次解锁后手动真实运行一次**

在 `http://localhost:8080` 创建本地管理员账号，打开 `apartment-rental-system-daily` 并点击“立即构建”。

Expected: Jenkins 真实拉取 GitHub `main`，依次执行 Toolchain、Backend Tests、Backend Package 和 Frontend Build。若项目测试失败，应保留红灯和真实错误，不修改流水线绕过失败。

- [ ] **Step 7: 记录最终验证并提交必要修正**

```powershell
python scripts/verify-jenkins-config.py
git diff --check
git status --short
```

Expected: 静态检查通过；运行证据与页面状态一致；用户原有未提交文件仍未被暂存或修改。

