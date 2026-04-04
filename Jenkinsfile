pipeline {
  agent any

  options {
    timestamps()
    ansiColor('xterm')
  }

  environment {
    CI = 'true'
    SMART_TASK_IMAGE_REGISTRY = 'ghcr.io/ilhem01'
    SMART_TASK_IMAGE_TAG = "${env.SMART_TASK_IMAGE_TAG ?: 'latest'}"
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
            sh '''
              set +e
              echo "== Compose down (project in workspace) =="
              docker compose down --remove-orphans 2>/dev/null || true

              echo "== Remove any leftover smart-task containers =="
              REMAINING="$(docker ps -aq --filter "name=smart-task" 2>/dev/null)"
              if [ -n "$REMAINING" ]; then
                docker rm -f $REMAINING
              else
                echo "No matching containers."
              fi

              echo "== Remove all containers (free host ports on agent) =="
              ALL_IDS="$(docker ps -aq 2>/dev/null)"
              if [ -n "$ALL_IDS" ]; then
                docker rm -f $ALL_IDS
              else
                echo "No containers left."
              fi
            '''
          } else {
            powershell '''
              $ErrorActionPreference = "Continue"
              Write-Host "== Compose down (project in workspace) =="
              docker compose down --remove-orphans 2>&1 | Out-Null

              Write-Host "== Remove containers matching name smart-task =="
              docker ps -aq --filter "name=smart-task" 2>$null | ForEach-Object {
                $id = ($_ -as [string]).Trim()
                if ($id) { & docker rm -f -- $id 2>&1 | Out-Null }
              }

              Write-Host "== Remove all remaining containers (free host ports) =="
              docker ps -aq 2>$null | ForEach-Object {
                $id = ($_ -as [string]).Trim()
                if ($id) { & docker rm -f -- $id 2>&1 | Out-Null }
              }
            '''
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

    // Jenkins credential id github-ghcr: type "Username with password" — username = GitHub login (not email);
    // password = classic PAT with read:packages + write:packages; add repo if packages are private / need repo access.
    stage('Login to GitHub Container Registry') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'github-ghcr', usernameVariable: 'GHCR_USER', passwordVariable: 'GHCR_TOKEN')]) {
          script {
            if (isUnix()) {
              sh '''
                set -eux
                echo "== docker login ghcr.io =="
                USER="$(printf '%s' "$GHCR_USER" | tr -d '\\r\\n')"
                TOKEN="$(printf '%s' "$GHCR_TOKEN" | tr -d '\\r\\n')"
                printf '%s' "$TOKEN" | docker login ghcr.io -u "$USER" --password-stdin
              '''
            } else {
              bat '''
                @echo off
                echo == docker login ghcr.io (Windows bat) ==
                if not defined GHCR_USER (
                  echo ERROR: GHCR_USER is not set. Check credential github-ghcr / withCredentials binding.
                  exit /b 1
                )
                if not defined GHCR_TOKEN (
                  echo ERROR: GHCR_TOKEN is not set. Check credential github-ghcr / withCredentials binding.
                  exit /b 1
                )
                powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass -Command "$u=$env:GHCR_USER.Trim(); $t=$env:GHCR_TOKEN.Trim(); if(-not $u -or -not $t){Write-Error 'Empty GHCR_USER or GHCR_TOKEN'; exit 1}; $t | docker login ghcr.io -u $u --password-stdin; exit $LASTEXITCODE"
                if errorlevel 1 exit /b 1
              '''
            }
          }
        }
      }
    }

    stage('Build and Push Docker Images to GHCR') {
      steps {
        script {
          if (isUnix()) {
            sh '''
              set -eux
              REG="${SMART_TASK_IMAGE_REGISTRY}"
              TAG="${SMART_TASK_IMAGE_TAG}"
              echo "== docker build + push auth-service =="
              docker build -t "${REG}/smart-task-auth-service:${TAG}" ./auth-service
              docker push "${REG}/smart-task-auth-service:${TAG}"
              echo "== docker build + push task-service =="
              docker build -t "${REG}/smart-task-task-service:${TAG}" ./task-service
              docker push "${REG}/smart-task-task-service:${TAG}"
              echo "== docker build + push api-gateway =="
              docker build -t "${REG}/smart-task-api-gateway:${TAG}" ./api-gateway
              docker push "${REG}/smart-task-api-gateway:${TAG}"
              echo "== docker build + push frontend =="
              VITE_API_URL="${VITE_API_URL:-http://localhost:8080}"
              docker build --build-arg "VITE_API_URL=${VITE_API_URL}" -t "${REG}/smart-task-frontend:${TAG}" ./smart-task-frontend
              docker push "${REG}/smart-task-frontend:${TAG}"
            '''
          } else {
            bat '''
              @echo off
              echo == docker build + push auth-service ==
              docker build -t %SMART_TASK_IMAGE_REGISTRY%/smart-task-auth-service:%SMART_TASK_IMAGE_TAG% ./auth-service
              if errorlevel 1 exit /b 1
              docker push %SMART_TASK_IMAGE_REGISTRY%/smart-task-auth-service:%SMART_TASK_IMAGE_TAG%
              if errorlevel 1 exit /b 1
              echo == docker build + push task-service ==
              docker build -t %SMART_TASK_IMAGE_REGISTRY%/smart-task-task-service:%SMART_TASK_IMAGE_TAG% ./task-service
              if errorlevel 1 exit /b 1
              docker push %SMART_TASK_IMAGE_REGISTRY%/smart-task-task-service:%SMART_TASK_IMAGE_TAG%
              if errorlevel 1 exit /b 1
              echo == docker build + push api-gateway ==
              docker build -t %SMART_TASK_IMAGE_REGISTRY%/smart-task-api-gateway:%SMART_TASK_IMAGE_TAG% ./api-gateway
              if errorlevel 1 exit /b 1
              docker push %SMART_TASK_IMAGE_REGISTRY%/smart-task-api-gateway:%SMART_TASK_IMAGE_TAG%
              if errorlevel 1 exit /b 1
              echo == docker build + push frontend ==
              if not defined VITE_API_URL set VITE_API_URL=http://localhost:8080
              docker build --build-arg VITE_API_URL=%VITE_API_URL% -t %SMART_TASK_IMAGE_REGISTRY%/smart-task-frontend:%SMART_TASK_IMAGE_TAG% ./smart-task-frontend
              if errorlevel 1 exit /b 1
              docker push %SMART_TASK_IMAGE_REGISTRY%/smart-task-frontend:%SMART_TASK_IMAGE_TAG%
              if errorlevel 1 exit /b 1
            '''
          }
        }
      }
    }

    stage('Pull Docker Images from GitHub Container Registry') {
      steps {
        script {
          if (isUnix()) {
            sh '''
              set -eux
              echo "== Pull app images from GHCR =="
              docker compose pull auth-service task-service api-gateway frontend
            '''
          } else {
            bat '''
              @echo off
              echo == Pull app images from GHCR ==
              docker compose pull auth-service task-service api-gateway frontend
            '''
          }
        }
      }
    }

    stage('Deploy') {
      steps {
        script {
          if (isUnix()) {
            sh 'docker compose up -d postgres auth-service task-service api-gateway frontend'
          } else {
            bat 'docker compose up -d postgres auth-service task-service api-gateway frontend'
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
