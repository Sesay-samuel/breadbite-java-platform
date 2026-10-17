
# Breadbite Java Platform
# Microsoft Azure Infrastructure as Code

locals {
  common_tags = merge(
    var.tags,
    {
      project     = var.project_name
      environment = var.environment
    }
  )
}

# Azure Resource Group
resource "azurerm_resource_group" "breadbite" {
  name     = var.resource_group_name
  location = var.location

  tags = local.common_tags
}
