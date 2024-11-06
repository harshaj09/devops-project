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
            //sqa_a25af99d06b87a263ccf7aed9033cd9d80b97b36
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
    }
}

// Sonar Scan Command 
// mvn sonar:sonar user/passwrd or token and where my sonar(host url) is and project key

/*
1* Pom.xml ====> Sonar properties 
2* sonar.properties =====> Properties 
3* own method ======> with sonar properties 
*/