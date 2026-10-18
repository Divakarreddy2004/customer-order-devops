# Customer Order Management – End-to-End DevOps CI/CD Pipeline

An automated CI/CD pipeline for a **Customer Order Management** application using **Git, GitHub, Jenkins, Docker, and AWS**. Every new commit pushed to GitHub triggers Jenkins, which builds the application, runs tests, packages it as a Docker image, pushes the image to AWS ECR, and deploys it to an AWS EC2 instance.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Objectives](#objectives)
3. [Technology Stack](#technology-stack)
4. [Application Overview](#application-overview)
5. [Pipeline Architecture](#pipeline-architecture)
6. [Pipeline Workflow](#pipeline-workflow)
7. [Repository Structure](#repository-structure)
8. [Prerequisites](#prerequisites)
9. [Step 1: AWS Setup](#step-1-aws-setup)
10. [Step 2: Jenkins Server Setup](#step-2-jenkins-server-setup)
11. [Step 3: Dockerfile](#step-3-dockerfile)
12. [Step 4: Jenkinsfile](#step-4-jenkinsfile)
13. [Step 5: Create the Jenkins Pipeline Job](#step-5-create-the-jenkins-pipeline-job)
14. [Step 6: Configure the Automatic Build Trigger](#step-6-configure-the-automatic-build-trigger)
15. [Step 7: Verify the Pipeline](#step-7-verify-the-pipeline)
16. [Troubleshooting](#troubleshooting)
17. [Future Improvements](#future-improvements)

---

## Project Overview

This project demonstrates a complete CI/CD workflow:

- **Git** tracks code changes locally.
- **GitHub** hosts the source code and notifies Jenkins on every push.
- **Jenkins** orchestrates the pipeline (checkout, build, test, Docker build, push, deploy).
- **Docker** packages the application into a portable image.
- **AWS** provides the infrastructure: **ECR** stores the images, **EC2** runs Jenkins and the deployed container.

The goal is to automate software delivery so that a single `git push` takes code from a developer's machine to a running application.

## Objectives

- Automate application builds using Jenkins.
- Integrate GitHub with Jenkins for source code management.
- Trigger builds automatically when new commits are pushed.
- Execute automated tests during the pipeline.
- Containerize the application with Docker.
- Store Docker images in AWS ECR.
- Deploy the container automatically to AWS EC2.
- Identify build failures through Jenkins console logs.

## Technology Stack

| Technology | Purpose |
|------------|---------|
| Git | Version control |
| GitHub | Source code repository and webhook source |
| Jenkins | CI/CD automation server |
| Maven | Build and dependency management |
| Java (JDK 17) | Application development |
| Docker | Containerization |
| AWS EC2 | Hosts Jenkins and the application container |
| AWS ECR | Private Docker image registry |
| AWS IAM | Access control for Jenkins and EC2 |
| Linux (Ubuntu) | Build and runtime environment |

## Application Overview

The Customer Order Management application manages customer orders throughout their lifecycle.

Typical features:

- Customer information management
- Order creation
- Order status tracking
- Order processing
- Order history management

> These features describe the intended application scope. They may require implementation in the application source code.

## Pipeline Architecture

```
Developer (writes code)
        |
        |  git push
        v
GitHub Repository
        |
        |  Webhook / SCM Polling
        v
Jenkins Server (AWS EC2)
        |
        +--> 1. Checkout source code
        +--> 2. Build with Maven
        +--> 3. Run automated tests
        +--> 4. Build Docker image
        +--> 5. Push image to AWS ECR
        |
        v
Deployment Server (AWS EC2)
        |
        +--> Pull image from ECR
        +--> Stop old container
        +--> Run new container
        |
        v
Application live on http://<EC2-PUBLIC-IP>:8080
```

## Pipeline Workflow

1. A developer modifies the application or README file.
2. The developer commits and pushes the changes to GitHub.
3. Jenkins detects the new commit through a GitHub webhook (or SCM polling).
4. Jenkins checks out the repository.
5. Jenkins builds the application with Maven.
6. Jenkins runs the automated tests and publishes the results.
7. Jenkins builds a Docker image tagged with the build number.
8. Jenkins pushes the image to AWS ECR.
9. Jenkins connects to the deployment server, pulls the new image, and restarts the container.
10. Jenkins reports the build result (success or failure).

## Repository Structure

```
customer-order-management/
├── src/
│   ├── main/java/...          # Application source code
│   └── test/java/...          # Unit tests
├── pom.xml                    # Maven build configuration
├── Dockerfile                 # Docker image definition
├── Jenkinsfile                # Pipeline as code
└── README.md                  # Project documentation
```

## Prerequisites

- A GitHub account and a repository containing the application code
- An AWS account
- Basic knowledge of Linux, Git, and Docker
- Local tools: Git, a terminal, and an SSH client

---

## Step 1: AWS Setup

### 1.1 Launch two EC2 instances (Ubuntu 22.04)

| Instance | Purpose | Suggested type |
|----------|---------|----------------|
| `jenkins-server` | Runs Jenkins | t2.medium (t2.micro is too small for Jenkins) |
| `app-server` | Runs the application container | t2.micro |

> For a simple demo, you may use a single instance for both. Separate instances are closer to real-world practice.

### 1.2 Configure Security Groups

**jenkins-server**

| Port | Protocol | Source | Purpose |
|------|----------|--------|---------|
| 22 | TCP | Your IP | SSH |
| 8080 | TCP | Your IP and GitHub webhook IPs | Jenkins UI and webhook |

**app-server**

| Port | Protocol | Source | Purpose |
|------|----------|--------|---------|
| 22 | TCP | Jenkins server IP | Deployment over SSH |
| 8080 | TCP | 0.0.0.0/0 | Application access |

> If Jenkins and the app both use port 8080 on the same machine, map the app to a different host port (for example `-p 8081:8080`).

### 1.3 Create an ECR repository

```bash
aws ecr create-repository \
  --repository-name customer-order-management \
  --region ap-south-1
```

Note the repository URI, for example:
`<AWS_ACCOUNT_ID>.dkr.ecr.ap-south-1.amazonaws.com/customer-order-management`

### 1.4 Create an IAM user (or role) for Jenkins

Attach a policy that allows pushing to ECR, such as `AmazonEC2ContainerRegistryPowerUser`. Generate an access key and keep it safe. You will store it in Jenkins credentials.

For the app server, attach an IAM role with `AmazonEC2ContainerRegistryReadOnly` so it can pull images from ECR.

---

## Step 2: Jenkins Server Setup

SSH into `jenkins-server` and install the required tools.

### 2.1 Install Java and Jenkins

```bash
sudo apt update
sudo apt install -y fontconfig openjdk-17-jre

sudo wget -O /usr/share/keyrings/jenkins-keyring.asc \
  https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] \
  https://pkg.jenkins.io/debian-stable binary/" | \
  sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update
sudo apt install -y jenkins
sudo systemctl enable --now jenkins
```

Get the initial admin password:

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

Open `http://<JENKINS-PUBLIC-IP>:8080`, paste the password, and install the suggested plugins.

### 2.2 Install Git, Maven, Docker, and AWS CLI

```bash
sudo apt install -y git maven docker.io unzip

# AWS CLI v2
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o awscliv2.zip
unzip awscliv2.zip && sudo ./aws/install

# Allow Jenkins to use Docker
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

### 2.3 Install Jenkins plugins

Go to **Manage Jenkins → Plugins → Available plugins** and install:

- Git
- GitHub Integration (GitHub plugin)
- Pipeline
- Docker Pipeline
- Amazon ECR
- AWS Credentials
- SSH Agent

### 2.4 Add credentials

Go to **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**:

| ID | Type | Value |
|----|------|-------|
| `aws-credentials` | AWS Credentials | IAM access key and secret key |
| `app-server-ssh` | SSH Username with private key | `ubuntu` and the `.pem` key of the app server |
| `github-credentials` | Username with password (token) | GitHub username and Personal Access Token (only needed for private repos) |

---

## Step 3: Dockerfile

Create `Dockerfile` in the project root:

```dockerfile
# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Stage 2: Run ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Test it locally:

```bash
docker build -t customer-order-management .
docker run -p 8080:8080 customer-order-management
```

---

## Step 4: Jenkinsfile

Create `Jenkinsfile` in the project root. Replace the placeholder values in the `environment` block.

```groovy
pipeline {
    agent any

    environment {
        AWS_REGION     = 'ap-south-1'
        AWS_ACCOUNT_ID = '<AWS_ACCOUNT_ID>'
        ECR_REPO       = 'customer-order-management'
        ECR_URI        = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}"
        IMAGE_TAG      = "${BUILD_NUMBER}"
        APP_SERVER     = 'ubuntu@<APP_SERVER_PUBLIC_IP>'
        CONTAINER_NAME = 'customer-order-app'
    }

    triggers {
        // Fallback if webhook is not used. Remove if using only a webhook.
        pollSCM('H/5 * * * *')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${ECR_REPO}:${IMAGE_TAG} ."
                sh "docker tag ${ECR_REPO}:${IMAGE_TAG} ${ECR_URI}:${IMAGE_TAG}"
                sh "docker tag ${ECR_REPO}:${IMAGE_TAG} ${ECR_URI}:latest"
            }
        }

        stage('Push to ECR') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding',
                                  credentialsId: 'aws-credentials']]) {
                    sh '''
                        aws ecr get-login-password --region $AWS_REGION | \
                        docker login --username AWS --password-stdin \
                        $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
                    '''
                    sh "docker push ${ECR_URI}:${IMAGE_TAG}"
                    sh "docker push ${ECR_URI}:latest"
                }
            }
        }

        stage('Deploy to EC2') {
            steps {
                sshagent(credentials: ['app-server-ssh']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${APP_SERVER} '
                            aws ecr get-login-password --region ${AWS_REGION} | \
                            docker login --username AWS --password-stdin \
                            ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com &&
                            docker pull ${ECR_URI}:${IMAGE_TAG} &&
                            docker stop ${CONTAINER_NAME} || true &&
                            docker rm ${CONTAINER_NAME} || true &&
                            docker run -d --name ${CONTAINER_NAME} -p 8080:8080 ${ECR_URI}:${IMAGE_TAG}
                        '
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully. Application deployed.'
        }
        failure {
            echo 'Pipeline failed. Check the console output for details.'
        }
        always {
            sh "docker image prune -f"
        }
    }
}
```

> The app server needs Docker and AWS CLI installed, and an IAM role attached that allows pulling from ECR.

Commit and push both files:

```bash
git add Dockerfile Jenkinsfile README.md
git commit -m "Add Dockerfile, Jenkinsfile and README"
git push origin main
```

---

## Step 5: Create the Jenkins Pipeline Job

1. Jenkins Dashboard → **New Item**
2. Name: `customer-order-management` → choose **Pipeline** → **OK**
3. Under **Pipeline**:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: `https://github.com/<your-username>/customer-order-management.git`
   - Credentials: `github-credentials` (if the repo is private)
   - Branch: `*/main`
   - Script Path: `Jenkinsfile`
4. Click **Save**, then **Build Now** once to confirm everything works.

---

## Step 6: Configure the Automatic Build Trigger

Two approaches can be used.

### Option 1: GitHub Webhook (recommended)

**In Jenkins:** Job → **Configure** → **Build Triggers** → tick **GitHub hook trigger for GITScm polling** → **Save**.

**In GitHub:** Repository → **Settings** → **Webhooks** → **Add webhook**

| Field | Value |
|-------|-------|
| Payload URL | `http://<JENKINS-PUBLIC-IP>:8080/github-webhook/` |
| Content type | `application/json` |
| Events | Just the push event |
| Active | Checked |

> The trailing slash in the Payload URL is required. Jenkins must be reachable from the internet (port 8080 open in the AWS Security Group).

### Option 2: Poll SCM

Use this if Jenkins is not reachable from GitHub.

Jenkins Job → **Configure** → **Build Triggers** → **Poll SCM**

```text
* * * * *
```

This checks GitHub every 5 minutes and builds only when there is a new commit. The cron expression must have exactly **5 fields**.

---

## Step 7: Verify the Pipeline

1. Edit any file (for example, add a line to this README).
2. Commit and push:

   ```bash
   git add .
   git commit -m "Test CI/CD trigger"
   git push origin main
   ```
3. Jenkins should start a new build automatically.
4. Open the build → **Console Output** and confirm all stages pass.
5. Verify the image in **AWS Console → ECR → customer-order-management**.
6. Open `http://<APP-SERVER-PUBLIC-IP>:8080` to confirm the application is running.

Useful commands on the app server:

```bash
docker ps                          # check the running container
docker logs customer-order-app     # view application logs
```

---

## Troubleshooting

| Problem | Likely cause | Fix |
|---------|--------------|-----|
| Cron error in Poll SCM | Fewer than 5 fields | Use `H/5 * * * *` |
| Webhook shows red cross in GitHub | Wrong URL or port blocked | Check the URL ends with `/github-webhook/` and port 8080 is open |
| Build does not start after push | Trigger not enabled | Tick **GitHub hook trigger** or configure Poll SCM |
| `permission denied` on `docker.sock` | Jenkins user not in docker group | `sudo usermod -aG docker jenkins` and restart Jenkins |
| `mvn: command not found` | Maven not installed | `sudo apt install maven` |
| ECR login fails | Wrong credentials or region | Check the `aws-credentials` ID, IAM policy, and region |
| Deployment SSH fails | Wrong key or security group | Check the SSH credential and that port 22 allows the Jenkins server IP |
| Jenkins is slow or crashes | Instance too small | Use at least t2.medium |
| Pipeline fails on tests | Failing unit tests | Check the console output and `target/surefire-reports` |

---

## Future Improvements

- Add code quality analysis with SonarQube
- Add image vulnerability scanning (Trivy or ECR scanning)
- Use Docker Compose or Amazon ECS/EKS for orchestration
- Add Slack or email notifications for build results
- Provision infrastructure with Terraform
- Add separate dev, staging, and production environments
- Use HTTPS with a reverse proxy (Nginx) and a domain name
- Store secrets in AWS Secrets Manager

---

## Author

**Your Name**
GitHub: [your-username](https://github.com/your-username)
Testing auto trigger Sun Oct 18 05:52:13 UTC 2026
