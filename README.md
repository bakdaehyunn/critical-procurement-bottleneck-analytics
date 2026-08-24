# AI Data Center Infrastructure Semantic Operations Platform

A local, ontology-native portfolio implementation for explaining AI data-center
recovery cases from deterministic recorded source simulations.

The current implementation answers a bounded question:

> Given recorded incident, workflow, work-order, validation, telemetry, impact,
> and direct dependency records, what blocker or evidence conflict can the
> operator inspect, and which local audited action is allowed?

![Semantic operations workbench](docs/assets/dashboard-preview.png)

## What is implemented

```text
recorded CSV simulation
  -> Kotlin source DTOs
  -> source, canonical, and provenance RDF models
  -> SHACL/provenance promotion gates
  -> Fuseki/TDB2 named graphs
  -> deterministic one-edge reasoning findings
  -> approved SPARQL query IDs
  -> private loopback Kotlin service
  -> React recovery workbench
```

- A seeded generator and one recorded-connector CSV contract produce local,
  deterministic source records.
- The mapper creates canonical infrastructure, evidence, workflow, impact, and
  topology facts. Incident current stage is copied from the incident source
  row; workflow events are evidence and timeline records, not a reconstructed
  runtime state machine.
- RDF provenance records source-system identity, source-record IDs, payload
  hashes, import activity, and derivation links. The original CSV files remain
  the recorded source artifacts; their complete payloads are not copied into
  RDF.
- SHACL and provenance gates protect graph promotion. Failed multi-graph writes
  are tested for rollback.
- The reasoner emits restore-readiness, recovery-blocker, trust, direct
  dependency-exposure, and direct blast-radius findings. It does not perform
  recursive or multi-hop traversal.
- The semantic service accepts approved query IDs rather than browser-supplied
  SPARQL, shapes typed envelopes, and is restricted to loopback/private use.
- The React UI provides Recovery Queue, Recovery Case, Review Inbox, and
  Platform Status workspaces.
- Three governed actions can create validated local audit records when the
  backend returns `AVAILABLE_FOR_LOCAL_AUDIT`. They do not execute work in an
  external system or mutate canonical, reasoning, operations, or source graphs.

See [Capability Truth Ledger](docs/capability_truth_ledger.md) for evidence per
claim and [Current Limitations](docs/current_limitations.md) for the production
gap.

## Exact portfolio scenarios

The public demonstration is limited to three seeded MVP scenarios:

1. `INC-GEN-SCN-20260611` — missing final validation and unresolved signoff.
2. `INC-GEN-SCN-20260613` — a GPU recovery case with a direct upstream chilled
   water dependency.
3. `INC-GEN-SCN-20260616` — conflicting validation evidence requiring review.

Their source rows, canonical mappings, validation boundary, reasoning results,
query contracts, UI routes, and action outcomes are documented in
[Verified Synthetic Scenario Walkthroughs](docs/10_operational_case_study.md).

## Run locally

Prerequisites: Docker and Node.js/npm.

Start Fuseki:

```bash
docker compose up fuseki
```

Generate, promote, and reason over the deterministic MVP fixture in another
terminal. This profile contains all three public scenario IDs:

```bash
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace/semantic-service \
  -e DCAI_FUSEKI_DATASET_URL=http://host.docker.internal:3030/infrastructure \
  gradle:8.10.2-jdk17 \
  gradle --no-daemon run --args="--repo-root=/workspace --generate-source-scenarios --generated-source-profile=mvp --generated-source-seed=20260610 --promote-source --source-release-id=generated-mvp-seed-20260610 --refresh-reasoning --reasoning-input-release-id=generated-mvp-seed-20260610 --reasoning-run-id=generated-mvp-seed-20260610-reasoning"
```

Run the private service:

```bash
docker run --rm -p 127.0.0.1:18080:18080 \
  -v "$PWD":/workspace \
  -w /workspace/semantic-service \
  -e DCAI_FUSEKI_DATASET_URL=http://host.docker.internal:3030/infrastructure \
  gradle:8.10.2-jdk17 \
  gradle --no-daemon run --args="--repo-root=/workspace --serve-private-query-endpoint"
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

This is a multi-command local demonstration, not a production deployment or a
single-command end-to-end installer.

## Action boundary

`semanticAvailableActionsByFinding` is the availability authority:

- `AcknowledgeRestoreBlocker` requires restore-readiness and recovery-blocker
  targets.
- `AssignEvidenceReview` requires a trust-finding target.
- `RecordValidationReview` requires a validation-evidence target.
- `RequestReasoningRefresh` remains `DISABLED` because there is no UI-facing
  reasoning runner.

The frontend may perform defensive target checks, but it cannot convert a
backend `DISABLED` action into an enabled control. Accepted submissions create
local action-audit and lifecycle records through private endpoints only.

## Verification

```bash
cd frontend
npm test
npm run lint
npm run build
```

```bash
docker run --rm \
  -v "$PWD":/workspace \
  -w /workspace/semantic-service \
  gradle:8.10.2-jdk17 \
  gradle --no-daemon test
```

SPARQL, RDF, SHACL, and additional repository checks are documented in
[Verification Plan](docs/06_verification_plan.md).

## Current documentation authority

Read these documents in order:

1. [Architecture](docs/01_architecture.md)
2. [Semantic API](docs/04_api.md)
3. [UI Specification](docs/05_ui_spec.md)
4. [Verification Plan](docs/06_verification_plan.md)
5. [Verified Synthetic Scenarios](docs/10_operational_case_study.md)
6. [Capability Truth Ledger](docs/capability_truth_ledger.md)
7. [Current Limitations](docs/current_limitations.md)

[Synthetic Operating Assumptions](docs/00_project_brief.md) explains why the
domain was modeled; it is not customer-discovery evidence.
[Local Runtime and Production Gaps](docs/09_production_rollout.md) separates
the runnable local stack from work required for production.

Historical phase, rewrite, and checkpoint notes are indexed under
[docs/ontology-native/README.md](docs/ontology-native/README.md). They preserve
implementation history and are not current product authority.

## Explicit non-claims

This repository does not claim real customer discovery, business ROI,
production readiness, authentication or authorization, production monitoring,
real DCIM/BMS/CMMS connectors, external work-order writeback, arbitrary source
ingestion, multi-hop topology inference, or real infrastructure control.
