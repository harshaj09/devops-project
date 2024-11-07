// This Jenkinsfile is for Eureka microservice

def buildApp() {
    return {
        echo "Building the ${env.APPLICATION_NAME} Application"
        //mvn command 
        sh 'mvn clean package -DskipTests=true'
        archiveArtifacts artifacts: 'target/*.jar'
    }
}

def dockedBuildandPush() {
    return {
        echo "Starting Docker build stage"
        sh "cp ${WORKSPACE}/target/i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING} ./.cicd/"
        echo "**************************** Building Docker Image ****************************"
        sh "docker build --force-rm --no-cache --build-arg JAR_SOURCE=i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING} -t ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT} ./.cicd"
        echo "**************************** Login to Docke Repo ****************************"
        sh "docker login -u ${DOCKER_CREDS_USR} -p ${DOCKER_CREDS_PSW}"
        echo "**************************** Docker Push ****************************"
        sh "docker push ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
    }
}

def imageValidation() {
    return {
        println ('Pulling the Docker Image')
        try {
            sh "docker pull ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
        }
        catch (Exception e) {
            println ("OOPS!!! Docker Image with this tag not available in the hub, so create the image")
            buildApp().call()
            dockedBuildandPush().call()
        }
    }
}

def dockerDeploy(envDeploy, hostPort, contPort) {
    return {
        // for every environment what will change ??
        // applicationName, hostPort, containerPort, environmentName, containerName 
        echo "**************************** Deploying to ${envDeploy} Environment ****************************"
        withCredentials([usernamePassword(credentialsId: 'maha_docker_vm_creds', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
            script {
                // Pull the image on the docker server
                sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker pull ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
                try {
                    // Stop the container
                    sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker stop ${env.APPLICATION_NAME}-${envDeploy}"
                    // Remove the Container
                    sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker rm ${env.APPLICATION_NAME}-${envDeploy}"
                } 
                catch(err) {
                    echo "Error Caught: $err"
                }
                // Create the container
                echo "Creating the Container"
                sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker run -d -p ${hostPort}:${contPort} --name ${env.APPLICATION_NAME}-$envDeploy ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
            }
        }
    }
}

pipeline {
    agent {
        label 'k8s-slave'
    }
    tools {
        maven 'Maven-3.8.8'
        jdk 'JDK-17'
    }
    parameters {
        choice (name: 'buildOnly', choices: 'no\nyes', description: 'Build the application only')
        choice (name: 'scanOnly', choices: 'no\nyes', description: 'Scan the application only')
        choice (name: 'dockerPush', choices: 'no\nyes', description: 'Build the Image and push to repository')
        choice (name: 'deployToDev', choices: 'no\nyes', description: 'This will deploy the application to Dev environment')
        choice (name: 'deployToTest', choices: 'no\nyes', description: 'This will deploy the application to Test environment')
        choice (name: 'deployToStage', choices: 'no\nyes', description: 'This will deploy the application to Stage environment')
        choice (name: 'deployToProd' choices: 'no\nyes', description: 'This will deploy the application to Prod environment')
    }
    environment {
        APPLICATION_NAME = "eureka"
        // https://www.jenkins.io/doc/pipeline/steps/pipeline-utility-steps/#readmavenpom-read-a-maven-project-file
        POM_VERSION = readMavenPom().getVersion()
        POM_PACKAGING = readMavenPom().getPackaging()
        SONAR_URL = "http://35.196.148.247:9000"
        SONAR_TOKEN = credentials('sonar_creds')
        DOCKER_HUB = "docker.io/i27k8s10"
        DOCKER_CREDS = credentials('docker_creds')
        // DOCKER_APPLICATION_NAME = "i27k8s10"
        // DOCKER_HOST_IP = "1.2.3.4"
    }
    stages {
        stage ('Build') {
            when {
                anyOf {
                    expression {
                        params.buildOnly == 'yes'
                    }
                }
            }
            // This step will take care of building the application
            steps {
                script {
                    buildApp().call()
                }
            }
        }
        // stage ('Unit Tests') {
        //     steps {
        //         echo "Executing Unit tests for ${env.APPLICATION_NAME} Application"
        //         sh 'mvn test'
        //     }
        //     post {
        //         always { //success
        //             junit 'target/surefire-reports/*.xml'
        //             jacoco execPattern: 'target/jacoco.exec'
        //         }
        //     }
        // }
        stage ('Sonar') {
            when {
                anyOf {
                    expression {
                        params.scanOnly == 'yes'
                    }
                }
            }
            steps {
                // Code Quality needs to be implemented 
                echo "Starting Sonar Scans with Quality Gates"
                // before we go to next step, install sonarqube plugin 
                // next goto manage jenkins > configure > sonarqube > give url and token for sonarqube
                withSonarQubeEnv('SonarQube'){ // SonarQube is the name that we configured in manage jenkins > sonarqube 
                    sh """
                    mvn sonar:sonar \
                        -Dsonar.projectKey=i27-eureka \
                        -Dsonar.host.url=${env.SONAR_URL} \
                        -Dsonar.login=${SONAR_TOKEN} 
                    """
                }
                timeout (time: 2, unit: 'MINUTES') { // NANOSECONDS, SECONDS, MINUTES, HOURS, DAYS
                    script {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }
        stage ("Docker Build and Push") {
            // agent {
            //     label 'docker-slave'
            // }
            when {
                allOf {
                    expression {
                        params.buildOnly == 'yes'
                        params.dockerPush == 'yes'
                    }
                }
            }
            steps {
                script {
                    dockedBuildandPush().call()
                }
                
            }
        }
        stage ('Deploy To Dev') {
            when {
                anyOf {
                    expression {
                        params.deployToDev == 'yes'
                    }
                }
            }
            steps {
                script {
                    imageValidation().call()
                    dockerDeploy('dev', '5761', '8761').call()
                    echo "deployed to dev environment successfully"
                }
            }
        }
        stage ('Deploy To Test') {
            when {
                anyOf {
                    expression {
                        params.deployToTest == 'yes'
                    }
                }
            }
            steps {
                script {
                    imageValidation().call()
                    dockerDeploy('test', '6761', '8761').call()
                    echo "deployed to test environment successfully"
                }
            }
        }
        stage ('Deploy To Stage') {
            when {
                anyOf {
                    expression {
                        params.deployToStage == 'yes'
                    }
                }
            }
            steps {
                script {
                    imageValidation().call()
                    dockerDeploy('stage', '7761', '8761').call()
                    echo "deployed to stage environment successfully"
                }
            }
        }
        stage ('Deploy To Prod') {
            when {
                allOf {
                    anyOf {
                        expression {
                            params.deployToProd == 'yes'
                            // any other conditions
                        }
                    }
                    anyOf {
                        branch "release/*"
                        expression {
                            tag pattern: "v\\d{1,2}\\.\\d{1,2}\\.\\d{1,2}", comparator: 'REGEXP'
                            // any other conditions
                        }
                    }
                }
                
            }
            steps {
                timeout (time: 300, unit: 'SECONDS') {
                    input message: "Would you like to deploy ${env.APLLICATION_NAME} application in Prod environment", ok: 'yes', submitter: 'mat' // submitter: 'john, mat'
                }
                script {
                    imageValidation().call()
                    dockerDeploy('prod', '8761', '8761').call()
                    echo "deployed to prod environment successfully"
                }
            }
        }

    }
}

/*
// Deploy to dev flow
1) jenkins should be connecting to the dev server using username and passwrd 
2) The password shoyld be availanle in the credentials

Different environments will have different host ports and same container port(8761)
// for this eureka microservice , i will define the below host ports 

// dev env ====> host port is 5761
// test env ====> host port is 6761
// stage env ====> host port is 7761
// Prod env ======> host port is 8761

*/

// Sonar Scan Command 
// mvn sonar:sonar user/passwrd or token and where my sonar(host url) is and project key

/*
1* Pom.xml ====> Sonar properties 
2* sonar.properties =====> Properties 
3* own method ======> with sonar properties 
*/

        // stage ('Docker Format') {
        //     steps {
        //         //i27-eureka-0.0.1-SNAPSHOT.jar
        //         // install Pipeline Utility Steps plugin before we run this stage

        //         // Current Format
        //         echo "The Current Format is: i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING}"
                                
        //                         // Expected : eureka-buildnumber-branchname.jar
        //         echo "The Custom Format is: ${env.APPLICATION_NAME}-${currentBuild.number}-${BRANCH_NAME}.${env.POM_PACKAGING}"

        //         // Custom Format 
        //     }
        // }
