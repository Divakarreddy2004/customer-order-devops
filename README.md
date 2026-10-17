# Customer Order Management – DevOps CI/CD Pipeline

## Project Overview

This project demonstrates the implementation of a DevOps CI/CD pipeline
for a Customer Order Management application using Jenkins and GitHub.

The pipeline automates the process of fetching source code, building the
application, running tests, and preparing it for deployment.

The primary objective is to automate the software delivery process and
ensure that every new commit pushed to GitHub can trigger a Jenkins build.

## Project Objectives

- Automate application builds using Jenkins.
- Integrate GitHub with Jenkins for source code management.
- Trigger builds when new commits are pushed.
- Execute automated tests during the pipeline.
- Identify build failures through Jenkins console logs.
- Establish a foundation for continuous delivery and deployment.

## Technology Stack

| Technology | Purpose |
|------------|---------|
| Git | Version control |
| GitHub | Source code repository |
| Jenkins | CI/CD automation |
| Maven | Application build management (if applicable) |
| Java | Application development (if applicable) |
| Linux | Build environment (if applicable) |

## Application Overview

The Customer Order Management application represents a business system
that manages customer orders throughout their lifecycle.

Typical application features include:

- Customer information management
- Customer order creation
- Order status tracking
- Order processing
- Order history management

These features describe the intended application scope; they may require
implementation in the application source code.

## DevOps Pipeline Workflow

The CI/CD workflow follows these stages:

1. A developer modifies the application or README file.
2. The developer commits and pushes changes to GitHub.
3. Jenkins detects the new commit through SCM polling or a GitHub webhook.
4. Jenkins checks out the repository.
5. Jenkins builds the application.
6. Jenkins runs automated tests.
7. Jenkins reports the build result.
8. Successful builds can proceed to deployment when deployment stages
   are configured.

## Pipeline Architecture

Developer
    |
    v
Git Repository (GitHub)
    |
    v
Jenkins CI/CD Pipeline
    |
    v
Source Code Checkout
    |
    v
Build Application
    |
    v
Run Automated Tests
    |
    v
Build Status
    |
    v
Deployment (Optional)

## Jenkins Integration

Jenkins is configured to integrate with the GitHub repository.

### Automatic Build Trigger

Two approaches can be used:

**Option 1: Poll SCM**

Configure the following schedule under:

Jenkins Job → Configure → Build Triggers → Poll SCM

```text
H/5 * * * *
