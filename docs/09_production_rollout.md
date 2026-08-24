# Local Runtime and Production Gaps

## Runnable local stack

```text
recorded fixtures -> mapping/promotion -> Fuseki/TDB2
                  -> reasoning       -> private Kotlin semantic-service
                                     -> React/Vite frontend
```

The exact local commands are maintained in the root `README.md`. The endpoint
binds to loopback, uses approved query and action contracts, and is suitable for
local demonstration and automated tests.

## What the local gates cover

- controlled input paths and connector format
- source-row quarantine for supported validation failures
- RDF parsing, SHACL, provenance, and managed graph URI policy
- rollback behavior for injected graph-write failures
- approved read-only query IDs and typed response shaping
- local action-request validation, idempotency, audit records, and controlled
  lifecycle transitions
- frontend unit tests, lint, and production compilation

## Required before production

| Area | Current repository | Production requirement |
| --- | --- | --- |
| Sources | Recorded CSV simulation | Authenticated, source-specific DCIM/BMS/CMMS/telemetry connectors |
| Identity | Request-provided actor strings | Authentication, authorization, service identity, and policy enforcement |
| Actions | Local audit records | Approved external writeback contracts, if desired |
| Runtime | Loopback service and local Compose | Deployment, secrets, TLS, scaling, backup, recovery, and change control |
| Operations | UI platform read model and logs | Metrics, alerts, tracing, SLOs, incident response, and capacity planning |
| Data | Deterministic fixtures | Domain validation, retention, privacy, migration, and production load testing |
| Resilience | Unit/in-memory failure injection | Environment-level failover, restore drills, and disaster recovery |

No shadow rollout, customer adoption, production monitoring result, or business
benefit has occurred. Kubernetes, orchestration, streaming, and observability
products are options to evaluate after requirements are known, not implemented
capabilities.
