package com.kesava.builds;
class Docker {
    def jenkins
    Docker(jenkins) {
        this.jenkins = jenkins
    }
    def add(firstNumber, secondNumber) {
        return firstNumber+secondNumber
    }
}