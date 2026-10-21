# Customer Order Management – Jenkins CI Pipeline

A Jenkins CI pipeline for a **Customer Order Management** Java application. Every new commit pushed to the `main` branch on GitHub triggers Jenkins, which checks out the code, compiles it, runs unit tests, and packages the application as a **WAR artifact**.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Pipeline Architecture](#pipeline-architecture)
4. [Pipeline Stages](#pipeline-stages)
5. [Repository Structure](#repository-structure)
6. [Prerequisites](#prerequisites)
7. [Jenkins Setup](#jenkins-setup)
8. [Pipeline Script](#pipeline-script)
9. [Automatic Build Trigger](#automatic-build-trigger)
10. [Verify the Pipeline](#verify-the-pipeline)
11. [Troubleshooting](#troubleshooting)
12. [Future Improvements](#future-improvements)

---

## Project Overview

- **Git / GitHub** host and track the source code.
- **Jenkins** automates the build, test, and packaging steps.
- **Maven** compiles the code, runs tests, and produces the WAR file.

The goal is to remove manual build work: a single `git push` starts the whole pipeline.

## Technology Stack

| Technology | Purpose |
|------------|---------|
| Git | Version control |
| GitHub | Source code repository |
| Jenkins | CI automation server |
| Maven | Build and dependency management |
| Java (JDK) | Application development |
| Tomcat (planned) | WAR deployment target |

## Pipeline Architecture

```
Developer
    |
    |  git push (main)
    v
GitHub Repository
    |
    |  Poll SCM (Jenkins checks for new commits)
    v
Jenkins
    |
    +--> 1. Code Checkout
    +--> 2. Code Compile
    +--> 3. Unit Test
    +--> 4. Create WAR Artifact
    |
    v
target/*.war
```

## Pipeline Stages

| Stage | Command | Purpose |
|-------|---------|---------|
| Code Checkout | `git branch: 'main', url: '...'` | Pull the latest code from GitHub |
| Code Compile | `mvn clean compile` | Compile the Java source code |
| Unit Test | `mvn test` | Run unit tests |
| Create WAR Artifact | `mvn package` | Build the WAR file in `target/` |

## Repository Structure

```
customer-order-devops/
├── src/
│   ├── main/java/...          # Application source code
│   └── test/java/...          # Unit tests
├── pom.xml                    # Maven build configuration
└── README.md                  # Project documentation
```

## Prerequisites

- A GitHub repository containing the application code
- Jenkins installed and running
- On the Jenkins machine: **Git**, **JDK**, and **Maven** (check with `git --version`, `java -version`, `mvn -v`)
- A Linux/macOS agent (the pipeline uses `sh`; on Windows, use `bat` instead)

## Jenkins Setup

1. Jenkins Dashboard → **New Item**
2. Enter a name and choose **Pipeline** → **OK**
3. Under **Pipeline**, set **Definition** to **Pipeline script** and paste the script below
4. Click **Save**

## Pipeline Script

```groovy
pipeline {
    agent any

    stages {
        stage('Code Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/Divakarreddy2004/customer-order-devops.git'
            }
        }

        stage('Code Compile') {
            steps {
                sh 'mvn clean compile'
            }
        }

        stage('Unit Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('Create WAR Artifact') {
            steps {
                sh 'mvn package'
            }
        }
    }

}
```

> The `post` block keeps the WAR file on the build page so it can be downloaded after the build.

## Automatic Build Trigger

### Option 1: Poll SCM (used in this project)

Jenkins Job → **Configure** → **Build Triggers** → tick **Poll SCM**

```text

* * * * *
```

- Jenkins checks GitHub **every 5 minutes** and builds only when there is a new commit.
- The cron expression must have exactly **5 fields**.
- `* * * * *` checks every minute. It works, but Jenkins warns that it is wasteful, so `H/5 * * * *` is preferred.
- Remember to click **Save** after changing the schedule.

### Option 2: GitHub Webhook (instant builds)

Use this only if Jenkins is reachable from the internet.

**In Jenkins:** Job → **Configure** → **Build Triggers** → tick **GitHub hook trigger for GITScm polling** → **Save**.

**In GitHub:** Repository → **Settings** → **Webhooks** → **Add webhook**

| Field | Value |
|-------|-------|
| Payload URL | `http://<JENKINS-URL>:8080/github-webhook/` |
| Content type | `application/json` |
| Events | Just the push event |

> The trailing slash in the Payload URL is required.

## Verify the Pipeline

1. Edit a file (for example, add a line to this README).
2. Commit and push:

   ```bash
   git add .
   git commit -m "Test CI trigger"
   git push origin main
   ```
3. Wait for the next poll. A new build appears in **Build History** and says **"Started by an SCM change"**.
4. Open the build → **Console Output** and confirm all four stages are green.
5. Download the WAR from the build page under **Build Artifacts**.

You can also click **Build Now** to run the pipeline manually at any time.

## Troubleshooting

| Problem | Likely cause | Fix |
|---------|--------------|-----|
| Warning: "Do you really mean every minute" | Schedule is `* * * * *` | Use `H/5 * * * *` |
| Build does not start after a push | Poll SCM not saved, or wrong branch | Click **Save**, check the commit is on `main`, and open the **Git Polling Log** |
| `mvn: command not found` | Maven not installed on the agent | `sudo apt install maven` or configure Maven in Jenkins tools |
| `sh` fails on Windows | Windows agent | Use `bat 'mvn ...'` instead of `sh` |
| `Couldn't find any revision to build` | Wrong branch name | Confirm the branch name in GitHub and match it in the script |
| Pipeline fails at Unit Test | Failing tests | Check the console output and `target/surefire-reports` |

## Future Improvements

- We need to connect to docker and AWS
- Publish test results with `junit 'target/surefire-reports/*.xml'`
- Deploy the WAR to Tomcat automatically
- Move the pipeline script into a `Jenkinsfile` in the repo (Pipeline script from SCM)
- Containerize the application with Docker and push images to AWS ECR
- Deploy the container to AWS EC2
- Add code quality analysis with SonarQube
- Add Slack or email notifications for build results
- Switch from polling to a GitHub webhook

---

## Change Log

| Date | Change |
|------|--------|
| Oct 18, 2026 | Created the 4-stage Jenkins pipeline (checkout, compile, test, WAR package) |
| Oct 18, 2026 | Enabled Poll SCM for automatic builds on push to `main` |
| Oct 18, 2026 | Updated README and re-tested the automatic trigger (Jenkins build #23) |

---

## Author

**Divakar Reddy**
GitHub: [Divakarreddy2004](https://github.com/Divakarreddy2004)
