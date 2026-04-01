pipeline {
  agent any

  options {
    timestamps()
    ansiColor('xterm')
  }

  environment {
    BACKEND_MODULES = 'auth-service task-service api-gateway'
    FRONTEND_DIR = 'smart-task-frontend'
    CI = 'true'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build And Test') {
      parallel {
        stage('Backend - Spring Boot Services') {
          steps {
            script {
              def modules = env.BACKEND_MODULES.split(' ')
              for (m in modules) {
                dir(m) {
                  if (isUnix()) {
                    sh 'mvn -B clean verify'
                  } else {
                    bat 'mvn -B clean verify'
                  }
                }
              }
            }
          }
          post {
            always {
              junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml'
              archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/*.jar'
            }
          }
        }

        stage('Frontend - Angular') {
          steps {
            dir("${env.FRONTEND_DIR}") {
              script {
                if (isUnix()) {
                  sh 'npm ci'
                  sh 'npm run test -- --watch=false --browsers=ChromeHeadless --no-progress'
                  sh 'npm run build -- --configuration production'
                } else {
                  bat 'npm ci'
                  bat 'npm run test -- --watch=false --browsers=ChromeHeadless --no-progress'
                  bat 'npm run build -- --configuration production'
                }
              }
            }
          }
          post {
            always {
              junit allowEmptyResults: true, testResults: 'smart-task-frontend/**/junit*.xml, smart-task-frontend/**/TESTS-*.xml'
              archiveArtifacts allowEmptyArchive: true, artifacts: 'smart-task-frontend/dist/**'
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
