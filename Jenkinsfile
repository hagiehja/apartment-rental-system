pipeline {
    agent any

    parameters {
        string(name: 'API_BASE_URL', defaultValue: 'http://test.example.com', description: '已启动的测试环境入口，不要填写 Jenkins 地址')
        booleanParam(name: 'RUN_INTERNAL', defaultValue: false, description: '直连微服务内部接口')
        booleanParam(name: 'RUN_MAINTENANCE', defaultValue: false, description: '执行测试数据插入和修复接口')
        booleanParam(name: 'RUN_DESTRUCTIVE', defaultValue: false, description: '执行充值、支付、退款、退租等用例')
        booleanParam(name: 'RUN_E2E', defaultValue: false, description: '执行完整租赁 E2E（自动包含 destructive 权限）')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        API_BASE_URL = "${params.API_BASE_URL}"
        API_TIMEOUT = '10'
        TEST_TENANT_ACCOUNT = credentials('api-test-tenant-account')
        TEST_TENANT_PASSWORD = credentials('api-test-tenant-password')
        TEST_LANDLORD_ACCOUNT = credentials('api-test-landlord-account')
        TEST_LANDLORD_PASSWORD = credentials('api-test-landlord-password')
        TEST_ADMIN_ACCOUNT = credentials('api-test-admin-account')
        TEST_ADMIN_PASSWORD = credentials('api-test-admin-password')
        VENV_DIR = '.venv-api'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Install') {
            steps {
                sh '''
                    python3 -m venv "$VENV_DIR"
                    "$VENV_DIR/bin/python" -m pip install --disable-pip-version-check -r api-tests/requirements.txt
                    rm -rf api-tests/allure-results api-tests/allure-report
                    mkdir -p api-tests/allure-results
                '''
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    "$VENV_DIR/bin/python" -c "import os, requests; u=os.environ['API_BASE_URL'].rstrip('/') + '/gateway/health'; r=requests.get(u, timeout=10); r.raise_for_status(); d=r.json(); assert d.get('service') == 'apartment-gateway' and d.get('status') == 'UP', d; print('gateway health: UP')"
                '''
            }
        }

        stage('Smoke Gate') {
            steps {
                sh '''
                    "$VENV_DIR/bin/python" -m pytest api-tests -m smoke \
                      --junitxml=api-tests/junit-smoke.xml \
                      --alluredir=api-tests/allure-results
                '''
            }
        }

        stage('Regression') {
            steps {
                sh '''
                    "$VENV_DIR/bin/python" -m pytest api-tests \
                      -m "not smoke and not internal and not maintenance and not destructive and not e2e" \
                      --junitxml=api-tests/junit-regression.xml \
                      --alluredir=api-tests/allure-results
                '''
            }
        }

        stage('Internal') {
            when { expression { return params.RUN_INTERNAL } }
            steps {
                sh '''
                    "$VENV_DIR/bin/python" -m pytest api-tests -m internal --run-internal \
                      --junitxml=api-tests/junit-internal.xml \
                      --alluredir=api-tests/allure-results
                '''
            }
        }

        stage('Maintenance') {
            when { expression { return params.RUN_MAINTENANCE } }
            steps {
                sh '''
                    "$VENV_DIR/bin/python" -m pytest api-tests -m maintenance --run-maintenance \
                      --junitxml=api-tests/junit-maintenance.xml \
                      --alluredir=api-tests/allure-results
                '''
            }
        }

        stage('Destructive') {
            when { expression { return params.RUN_DESTRUCTIVE } }
            steps {
                sh '''
                    "$VENV_DIR/bin/python" -m pytest api-tests -m "destructive and not e2e" --run-destructive \
                      --junitxml=api-tests/junit-destructive.xml \
                      --alluredir=api-tests/allure-results
                '''
            }
        }

        stage('E2E') {
            when { expression { return params.RUN_E2E } }
            steps {
                sh '''
                    "$VENV_DIR/bin/python" -m pytest api-tests -m e2e --run-e2e \
                      --junitxml=api-tests/junit-e2e.xml \
                      --alluredir=api-tests/allure-results
                '''
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'api-tests/junit-*.xml'
            allure includeProperties: false, jdk: '', results: [[path: 'api-tests/allure-results']]
            archiveArtifacts allowEmptyArchive: true, artifacts: 'api-tests/junit-*.xml'
        }
    }
}
