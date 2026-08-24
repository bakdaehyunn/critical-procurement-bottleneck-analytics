# Current Limitations

This repository is a local engineering demonstration. Its useful evidence is
contract correctness, deterministic transformation/reasoning, failure gates,
and explainability—not production or business outcomes.

## Data and domain

- All operational records are deterministic synthetic or recorded fixtures.
- One connector format simulates several source families; it does not connect
  to a real DCIM, BMS, CMMS, incident platform, or telemetry service.
- The ontology, thresholds, statuses, and action vocabulary have not been
  validated by data-center domain experts.
- Source CSVs are retained as files. RDF provenance stores IDs and hashes, not
  full raw payloads or cryptographic source attestations.

## Logic

- Incident current stage is source-provided. Workflow events do not reconstruct
  authoritative runtime state.
- Queue rank, score, and recommended action are optional controlled-fixture
  facts; the current reasoner does not calculate them.
- Dependency and blast-radius findings use a single explicit edge. There is no
  recursive traversal, transitive path reasoning, cycle handling, or topology
  discovery.
- Reasoning rules are deterministic heuristics for the recorded vocabulary, not
  a validated operational safety policy.

## Actions and security

- The service is private/loopback and has no authentication or authorization.
  Actor identifiers are request fields, not authenticated identities.
- Supported actions create managed local audit facts and lifecycle records
  only. They do not change source, canonical, reasoning, operations, production,
  or external systems.
- Promotion and reasoning review controls remain read-only in the UI. Reasoning
  refresh is disabled as an action affordance.

## Runtime and verification

- Local Fuseki, Docker, Gradle, and Vite workflows are documented; no production
  deployment is supplied.
- There is no production secrets management, TLS, monitoring, alerting, backup,
  restore, high availability, disaster recovery, or deployment pipeline.
- Stress generation proves deterministic fixture creation at 600 scenarios and
  more than 10,000 rows; it is not production performance or load testing.
- Automated checks cover Kotlin contracts/services, RDF/SHACL behavior, SPARQL
  parsing, frontend unit behavior, lint, and build. There is no browser E2E
  suite, accessibility certification, production integration test, or required
  coverage threshold.
- The demonstration requires multiple commands. A one-command environment and
  recorded walkthrough are not implemented.

## Claims that require future evidence

Do not claim faster recovery, reduced handoff time, fewer missed validations,
better reliability, graph superiority, production readiness, or operator
adoption until those outcomes are measured against an explicit baseline.
