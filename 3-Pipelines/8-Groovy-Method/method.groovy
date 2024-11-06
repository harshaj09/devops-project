// Sample Example for a method 


def printName(name, age) {
    return {
        echo "Welcome Mr ${name}"
        echo "Your age is ${age}"
    }
}


pipeline {
    agent any 
    stages {
        stage ('FirstStage') {
            steps {
                script {
                    printName('John', '45').call()
                }
            }
        }
        stage ('SecondStage') {
            steps {
                script {
                    printName('Siva', '30').call()
                }
            }
        }
    }
}

