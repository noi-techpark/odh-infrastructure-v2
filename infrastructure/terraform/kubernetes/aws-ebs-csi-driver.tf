################################################################################
## This file contains the policy configuration for the EBS CSI addon for EKS.
## https://registry.terraform.io/modules/terraform-aws-modules/eks/aws/19.10.0
################################################################################

locals {
  ebs_csi_service_account_namespace = "kube-system"
  ebs_csi_service_account_name      = "ebs-csi-controller-sa"
}

module "ebs_csi_controller_role" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-assumable-role-with-oidc"
  version = "~> 5.11"

  create_role                   = true
  role_name                     = "${data.aws_eks_cluster.default.id}-ebs-csi-controller"
  provider_url                  = replace(data.aws_eks_cluster.default.identity[0].oidc[0].issuer, "https://", "")
  role_policy_arns              = [aws_iam_policy.ebs_csi_controller.arn]
  oidc_fully_qualified_subjects = ["system:serviceaccount:${local.ebs_csi_service_account_namespace}:${local.ebs_csi_service_account_name}"]
}

resource "aws_iam_policy" "ebs_csi_controller" {
  name_prefix = "ebs-csi-controller"
  description = "EKS ebs-csi-controller policy for cluster ${data.aws_eks_cluster.default.id}"
  policy      = file("${path.module}/aws-ebs-csi-driver-policy.json")
}
