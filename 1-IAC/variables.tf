variable "projectid" {
  default = "quantum-weft-420714"
}

variable "region" {
  default = "us-central1"
  type = string
}

variable "vpc_name" {
  default = "i27-ecommerce-vpc"
}

variable "cidr" {
  default = "10.1.0.0/16"
}

variable "firewall_name" {
  default = "i27-firewall"
}

variable "ports" {
  default = [80, 8080, 8081, 9000, 22]
}

variable "instances" {
  default = {
    "jenkins-master" = {
      instance_type = "e2-medium"
      zone = "us-central1-a"
    }
    "jenkins-slave" = {
      instance_type = "n1-standard-1"
      zone = "us-central1-b"
    }
    "ansible" = {
      instance_type = "e2-medium"
      zone = "us-central1-a"
    }
    "sonarqube" = {
      instance_type = "e2-medium"
      zone = "us-east1-b"
    }
    "docker-server" = {
      instance_type = "e2-medium"
      zone = "us-east1-b"
    }
  }
}

variable "vm_user" {
  default = "siva" # -var=vm_user=mahers
}
variable "node_count" {
  default = 1
}

