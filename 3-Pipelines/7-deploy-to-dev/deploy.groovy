// This Jenkinsfile is for Eureka microservice


pipeline {
    agent {
        label 'k8s-slave'
    }
    tools {
        maven 'Maven-3.8.8'
        jdk 'JDK-17'
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
            // This step will take care of building the application
            steps {
                echo "Building the ${env.APPLICATION_NAME} Application"
                //mvn command 
                sh 'mvn clean package -DskipTests=true'
                archiveArtifacts artifacts: 'target/*.jar'
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
            steps {
                echo "Starting Docker build stage"
                sh """
                ls -la
                pwd
                cp ${WORKSPACE}/target/i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING} ./.cicd/
                echo "Listing Files in .cicd folder"
                ls -la ./.cicd/
                echo "**************************** Building Docker Image ****************************"
                docker build --force-rm --no-cache --build-arg JAR_SOURCE=i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING} -t ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT} ./.cicd
                docker images
                echo "**************************** Login to Docke Repo ****************************"
                docker login -u ${DOCKER_CREDS_USR} -p ${DOCKER_CREDS_PSW}
                echo "**************************** Docker Push ****************************"
                docker push ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}
                """
            }
        }
        stage ('Deploy To Dev') {
            steps {
                echo "**************************** Deploying to Dev Environment ****************************"
                withCredentials([usernamePassword(credentialsId: 'maha_docker_vm_creds', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                    // some block
                    // With this block, the slave will be connecting to the docker-server using ssh 
                    // and will execute what all commands i want to go for 
                    
                   // sshpass -p password ssh -o StrictHostKeyChecking=no username@hostip command_to_run
                   // sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} hostname -i"
                   // docker run -d -p hp:cp --name containername image:tagname
                   // docker run -d -p 5761:8761 --name ${env.APPLICATION_NAME}-dev ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}
                  script {
                    // Pull the image on the docker server
                    sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker pull ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
                    try {
                        // Stop the container
                        sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker stop ${env.APPLICATION_NAME}-dev"
                        // Remove the Container
                        sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker rm ${env.APPLICATION_NAME}-dev"
                    } catch(err) {
                        echo "Error Caught: $err"
                    }
                    // Create the container
                    echo "Creating the Container"
                    sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker run -d -p 5761:8761 --name ${env.APPLICATION_NAME}-dev ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
                  }
                  
                }
            }
        }
        stage ('Deploy To Test') {
            steps {
                echo "**************************** Deploying to Test Environment ****************************"
                withCredentials([usernamePassword(credentialsId: 'maha_docker_vm_creds', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                  script {
                    // Pull the image on the docker server
                    sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker pull ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
                    try {
                        // Stop the container
                        sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker stop ${env.APPLICATION_NAME}-test"
                        // Remove the Container
                        sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker rm ${env.APPLICATION_NAME}-test"
                    } catch(err) {
                        echo "Error Caught: $err"
                    }
                    // Create the container
                    echo "Creating the Container"
                    sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker run -d -p 6761:8761 --name ${env.APPLICATION_NAME}-test ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
                  }
                  
                }
            }
        }
        stage ('Deploy To Stage') {
            steps {
                echo "**************************** Deploying to Test Environment ****************************"
                withCredentials([usernamePassword(credentialsId: 'maha_docker_vm_creds', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                  script {
                    // Pull the image on the docker server
                    sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker pull ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
                    try {
                        // Stop the container
                        sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker stop ${env.APPLICATION_NAME}-test"
                        // Remove the Container
                        sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker rm ${env.APPLICATION_NAME}-test"
                    } catch(err) {
                        echo "Error Caught: $err"
                    }
                    // Create the container
                    echo "Creating the Container"
                    sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker run -d -p 6761:8761 --name ${env.APPLICATION_NAME}-test ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
                  }
                  
                }
            }
        }
        stage ('Deploy To Prod') {
            steps {
                echo "**************************** Deploying to Test Environment ****************************"
                withCredentials([usernamePassword(credentialsId: 'maha_docker_vm_creds', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                  script {
                    // Pull the image on the docker server
                    sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker pull ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
                    try {
                        // Stop the container
                        sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker stop ${env.APPLICATION_NAME}-test"
                        // Remove the Container
                        sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker rm ${env.APPLICATION_NAME}-test"
                    } catch(err) {
                        echo "Error Caught: $err"
                    }
                    // Create the container
                    echo "Creating the Container"
                    sh "sshpass -p ${PASSWORD} ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker run -d -p 6761:8761 --name ${env.APPLICATION_NAME}-test ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
                  }
                  
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