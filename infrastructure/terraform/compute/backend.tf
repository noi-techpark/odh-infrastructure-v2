################################################################################
## This file contains the configuration of Terraform Cloud and its providers.
################################################################################

terraform {
  cloud {
    organization = "noi-digital"

    workspaces {
      name = "opendatahub-ingress-compute-dev"
    }
  }

  required_providers {
    # The configuration of the AWS provider and its required version.
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.55"
    }
  }

  # The required version of Terraform itself.
  required_version = "~> 1.4"
}