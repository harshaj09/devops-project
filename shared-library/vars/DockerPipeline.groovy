import com.kesava.builds.Docker
def call(Map pipelineParams) {
    Docker docker = new Docker(this)
    pipeline {
        agent {
            label 'k8s-slave'
        }
        tools {
            maven 'Mvn-3.8.8'
            jdk 'JDK-17'
        }
        parameters {
            choice (name: 'BuildOnly', choices: 'no\nyes', description: 'Build the application only')
            choice (name: 'scanOnly', choices: 'no\nyes', description: 'Scan the application only')
            choice (name: 'dockerPush', choices: 'no\nyes', description: 'Build the Image and push tp repository')
            choice (name: 'deployToDev', choices: 'no\nyes', description: 'This will deploy the application to Dev environment')
            choice (name: 'deployToTest', choices: 'no\nyes', description: 'This will deploy the application to Test environment')
            choice (name: 'deployToStage', choices: 'no\nyes', description: 'This will deploy the application to Stage environment')
            choice (name: 'deployToProd' choices: 'no\nyes', description: 'This will deploy the application to Prod environment')
        }
        environment {
            APPLICATION_NAME = "${appName}"
            SONAR_URL = "http://35.196.148.247:9000"
            SONAR_TOKEN = credentials('sonar_creds')
            DOCKER_HUB = "docker.io/i27k8s10"
            DOCKER_CREDS = credentials('docker_creds')
            POM_PACKAGING = readMavenPom().getPackaging()
            POM_VERSION = readMavenPom().getVersion()
            POM_ARTIFACTID = readMavenPom().getArtifactId()
        }
        stages {
            stage ('Addition Method') {
                steps {
                    script {
                        echo "****** Using Addition Method ******"
                        println docker.add(20,35)
                    }
                }
            }
        }
    }
}