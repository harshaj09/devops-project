package com.kesava.builds;
class Docker {
    def jenkins
    Docker(jenkins) {
        this.jenkins = jenkins
    }
    def add(firstNumber, secondNumber) {
        return firstNumber+secondNumber
    }
    def build(appName) {
        jenkins.sh """
        echo "Building the artifact for ${appName} application"
        mvn clean package -DskipTests=true
        """
    }
}