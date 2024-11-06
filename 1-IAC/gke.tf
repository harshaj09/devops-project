# Write the terraform script for GKE 
resource "google_container_cluster" "my_cluster" {
  name = "cart-cluster"
  location = "us-west1-a"
  initial_node_count = var.node_count
  node_config {
    machine_type = "e2-medium"
    disk_size_gb = "30"
  }
}

