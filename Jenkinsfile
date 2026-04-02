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
          publishHTML(target: [
            allowMissing: true,
            alwaysLinkToLastBuild: true,
            keepAll: true,
            reportDir: 'smart-task-frontend/cypress/reports',
            reportFiles: 'index.html',
            reportName: 'Cypress HTML Report'
          ])
          archiveArtifacts allowEmptyArchive: true,
            artifacts: '**/target/*.jar, smart-task-frontend/dist/**'
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
                set -eu
                echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

                docker tag smart-task-app-auth-service:latest "$DOCKER_USER/$IMAGE_PREFIX-auth-service:latest"
                docker tag smart-task-app-task-service:latest "$DOCKER_USER/$IMAGE_PREFIX-task-service:latest"
                docker tag smart-task-app-api-gateway:latest "$DOCKER_USER/$IMAGE_PREFIX-api-gateway:latest"
                docker tag smart-task-app-frontend:latest "$DOCKER_USER/$IMAGE_PREFIX-frontend:latest"

                docker push "$DOCKER_USER/$IMAGE_PREFIX-auth-service:latest"
                docker push "$DOCKER_USER/$IMAGE_PREFIX-task-service:latest"
                docker push "$DOCKER_USER/$IMAGE_PREFIX-api-gateway:latest"
                docker push "$DOCKER_USER/$IMAGE_PREFIX-frontend:latest"

                docker logout
              '''
            } else {
              bat '''
                @echo off
                echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin

                docker tag smart-task-app-auth-service:latest %DOCKER_USER%/%IMAGE_PREFIX%-auth-service:latest
                docker tag smart-task-app-task-service:latest %DOCKER_USER%/%IMAGE_PREFIX%-task-service:latest
                docker tag smart-task-app-api-gateway:latest %DOCKER_USER%/%IMAGE_PREFIX%-api-gateway:latest
                docker tag smart-task-app-frontend:latest %DOCKER_USER%/%IMAGE_PREFIX%-frontend:latest

                docker push %DOCKER_USER%/%IMAGE_PREFIX%-auth-service:latest
                docker push %DOCKER_USER%/%IMAGE_PREFIX%-task-service:latest
                docker push %DOCKER_USER%/%IMAGE_PREFIX%-api-gateway:latest
                docker push %DOCKER_USER%/%IMAGE_PREFIX%-frontend:latest

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
