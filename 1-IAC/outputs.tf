# # Display the VM's public and private ip


output "instance_ips" {
  value = {
    # for i 
    for instance in google_compute_instance.tf-vm-instances :
    instance.name => {
        private_ip = instance.network_interface[0].network_ip
        public_ip = instance.network_interface[0].access_config[0].nat_ip
    }
  }
}



# output "jenkins_master_private_ip" {
#   value = google_compute_instance.tf-vm-instances["jenkins-master"].network_interface[0].network_ip
# }


# output "jenkins_master_public_ip" {
#   value = google_compute_instance.tf-vm-instances["jenkins-master"].network_interface[0].access_config[0].nat_ip
# }

# output "jenkins_slave_private_ip" {
#   value = google_compute_instance.tf-vm-instances["jenkins-slave"].network_interface[0].network_ip
# }


# output "jenkins_slave_public_ip" {
#   value = google_compute_instance.tf-vm-instances["jenkins-slave"].network_interface[0].access_config[0].nat_ip
# }

# output "ansible_private_ip" {
#   value = google_compute_instance.tf-vm-instances["ansible"].network_interface[0].network_ip
# }


# output "ansible_public_ip" {
#   value = google_compute_instance.tf-vm-instances["ansible"].network_interface[0].access_config[0].nat_ip
# }