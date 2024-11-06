# This will create a CloudSql instance
# Create a MySql Db in CloudSql 

variable "database_password" {
  type = string
  sensitive = true
}

resource "google_sql_database_instance" "tf_sql_instance" {
  name = "i27-db-instance"
  database_version = "MYSQL_8_0"
  root_password = var.database_password
  deletion_protection = false
  settings {
    tier = "db-f1-micro"
    ip_configuration {
        ipv4_enabled = true
        authorized_networks {
          name = "allow-apps"
          value = "0.0.0.0/0"
        }
    }
  }
}

resource "google_sql_database" "tf_db" {
  name = "learnerCrownClothing"
  instance = google_sql_database_instance.tf_sql_instance.name
}