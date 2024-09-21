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
                sh "mvn -DskipTests=true clean package"
            }
            post {
                success {
//                    junit '**/target/surefire-reports/TEST-*.xml'
                    archiveArtifacts 'target/*.jar'
                }
            }
        }

        stage('Build Image And Deploy') {
            steps {
                script {
                    // Calculate the Jenkins directory path
                    def jenkinsDir = sh(script: 'realpath --relative-to=/var/jenkins_home $(pwd)', returnStdout: true).trim()
                    println "jenkinsDir=${jenkinsDir}"
                    // Use sshagent to manage the SSH connection and execute the commands
                    sshagent(['jenkins']) {
                        sh """
                            ssh root@192.168.31.114 << 'EOF'
                                source ~/.bashrc
                                base_dir=/etc/config/jenkins/jenkins_home/${jenkinsDir}
                                echo "base_dir=\\$PATH"
                                echo "base_dir=\$base_dir"
                                echo "https_proxy=\$https_proxy"
                                echo "http_proxy=\$http_proxy"
                                systemctl show-environment
                                cd \$base_dir
                                docker compose down
                                docker compose up -d
                        """
                    }
                }
            }

            post {
                success {
                    emailext subject: "${env.JOB_NAME} - Build # ${env.BUILD_NUMBER} - successful!",
                            body: "${env.JOB_NAME} - Build # ${env.BUILD_NUMBER} - ${env.BUILD_STATUS}:\n" +
                                    "\n" +
                                    "Check console output at ${env.BUILD_URL} to view the results.",
                            recipientProviders: [developers(), requestor()]
                }
                failure {
                    emailext subject: "${env.JOB_NAME} - Build # ${env.BUILD_NUMBER} - failed!",
                            body: "${env.JOB_NAME} - Build # ${env.BUILD_NUMBER} - ${env.BUILD_STATUS}:\n" +
                                    "\n" +
                                    "Check console output at ${env.BUILD_URL} to view the results.",
                            recipientProviders: [developers(), requestor()]
                }
            }
        }
    }
}