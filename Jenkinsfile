pipeline {
    agent any

    stages {

        stage('Checkout Code') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/Divakarreddy2004/customer-order-devops.git'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t customer-order-service -f docker/Dockerfile .'
            }
        } 

        stage('Run Docker Container') {
            steps {
                sh 'docker rm -f customer-order || true'
                sh 'docker run -d -p 8080:8080 --name customer-order customer-order-service'
            }
        }
    }
}
