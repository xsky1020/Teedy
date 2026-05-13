pipeline {
    agent any
    stages {
        stage('Clean') {
            steps {
                sh 'mvn clean'
            }
        }
        stage('Compile') {
            steps {
                sh 'mvn compile'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test -Dmaven.test.failure.ignore=true'
                sh 'ls -1 docs-*/target/surefire-reports/TEST-*.xml 2>/dev/null || true'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/TEST-*.xml', allowEmptyResults: true, skipPublishingChecks: true
                }
            }
        }
        stage('PMD') {
            steps {
                sh 'mvn pmd:pmd'
            }
        }
        stage('JaCoCo') {
            steps {
                sh 'mvn jacoco:report'
            }
        }
        
        stage('Site') {
            steps {
                sh 'mvn site site:stage'
                sh '''
                    mkdir -p target/staging/docs-core target/staging/docs-web-common target/staging/docs-web
                    cp -R docs-core/target/site/. target/staging/docs-core/ 2>/dev/null || true
                    cp -R docs-web-common/target/site/. target/staging/docs-web-common/ 2>/dev/null || true
                    cp -R docs-web/target/site/. target/staging/docs-web/ 2>/dev/null || true
                '''
          }
        }
        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: '**/target/site/**', fingerprint: true, allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/staging/**', fingerprint: true, allowEmptyArchive: true
            archiveArtifacts artifacts: '**/target/**/*.jar', fingerprint: true
            archiveArtifacts artifacts: '**/target/**/*.war', fingerprint: true
        }
    }
}
