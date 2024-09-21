pipeline {
    agent any

    tools {
        // Install the Maven version configured as "M3" and add it to the path.
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
                // Get some code from a GitHub repository
                git 'https://github.com/red352/demo-jenkins.git'

                // Run Maven on a Unix agent.
                sh "mvn -Dmaven.test.failure.ignore=true clean package"

                // To run Maven on a Windows agent, use
                // bat "mvn -Dmaven.test.failure.ignore=true clean package"
            }

            post {
                // If Maven was able to run the tests, even if some of the test
                // failed, record the test results and archive the jar file.
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
                        ssh root@192.168.31.114 << EOF
                            base_dir=/etc/config/jenkins/jenkins_home/\${jenkins_dir} &&
                            echo base_dir=\\$base_dir &&
                            cd \\$base_dir &&
                            docker buildx build -t ${env.IMAGE_NAME}:${env.IMAGE_TAG} . &&
                            docker compose up -d 
                        EOF
                    """

                }
            }
        }
    }
}
