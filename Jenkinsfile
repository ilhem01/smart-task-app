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

    stage('Push Docker Images') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          script {
            if (isUnix()) {
              sh '''
                set -eux
                echo "== Docker push stage (Linux) =="
                echo "Docker user: $DOCKER_USER"
                docker --version

                echo "== Login Docker Hub =="
                echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

                echo "== Verify local images exist =="
                docker image inspect smart-task-app-auth-service:latest >/dev/null
                docker image inspect smart-task-app-task-service:latest >/dev/null
                docker image inspect smart-task-app-api-gateway:latest >/dev/null
                docker image inspect smart-task-app-frontend:latest >/dev/null

                AUTH_IMG="$DOCKER_USER/$IMAGE_PREFIX-auth-service:latest"
                TASK_IMG="$DOCKER_USER/$IMAGE_PREFIX-task-service:latest"
                GATEWAY_IMG="$DOCKER_USER/$IMAGE_PREFIX-api-gateway:latest"
                FRONT_IMG="$DOCKER_USER/$IMAGE_PREFIX-frontend:latest"

                echo "== Tag images =="
                docker tag smart-task-app-auth-service:latest "$AUTH_IMG"
                docker tag smart-task-app-task-service:latest "$TASK_IMG"
                docker tag smart-task-app-api-gateway:latest "$GATEWAY_IMG"
                docker tag smart-task-app-frontend:latest "$FRONT_IMG"

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
                echo Docker user: %DOCKER_USER%
                docker --version

                echo == Login Docker Hub ==
                echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin

                echo == Verify local images exist ==
                docker image inspect smart-task-app-auth-service:latest >nul
                docker image inspect smart-task-app-task-service:latest >nul
                docker image inspect smart-task-app-api-gateway:latest >nul
                docker image inspect smart-task-app-frontend:latest >nul

                set AUTH_IMG=%DOCKER_USER%/%IMAGE_PREFIX%-auth-service:latest
                set TASK_IMG=%DOCKER_USER%/%IMAGE_PREFIX%-task-service:latest
                set GATEWAY_IMG=%DOCKER_USER%/%IMAGE_PREFIX%-api-gateway:latest
                set FRONT_IMG=%DOCKER_USER%/%IMAGE_PREFIX%-frontend:latest

                echo == Tag images ==
                docker tag smart-task-app-auth-service:latest %AUTH_IMG%
                docker tag smart-task-app-task-service:latest %TASK_IMG%
                docker tag smart-task-app-api-gateway:latest %GATEWAY_IMG%
                docker tag smart-task-app-frontend:latest %FRONT_IMG%

                echo == Push images ==
                docker push %AUTH_IMG%
                docker push %TASK_IMG%
                docker push %GATEWAY_IMG%
                docker push %FRONT_IMG%

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
