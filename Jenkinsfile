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

stage('Build And Test In Docker Compose') {
  steps {
    script {
      if (isUnix()) {
        sh 'docker-compose up -d postgres'
        sh 'docker-compose run --rm ci-backend-tests'
        sh 'docker-compose run --rm ci-frontend-tests'
      } else {
        bat 'docker-compose up -d postgres'
        bat 'docker-compose run --rm ci-backend-tests'
        bat 'docker-compose run --rm ci-frontend-tests'
      }
    }
  }
  post {
    always {
      script {
        if (isUnix()) {
          sh 'docker-compose down -v --remove-orphans || true'
        } else {
          bat 'docker-compose down -v --remove-orphans'
        }
      }
      junit allowEmptyResults: true,
        testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, smart-task-frontend/**/junit*.xml, smart-task-frontend/**/TESTS-*.xml'

      archiveArtifacts allowEmptyArchive: true,
        artifacts: '**/target/*.jar, smart-task-frontend/dist/**'
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
