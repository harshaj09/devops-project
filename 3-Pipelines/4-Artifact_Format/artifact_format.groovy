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

            // Code Quality needs to be implemented 
            steps {
                sh """
                echo "Starting Sonar Scan"
                mvn sonar:sonar \
                    -Dsonar.projectKey=i27-eureka \
                    -Dsonar.host.url=${env.SONAR_URL} \
                    -Dsonar.login=${SONAR_TOKEN} 
                """
            }
        }
        stage ('Docker Format') {
            steps {
                //i27-eureka-0.0.1-SNAPSHOT.jar
                // install Pipeline Utility Steps plugin before we run this stage

                // Current Format
                echo "The Current Format is: i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING}"
                                
                // Expected : eureka-buildnumber-branchname.jar
                echo "The Custom Format is: ${env.APPLICATION_NAME}-${currentBuild.number}-${BRANCH_NAME}.${env.POM_PACKAGING}"

                // Custom Format 
            }
        }

    }
}

// Sonar Scan Command 
// mvn sonar:sonar user/passwrd or token and where my sonar(host url) is and project key

/*
1* Pom.xml ====> Sonar properties 
2* sonar.properties =====> Properties 
3* own method ======> with sonar properties 
*/