# ADR-006: Authentication

## Status

Accepted

## Context

RemindLedger is a fully authenticated application — all content is private to the
logged-in user. Authentication is a cross-cutting concern that affects every layer:

- **Frontend (React SPA):** login form, token storage, authenticated API calls.
- **Backend (Spring Boot):** token validation, user identity extraction, endpoint protection.
- **Database:** User entity shape (external subject ID vs password hash).
- **Infrastructure (Terraform):** provisioning the identity provider.

The decision must be made before the first API endpoint is written because it
determines the Spring Security configuration, the frontend auth flow, and the
User table schema.

## Decision

**AWS Cognito (User Pool) with a custom login UI.**

Cognito acts as the OAuth 2.0 / OpenID Connect identity provider. The React SPA
uses a custom-built login form that authenticates against Cognito via the
standard OAuth 2.0 token endpoint. The Spring Boot API validates Cognito-issued
JWTs using the standard JWKS endpoint — no Cognito-specific code in the backend.

- **Phase 1:** Email/password authentication only, custom login form in React.
- **Phase 2:** Social login (Google, Apple) and MFA added as incremental enhancements.

## Alternatives considered


| Option                                                    | Reason rejected                                                                                                                                                                                                                                                  |
| --------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Self-managed (Spring Security + bcrypt + self-issued JWT) | Full control but requires writing and maintaining security-critical code: password hashing, token signing, email verification, password reset, rate limiting. High effort and risk for a concern that is not the project's core domain.                          |
| Auth0                                                     | Excellent developer experience and documentation. Not selected because it is not AWS-native, cannot be provisioned via the Terraform AWS provider (requires a separate Auth0 Terraform provider), and introduces an external dependency outside the AWS account. |
| Keycloak (self-hosted on ECS)                             | Feature-rich open-source identity provider. Not selected because it requires running and maintaining an additional ECS service with its own database — significant operational overhead for a single-developer project.                                          |
| Cognito Hosted UI                                         | Pre-built login page provided by Cognito. Not selected because it offers limited visual customisation and redirects the user away from the application, breaking the SPA experience. A custom login form provides full UX control.                               |


## Consequences

- Cognito User Pool is provisioned via Terraform (ADR-004). Configuration includes password policy, email verification, and app client settings.
- The React SPA authenticates using a standard OIDC client library (e.g. `oidc-client-ts` or `react-oidc-context`). Tokens (ID token, access token) are stored in memory; refresh tokens use Cognito's secure cookie-based rotation.
- The Spring Boot API uses `spring-boot-starter-oauth2-resource-server` to validate JWTs. Configuration requires only the Cognito issuer URI and JWKS endpoint — no Cognito SDK dependency.
- The User table stores Cognito's `sub` (subject UUID) as the external identity reference. No passwords or credentials are stored in the application database.
- Because Cognito exposes standard OAuth 2.0 / OIDC endpoints, switching to another OIDC provider (Auth0, Keycloak) in the future requires only configuration changes, not code changes.
- Cognito is free for up to 50,000 monthly active users; no cost impact at this scale.
- Social login (Google, Apple) and MFA are deferred to Phase 2. Cognito supports both as additive configuration without application code changes.

