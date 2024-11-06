# Provider for Google


provider "google" {
  project     = var.projectid
  region      = var.region
  credentials = file("creds.json")
}



# terraform {
#    # Remote backend solution to store terraform state information
#   backend "gcs" {
#     bucket = "buckename"
#     prefix = "infrastate"
#   }
# }