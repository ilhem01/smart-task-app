pipeline {
  agent any

  options {
    timestamps()
    ansiColor('xterm')
  }

  environment {
    CI = 'true'
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

    stage('Deploy') {
      steps {
        script {
          if (isUnix()) {
            sh 'docker compose up -d --build postgres auth-service task-service api-gateway frontend'
          } else {
            bat 'docker compose up -d --build postgres auth-service task-service api-gateway frontend'
          }
        }
      }
    }

    stage('Debug Docker') {
      steps {
        bat 'whoami'
        bat 'docker context ls'
        bat 'docker info'
      }
    }
  }

  post {
    always {
      echo "Pipeline finished for ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    }
  }
}
