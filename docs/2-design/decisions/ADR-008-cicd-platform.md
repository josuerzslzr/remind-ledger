# ADR-008: CI/CD Platform

## Status

Accepted

## Context

RemindLedger is a monorepo (ADR-007) containing three components — backend
(Spring Boot / Maven), frontend (React / Vite), and infrastructure (Terraform).
Each component needs an automated pipeline to build, test, and deploy to AWS.

The CI/CD platform must:

- **Support monorepo path filtering** — run only the relevant pipeline when a
component changes, not all three on every push.
- **Deploy to AWS** — push container images to ECR, deploy ECS tasks, sync
static files to S3, and run `terraform apply`.
- **Authenticate to AWS securely** — especially given that the repository is
public (visible to recruiters and the community).

## Decision

**GitHub Actions** with **OIDC federation** for AWS authentication.

### Workflow structure

One workflow file per component, stored in `.github/workflows/`:


| Workflow       | Trigger path  | What it does                                                                        |
| -------------- | ------------- | ----------------------------------------------------------------------------------- |
| `backend.yml`  | `backend/`**  | Maven build + test, Docker image build, push to ECR, deploy to ECS.                 |
| `frontend.yml` | `frontend/**` | npm install + build, sync `dist/` to S3, invalidate CloudFront cache.               |
| `infra.yml`    | `infra/**`    | `terraform fmt -check`, `terraform plan` on PR, `terraform apply` on merge to main. |


### AWS authentication — OIDC federation

GitHub's OIDC provider is registered in the AWS account as an identity provider.
An IAM role with a trust policy scoped to this repository and branch is assumed
by workflows at runtime — no long-lived AWS credentials are stored.

All AWS-specific values (account ID, role ARN, resource identifiers) are stored
in **GitHub Secrets** and **GitHub Variables**, keeping the workflow files free of
account details. This is essential because the repository is public.

### Security conventions

- Third-party actions are pinned to a **commit SHA**, not a mutable tag, to
prevent supply-chain attacks.
- Secrets are passed via `env:` blocks, never interpolated inline in `run:`
steps, to prevent accidental log exposure.
- PR-triggered workflows from forks do not have access to secrets (GitHub's
default behaviour for public repos).
- The OIDC trust policy restricts role assumption to
`repo:<owner>/remind-ledger:ref:refs/heads/main` for deployment workflows.

## Alternatives considered


| Option                       | Reason rejected                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| AWS CodePipeline + CodeBuild | No native monorepo path filtering — requires custom Lambda glue to inspect changed files and conditionally trigger the right pipeline. Pipeline structure is an AWS resource (Terraform), while build commands live in `buildspec.yml` in the repo — splitting the definition across two places. Debugging requires console-hopping from GitHub to AWS Console to CloudWatch. Stronger AWS auth story (native IAM), but OIDC federation closes that gap. Higher cost from day one (~$3–5/month) vs. GitHub Actions' free tier. |
| GitLab CI                    | Strong CI/CD capabilities, but requires GitLab as the Git host or repo mirroring from GitHub. Adds complexity and splits the developer experience across two platforms.                                                                                                                                                                                                                                                                                                                                                        |
| Jenkins (self-hosted)        | Maximum flexibility but significant operational overhead — hosting, patching, plugin management. Overkill for a single-developer project.                                                                                                                                                                                                                                                                                                                                                                                      |
| CircleCI / Travis CI         | External SaaS; additional vendor dependency outside GitHub. No clear advantage over GitHub Actions for a GitHub-hosted repository. Travis CI's reliability and market position have declined since the 2020 migration.                                                                                                                                                                                                                                                                                                         |


## Consequences

- Workflow files live in `.github/workflows/` and are version-controlled alongside application code. Changes to CI are reviewed in pull requests like any other code change.
- GitHub Actions free tier provides 2,000 minutes/month for private repos (unlimited for public repos). At this project's scale, CI costs are zero.
- OIDC federation setup requires a one-time Terraform configuration: an `aws_iam_openid_connect_provider` resource and an IAM role with a trust policy. This is defined in `infra/`.
- The IAM role assumed by GitHub Actions must follow least privilege — scoped to only the actions each workflow needs (ECR push, ECS deploy, S3 sync, Terraform state access).
- GitHub Secrets (`AWS_ROLE_ARN`, `AWS_ACCOUNT_ID`, resource identifiers) and GitHub Variables (`AWS_REGION`) must be configured manually in the repository settings before the first workflow run. These are not managed by Terraform.
- Deployment strategy for Phase 1 is automatic on merge to `main`. Manual deployment gates (environment protection rules) are deferred to Phase 2.

