pipeline {
    agent any

    environment {
        DOCKERHUB_REPO = "jaypark0205/recommendation-api"
        CANARY_TAG     = "canary-${GIT_COMMIT[0..6]}"
        DEPLOY_SERVER  = "172.21.33.26"
        DEPLOY_USER    = "student"
        DEPLOY_PATH    = "/home/sw_team_4/ci-practice-deploy-server"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh './gradlew clean build -x test'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonar-server') {
                    sh './gradlew sonarqube'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'Dockerhub-recommendation-api',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                        docker build -t ${DOCKERHUB_REPO}:${CANARY_TAG} .
                        docker push ${DOCKERHUB_REPO}:${CANARY_TAG}
                        docker logout
                    """
                }
            }
        }

        stage('Deploy Canary') {
            steps {
                sshagent(['deploy-server']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no \
                            ${DEPLOY_USER}@${DEPLOY_SERVER} \
                            'bash ${DEPLOY_PATH}/deploy.sh ${CANARY_TAG}'
                    """
                }
            }
        }
    }

    post {
        success {
            slackSend(
                channel: '#deploy',
                color: 'good',
                message: "✅ 카나리 배포 성공\n태그: ${CANARY_TAG}\n빌드: ${BUILD_URL}"
            )
        }
        failure {
            slackSend(
                channel: '#deploy',
                color: 'danger',
                message: "❌ 배포 실패: ${env.STAGE_NAME} 단계\n태그: ${CANARY_TAG}\n빌드: ${BUILD_URL}"
            )
        }
    }
}