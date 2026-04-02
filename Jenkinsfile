pipeline {
  agent any

  options {
    timestamps()
    ansiColor('xterm')
  }

  environment {
    CI = 'true'
    IMAGE_PREFIX = 'smart-task'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Clean Docker') {
      steps {
        script {
          if (isUnix()) {
            sh 'docker compose down --remove-orphans || true'
          } else {
            bat 'docker compose down --remove-orphans'
          }
        }
      }
    }

    stage('Build And Test In Docker Compose') {
      steps {
        script {
          if (isUnix()) {
            sh 'docker compose up -d postgres'
            sh 'docker compose run --rm -T ci-backend-tests'
            timeout(time: 15, unit: 'MINUTES') {
              sh 'docker compose run --rm -T ci-frontend-tests'
            }
          } else {
            bat 'docker compose up -d postgres'
            bat 'docker compose run --rm -T ci-backend-tests'
            timeout(time: 15, unit: 'MINUTES') {
              bat 'docker compose run --rm -T ci-frontend-tests'
            }
          }
        }
      }
      post {
        always {
          script {
            if (isUnix()) {
              sh 'docker compose down --remove-orphans || true'
            } else {
              bat 'docker compose down --remove-orphans'
            }
          }
          junit allowEmptyResults: true,
            testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, smart-task-frontend/**/junit*.xml, smart-task-frontend/**/TESTS-*.xml, smart-task-frontend/cypress/results/*.xml'
          script {
            if (fileExists('smart-task-frontend/cypress/reports/index.html')) {
              publishHTML(target: [
                allowMissing: true,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'smart-task-frontend/cypress/reports',
                reportFiles: 'index.html',
                reportName: 'Cypress HTML Report'
              ])
            } else {
              echo 'Cypress HTML report not found; skipping publishHTML.'
            }
          }
          archiveArtifacts allowEmptyArchive: true,
            artifacts: '**/target/*.jar, **/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, smart-task-frontend/cypress/results/*.xml, smart-task-frontend/cypress/reports/**, smart-task-frontend/cypress/screenshots/**, smart-task-frontend/cypress/videos/**, smart-task-frontend/dist/**'
        }
      }
    }

    stage('Build Docker Images') {
      steps {
        script {
          if (isUnix()) {
            sh 'docker compose build auth-service task-service api-gateway frontend'
          } else {
            bat 'docker compose build auth-service task-service api-gateway frontend'
          }
        }
      }
    }

    stage('Docker Hub Login') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          script {
            if (isUnix()) {
              sh '''
                set -eux
                echo "== Docker login stage (Linux) =="
                echo "Docker user: $DOCKER_USER"
                docker --version

                echo "== Login Docker Hub =="
                echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
              '''
            } else {
              bat '''
                @echo off
                echo == Docker login stage (Windows) ==
                echo Docker user: %DOCKER_USER%
                docker --version

                echo == Login Docker Hub ==
                echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin
                if errorlevel 1 exit /b 1
              '''
            }
          }
        }
      }
    }

    stage('Push Docker Images') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          script {
            if (isUnix()) {
              sh '''
                set -eux
                echo "== Docker push stage (Linux) =="

                echo "== Verify local images exist =="
                AUTH_SRC="$(docker compose images -q auth-service)"
                TASK_SRC="$(docker compose images -q task-service)"
                GATEWAY_SRC="$(docker compose images -q api-gateway)"
                FRONT_SRC="$(docker compose images -q frontend)"
                [ -n "$AUTH_SRC" ] && [ -n "$TASK_SRC" ] && [ -n "$GATEWAY_SRC" ] && [ -n "$FRONT_SRC" ]

                AUTH_IMG="$DOCKER_USER/$IMAGE_PREFIX-auth-service:latest"
                TASK_IMG="$DOCKER_USER/$IMAGE_PREFIX-task-service:latest"
                GATEWAY_IMG="$DOCKER_USER/$IMAGE_PREFIX-api-gateway:latest"
                FRONT_IMG="$DOCKER_USER/$IMAGE_PREFIX-frontend:latest"

                echo "== Tag images =="
                docker tag "$AUTH_SRC" "$AUTH_IMG"
                docker tag "$TASK_SRC" "$TASK_IMG"
                docker tag "$GATEWAY_SRC" "$GATEWAY_IMG"
                docker tag "$FRONT_SRC" "$FRONT_IMG"

                echo "== Push images =="
                docker push "$AUTH_IMG"
                docker push "$TASK_IMG"
                docker push "$GATEWAY_IMG"
                docker push "$FRONT_IMG"

                echo "== Logout Docker Hub =="
                docker logout
              '''
            } else {
              bat '''
                @echo off
                echo == Docker push stage (Windows) ==

                echo == Verify local images exist ==
                for /f %%i in ('docker compose images -q auth-service') do set AUTH_SRC=%%i
                for /f %%i in ('docker compose images -q task-service') do set TASK_SRC=%%i
                for /f %%i in ('docker compose images -q api-gateway') do set GATEWAY_SRC=%%i
                for /f %%i in ('docker compose images -q frontend') do set FRONT_SRC=%%i
                if "%AUTH_SRC%"=="" exit /b 1
                if "%TASK_SRC%"=="" exit /b 1
                if "%GATEWAY_SRC%"=="" exit /b 1
                if "%FRONT_SRC%"=="" exit /b 1

                set AUTH_IMG=%DOCKER_USER%/%IMAGE_PREFIX%-auth-service:latest
                set TASK_IMG=%DOCKER_USER%/%IMAGE_PREFIX%-task-service:latest
                set GATEWAY_IMG=%DOCKER_USER%/%IMAGE_PREFIX%-api-gateway:latest
                set FRONT_IMG=%DOCKER_USER%/%IMAGE_PREFIX%-frontend:latest

                echo == Tag images ==
                docker tag %AUTH_SRC% %AUTH_IMG%
                if errorlevel 1 exit /b 1
                docker tag %TASK_SRC% %TASK_IMG%
                if errorlevel 1 exit /b 1
                docker tag %GATEWAY_SRC% %GATEWAY_IMG%
                if errorlevel 1 exit /b 1
                docker tag %FRONT_SRC% %FRONT_IMG%
                if errorlevel 1 exit /b 1

                echo == Push images ==
                docker push %AUTH_IMG%
                if errorlevel 1 exit /b 1
                docker push %TASK_IMG%
                if errorlevel 1 exit /b 1
                docker push %GATEWAY_IMG%
                if errorlevel 1 exit /b 1
                docker push %FRONT_IMG%
                if errorlevel 1 exit /b 1

                echo == Logout Docker Hub ==
                docker logout
              '''
            }
          }
        }
      }
    }
  }

  post {
    always {
      echo "Pipeline finished for ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    }
  }
}
