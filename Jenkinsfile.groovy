pipeline {
    agent any

    tools {
        maven "M3"
        jdk "jdk21"
    }

    environment {
        IMAGE_NAME = 'test'
        IMAGE_TAG = 'latest'
    }

    stages {
        stage('Build') {
            steps {
                git 'https://github.com/red352/demo-jenkins.git'
                sh "mvn -Dmaven.test.failure.ignore=true clean package"
            }
            post {
                success {
                    junit '**/target/surefire-reports/TEST-*.xml'
                    archiveArtifacts 'target/*.jar'
                }
            }
        }

        stage('Build Image And Deploy') {
            steps {
                sshagent(['jenkins']) {
                    sh """
                        jenkins_dir=\$(realpath --relative-to=/var/jenkins_home \$(pwd))
                        echo "jenkins_dir=\$jenkins_dir"
                        ssh root@192.168.31.114 << 'EOF'
                            base_dir=/etc/config/jenkins/jenkins_home/\${jenkins_dir}
                            echo "base_dir=\$base_dir"
                            cd \$base_dir
                            docker buildx build -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} .
                            docker compose up -d
                        EOF
                    """
                }
            }
        }
    }
}
