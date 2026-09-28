# Freelancing platform — contributor branch

Team project with two kinds of work: larger client projects with proposals and contracts, and smaller fixed-scope offers that freelancers publish as service listings (for example, logos or templates). This fork preserves my contributor branch, `microservice_faresjebali_v2`; the upstream repository is owned by the team.

## My contribution

- Worked on the user, contract, and service-listing backend workflows, including shops, orders, and custom offers.
- Built a Jenkins pipeline for the microservices with SonarQube analysis, Docker image publishing, and a local Kubernetes deployment workflow.
- Added Prometheus scrape configuration and Grafana monitoring for the services and MySQL.
- Used Docker Desktop with a single local Kubernetes cluster. This does not imply experience operating a production cluster.

Other microservices and the web frontend are part of the broader team repository. Their presence here does not mean I authored them.

## Repository layout

| Path | Purpose |
| --- | --- |
| `microservice_user/` | User accounts and authentication |
| `microservice_contract/` | Contract and signature workflows |
| `microservice_service/` | Freelancer shops, listings, offers, and orders |
| `Microservice_project/`, `Microservice_proposal/` | Larger project and proposal workflows |
| `gateway/`, `eureka_server/` | Routing and service discovery |
| `k8s/`, `docker-compose*.yml` | Local orchestration and monitoring manifests |
| `Jenkinsfile`, `prometheus.yml`, `grafana/` | CI/CD and metrics configuration |

## Review status

A [draft security review](https://github.com/faresjebal/Esprit-pi-4sae2-2026-freelanceplatform/pull/1) proposes removing literal credentials and restricting several contract, listing, offer, and order routes. It has not been merged into this contributor branch. Read `SECURITY_SETUP.md` on the draft branch for runtime variables, credential rotation, and remaining risks.

The upstream and fork history contain old development credentials. Do not deploy this repository or use real user data without rotating them and testing the services. The Jenkins pipeline in this branch currently skips tests and does not enforce a SonarQube quality gate. No full Java, Docker, or Kubernetes test run was available during this review.
