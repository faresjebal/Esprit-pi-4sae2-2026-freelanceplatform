# Security setup for the reviewed branch

This branch removes several literal credentials from current service configuration. It does **not** erase them from Git history or from the upstream repository. Revoke and replace the exposed Gmail app passwords and Gemini API key; rotate the JWT signing key across all token issuers and verifiers. Existing tokens signed with the old key must be invalidated. Rotate any MySQL/Grafana passwords if these defaults were used beyond local development.

## Local configuration

Set these environment variables before starting the corresponding services:

- `JWT_SECRET`: one new Base64-encoded HMAC key shared consistently by the JWT issuer and verifiers.
- `GEMINI_API_KEY`: replacement Gemini key for the project and proposal services.
- `PROJECT_MAIL_PASSWORD`, `CONTRACT_MAIL_PASSWORD`, `USER_MAIL_PASSWORD`, `PAYMENT_MAIL_PASSWORD`: replacement mail app passwords for their configured accounts. If a service does not send mail, disable its mail feature rather than adding an unused credential.
- `MYSQL_ROOT_PASSWORD`: required for `docker-compose.yml` and `docker-compose.monitoring.yml`.
- `GRAFANA_ADMIN_PASSWORD`: required for `docker-compose.monitoring.yml`.

Docker Compose now passes these variables to the services that need them. The Kubernetes service manifests expect an uncommitted `app-credentials` Secret in namespace `pidev` with keys `JWT_SECRET`, `GEMINI_API_KEY`, `PROJECT_MAIL_PASSWORD`, `CONTRACT_MAIL_PASSWORD`, `USER_MAIL_PASSWORD`, and `PAYMENT_MAIL_PASSWORD`. Create it through a secret manager or a local, uncommitted file before applying the service manifests. Keep credentials out of command history and CI logs.

The committed `k8s/secret.yaml` was removed. Create `mysql-secret` in the `pidev` namespace from a secret manager or a local, uncommitted secret file before applying the other manifests. It needs `username` and `password` keys; the MySQL root password and service data source password must agree. The `app-credentials` Secret described above is separate. Do not commit the replacement secret.

## Remaining work before deployment

- Contract creation is still permitted without user authentication because the proposal service calls it internally; replace that with authenticated service-to-service communication.
- Review signature, extension, add-on reads, and other controller routes for object-level authorization. This PR limits contract/PDF and order reads, listing and custom-offer actions, and add-on writes, but it is not a complete security audit.
- Jenkins currently skips Maven tests and does not enforce a SonarQube quality gate. Fix and exercise the pipeline before deployment.
- Generated `target/` artifacts and old commits may retain prior configuration values. Coordinate history remediation with the upstream owner after credentials have been revoked.
- Test the frontend and dependent services against changed access rules. Contract listing is now admin-only; buyers/sellers should use their scoped endpoints.

This is a draft change; Java, Docker, Kubernetes and Jenkins were not run in the review environment.
