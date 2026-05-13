pipeline {
    agent any

    environment {
        DOCKER_HUB_CREDENTIALS_ID = credentials('1')
        DOCKER_IMAGE = 'xsky1014/teedy-app'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        GIT_REPOSITORY = 'https://github.com/xsky1020/Teedy.git'
        GIT_BRANCH = '*/master'
    }

    stages {
        stage('Build') {
            steps {
                checkout scmGit(
                    branches: [[name: env.GIT_BRANCH]],
                    extensions: [],
                    userRemoteConfigs: [[url: env.GIT_REPOSITORY]]
                )
                sh 'mvn -B -DskipTests clean package'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}")
                }
            }
        }

        stage('Upload Docker Image') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', env.DOCKER_HUB_CREDENTIALS_ID) {
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push()
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push('latest')
                    }
                }
            }
        }

        stage('Run Container') {
            steps {
                script {
                    sh 'docker stop teedy-container-8081 || true'
                    sh 'docker rm teedy-container-8081 || true'
                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run('--name teedy-container-8081 -d -p 8081:8080')
                    sh 'docker ps --filter "name=teedy-container"'
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/target/**/*.jar', fingerprint: true, allowEmptyArchive: true
            archiveArtifacts artifacts: '**/target/**/*.war', fingerprint: true, allowEmptyArchive: true
        }
    }
}
