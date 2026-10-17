
variable "project_name" {
  description = "Name of the Breadbite project"
  type        = string
  default     = "breadbite"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "dev"
}

variable "location" {
  description = "Azure region for resources"
  type        = string
  default     = "uksouth"
}

variable "resource_group_name" {
  description = "Azure resource group name"
  type        = string
  default     = "rg-breadbite-dev"
}

variable "tags" {
  description = "Common tags for Azure resources"
  type        = map(string)

  default = {
    project     = "breadbite"
    environment = "dev"
    managed_by  = "terraform"
  }
}
