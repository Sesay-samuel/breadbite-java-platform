
# Breadbite Azure Infrastructure Outputs

output "resource_group_name" {
  description = "Name of the Breadbite Azure Resource Group"
  value       = azurerm_resource_group.breadbite.name
}

output "resource_group_location" {
  description = "Azure region of the Breadbite Resource Group"
  value       = azurerm_resource_group.breadbite.location
}

output "resource_group_id" {
  description = "Azure Resource Group resource ID"
  value       = azurerm_resource_group.breadbite.id
}
