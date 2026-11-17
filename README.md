# Customer Order Management – Jenkins Multibranch Pipeline with Docker

A DevOps project demonstrating automated CI/CD using **Git, GitHub, Jenkins Multibranch Pipeline, Docker, and AWS EC2**.

Whenever code is pushed to a configured GitHub branch, Jenkins automatically discovers the branch, checks out the source code, builds a Docker image, and deploys the application inside a Docker container.

---

## Table of Contents

1. Project Overview
2. Technology Stack
3. Project Architecture
4. Multibranch Pipeline Workflow
5. Pipeline Stages
6. Repository Structure
7. Prerequisites
8. AWS EC2 Setup
9. Jenkins Multibranch Pipeline Configuration
10. Jenkinsfile
11. Branch-Based Docker Deployment
12. Verify the Pipeline
13. Troubleshooting
14. Future Improvements
15. Author

---

## 1. Project Overview

The objective of this project is to automate application deployment using Jenkins Multibranch Pipeline and Docker.

Key features:

- GitHub integration for source code management.
- Automatic branch discovery using Jenkins Multibranch Pipeline.
- Automatic source code checkout using `checkout scm`.
- Docker image creation using a Dockerfile.
- Automated Docker container deployment.
- Separate Docker containers for different branches.
- Deployment on AWS EC2.

## 2. Technology Stack

| Technology | Purpose |
|---|---|
| Git | Version control |
| GitHub | Source code repository |
| Jenkins | CI/CD automation |
| Jenkins Multibranch Pipeline | Automatic branch discovery and builds |
| Docker | Application containerization |
| Dockerfile | Docker image creation |
| AWS EC2 | Cloud infrastructure |
| Ubuntu | Operating system |
| Shell Scripting | Pipeline automation |

## 3. Project Architecture

```text
                 Developer
                     |
                     | git push
                     v
              GitHub Repository
                     |
          +----------+----------+
          |                     |
       main branch          demo branch
          |                     |
          +----------+----------+
                     |
                     v
          Jenkins Multibranch Pipeline
                     |
              Branch Discovery
                     |
                checkout scm
                     |
                     v
             Build Docker Image
                     |
                     v
             Run Docker Container
                     |
             +-------+-------+
             |               |
          main branch     demo branch
             |               |
          Port 8081       Port 8082
             |               |
             v               v
       Docker Container  Docker Container
                     |
                     v
                 AWS EC2
```

## 4. Multibranch Pipeline Workflow

1. Developer pushes code to GitHub.
2. Jenkins scans the configured GitHub repository.
3. Jenkins discovers branches containing a Jenkinsfile.
4. Jenkins creates separate pipeline jobs for discovered branches.
5. Jenkins checks out the appropriate branch using `checkout scm`.
6. Jenkins builds the Docker image.
7. Jenkins runs the Docker container.
8. Each configured branch gets its own container name and host port.

## 5. Pipeline Stages

| Stage | Command | Purpose |
|---|---|---|
| Checkout Code | `checkout scm` | Checks out the current branch |
| Build Docker Image | `docker build` | Creates the Docker image |
| Run Docker Container | `docker run` | Deploys the application |

## 6. Repository Structure

```text
customer-order-devops/
│
├── src/
│   ├── main/
│   └── test/
│
├── docker/
│   └── Dockerfile
│
├── Jenkinsfile
├── pom.xml
└── README.md
```

## 7. Prerequisites

- GitHub repository.
- Jenkins installed on Ubuntu EC2.
- Docker Engine installed.
- Docker permissions configured for the Jenkins user.
- Jenkins GitHub Branch Source plugin.
- AWS EC2 instance with sufficient disk space.
- Inbound security group rules for application ports.

Verify installations:

```bash
git --version
docker --version
java -version
```

## 8. AWS EC2 Setup

Jenkins is hosted on an Ubuntu EC2 instance.

Important configurations:

- Jenkins runs on port 8080.
- Docker Engine is installed on the EC2 instance.
- Jenkins user has permission to execute Docker commands.
- EBS root volume is configured with sufficient storage.
- Application ports 8081 and 8082 are configured in the EC2 Security Group when external access is required.

## 9. Jenkins Multibranch Pipeline Configuration

