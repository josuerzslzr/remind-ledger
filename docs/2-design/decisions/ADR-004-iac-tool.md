# ADR-004: Infrastructure as Code Tool

## Status

Accepted

## Context

RemindLedger is cloud-native and AWS-hosted. All infrastructure (VPC, ECS, RDS, ALB,
SQS, IAM, etc.) must be provisioned and managed as code. The IaC tool must be
widely adopted, well-documented for AWS, and support a declarative workflow with
reliable state management.

## Decision

**Terraform (HCL).**

Remote state stored in S3 with DynamoDB locking — configured from day one, not
as an afterthought.

## Alternatives considered


| Option                       | Reason rejected                                                                                                                                                                                                                      |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| AWS CDK (TypeScript or Java) | Strong choice for AWS-only projects; uses real programming languages (TypeScript, Java). Not selected because Terraform is cloud-agnostic, more widely adopted across the industry, and HCL skills transfer to non-AWS environments. |
| AWS CloudFormation           | Verbose YAML/JSON; CDK compiles to CloudFormation under the hood. No reason to write it directly.                                                                                                                                    |
| AWS SAM                      | Specific to serverless/Lambda-heavy architectures; not the primary deployment model.                                                                                                                                                 |
| Pulumi                       | Strong alternative; growing adoption. Deferred — Terraform is the more widely recognised IaC skill in job postings and covers the same use cases.                                                                                    |


## Consequences

- HCL is a separate language from Java/TypeScript; context switching during development is expected.
- HashiCorp changed Terraform's licence to BSL in 2023. For a personal project this has no practical impact. OpenTofu (Linux Foundation fork) is HCL-compatible — Terraform skills transfer 1:1 if a migration is needed.
- Remote state setup (S3 bucket + DynamoDB table) must be bootstrapped manually or via a separate Terraform script before the main configuration can be applied.
- Terraform state file must never be committed to git.

