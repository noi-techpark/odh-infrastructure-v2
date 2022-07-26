################################################################################
## This file contains the kubernetes provider.
## https://registry.terraform.io/providers/hashicorp/kubernetes/latest/docs
################################################################################

data "aws_eks_cluster" "default" {
  name = "aws-main-eu-01"
}

data "aws_eks_cluster_auth" "default" {
  name = "aws-main-eu-01"
}

# The instance of the kubernetes provider.
provider "kubernetes" {
  host                   = data.aws_eks_cluster.default.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.default.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.default.token
}