1. Open Jenkins Dashboard.
2. Select New Item.
3. Enter the pipeline name.
4. Select Multibranch Pipeline.
5. Configure Branch Sources.
6. Select GitHub.
7. Provide the GitHub repository URL.
8. Configure credentials if required.
9. Enable branch discovery.
10. Configure Pull Request discovery if required.
11. Save the configuration.
12. Select Scan Multibranch Pipeline Now.

Jenkins automatically discovers branches containing a Jenkinsfile.

**Important:** Unlike a normal Pipeline, the Git repository URL and branch selection are configured in Branch Sources, not hardcoded in the Jenkinsfile.

## 10. Jenkinsfile

```groovy
pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
    }

    stages {

        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t customer-order-service -f docker/Dockerfile .'
            }
        }

        stage('Run Docker Container') {
            steps {
                script {
                    def branch = env.BRANCH_NAME.replaceAll(
                        /[^a-zA-Z0-9_.-]/, '-'
                    )

                    def port = branch == 'main' ? '8081' : '8082'

                    sh "docker rm -f customer-order-${branch} || true"

                    sh """
                        docker run -d \
                        -p ${port}:8080 \
                        --name customer-order-${branch} \
                        customer-order-service
                    """
                }
            }
        }
    }
}
```

## 11. Branch-Based Docker Deployment

Different branches use different Docker container names and host ports.

| Branch | Container Name | Host Port | Container Port |
|---|---|---|---|
| main | customer-order-main | 8081 | 8080 |
| demo | customer-order-demo | 8082 | 8080 |

The pipeline uses the Jenkins environment variable `BRANCH_NAME` to identify the branch.

This avoids Docker container name conflicts when multiple branches execute on the same Docker host.

### Docker Commands

Build image:

```bash
docker build -t customer-order-service -f docker/Dockerfile .
```

Run main container:

```bash
docker run -d -p 8081:8080 \
--name customer-order-main customer-order-service
```

Run demo container:

```bash
docker run -d -p 8082:8080 \
--name customer-order-demo customer-order-service
```

Check running containers:

```bash
docker ps
```

Check Docker images:

```bash
docker images
```

## 12. Verify the Pipeline

1. Create a new branch in GitHub.
2. Add or update the Jenkinsfile.
3. Commit and push the changes.
4. Run Scan Multibranch Pipeline Now if automatic discovery has not occurred.
5. Open the discovered branch job.
6. Verify all pipeline stages.
7. Check Docker containers on EC2.

```bash
sudo docker ps
```

Access the application using:

```text
http://EC2-PUBLIC-IP:8081
http://EC2-PUBLIC-IP:8082
```

The application must be listening on container port 8080.

## 13. Troubleshooting

| Problem | Possible Cause | Solution |
|---|---|---|
| `checkout scm` failure | Incorrect branch source configuration | Verify GitHub repository configuration |
| `docker: not found` | Docker not installed | Install Docker Engine |
| Docker permission denied | Jenkins user lacks Docker permissions | Add Jenkins user to Docker group |
| Container name conflict | Duplicate container name | Use branch-specific container names |
| Port already in use | Another service occupies the host port | Use a different host port |
| Disk space warning | Insufficient EC2 storage | Increase EBS volume and expand filesystem |
| Branch not discovered | Branch discovery not configured | Check Branch Sources and scan repository |

## 14. Future Improvements

- Implement GitHub Pull Request builds.
- Integrate Docker image tagging based on Git branch and commit ID.
- Push Docker images to AWS ECR.
- Deploy applications using Kubernetes.
- Automate infrastructure using Terraform.
- Configure Ansible for server provisioning.
- Integrate Prometheus and Grafana for monitoring.
- Implement Jenkins email notifications.
- Add SonarQube for code quality analysis.

## 15. Author

**Divakar Reddy**

GitHub: https://github.com/Divakarreddy2004

LinkedIn: https://www.linkedin.com/in/divakarreddy2004

---

## Project Learning Outcomes

- Git and GitHub branch management.
- Jenkins Multibranch Pipeline configuration.
- SCM-based source code checkout.
- Docker image creation and container management.
- Branch-specific application deployment.
- AWS EC2 administration and disk management.
- CI/CD troubleshooting and automation.
