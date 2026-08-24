# Architecture

## Flow

```text
scattered AI infrastructure source records
  -> source-to-canonical RDF mappings
  -> named RDF graphs in Fuseki/TDB2
  -> OWL/RDFS ontology modules
  -> SHACL validation gates
  -> approved read-only SPARQL queries
  -> Kotlin/JVM semantic-service
  -> React semantic operations workbench
```

The RDF graph store is the source of truth. The semantic-service is the
controlled application boundary over approved query IDs, typed result
envelopes, provenance, and semantic error contracts.

## Source System Integration Model

The project models source families that are commonly fragmented in AI data
center operations:

| Source family | Canonical semantic target | Operational question answered | Trust risk |
| --- | --- | --- | --- |
| Incident export | `dcai:InfrastructureIncident` | Which source-provided stage and asset are recorded? | Missing required fields, stale current stage, duplicate source incident |
| Workflow event export | workflow stages and event records | Which events were recorded and when? | Missing stage evidence, duplicate event, mismatch with source-provided current stage |
| Facility work orders | `dcai:WorkOrderEvidence` | Who owns repair work and whether work is waiting, started, or complete? | Work order without incident, waiting state without spare evidence |
| Spare and inventory context | work-order spare fields and blocker findings | Is the blocker stock, critical spare availability, or vendor dispatch? | Out-of-stock spare, missing required spare link |
| Vendor ETA context | impact/vendor state fields | Is external recovery late, confirmed, or not required? | ETA in the past without missed status, event/snapshot mismatch |
| Telemetry | `dcai:TelemetryEvidence` | Is thermal, power, or redundancy exposure supported by monitoring evidence? | Alert without known asset, thermal breach without abnormal reading |
| Validation and impact | `dcai:ValidationEvidence` and `dcai:ImpactObservation` | Is return-to-service safe, and how much rack/GPU/capacity exposure remains? | Validation before completed work, stale or missing impact snapshot |
| Infrastructure topology | dependency paths and dependency impact findings | Which upstream power, cooling, telemetry, or redundancy assets does an affected asset depend on? | Missing asset reference, invalid dependency type, stale topology extract |

These are source families within one local recorded connector format, not real
heterogeneous connectors. Accepted rows are mapped into canonical RDF with
source-record provenance. Provenance contains source identity, record IDs, and
payload hashes; complete CSV payloads remain in the recorded files. Graph
promotion requires parseable RDF, SHACL conformance, and provenance links.

## Runtime Responsibilities

- Fuseki/TDB2 stores persistent named RDF graphs.
- OWL/RDFS modules define the domain vocabulary.
- SHACL shapes validate canonical, evidence, topology, provenance, reasoning,
  and AI interaction contracts.
- Approved SPARQL files under `queries/` define read models.
- `queries/manifest.ttl` supplies approved definitions; the Kotlin
  `QueryContractRegistry` is the runtime authority that binds each definition
  to its codec, owner, endpoint exposure, and paging policy.
- The Kotlin/JVM semantic-service loads contracts, reads Fuseki graphs,
  executes approved read-only SPARQL, shapes typed envelopes, serializes
  semantic responses, and rejects unapproved query IDs.
- The React semantic operations workbench preserves the follow-up workflow UX while reading from
  the semantic-service private endpoint.
- One cached semantic validation engine loads ontology superclass data once and
  caches explicit shape profiles; domain gates retain their provenance and
  policy checks.
- Neutral ontology vocabulary and managed graph/identifier policies live under
  `ontology` and `graph`, rather than under ingestion or feature packages.
- The local CLI composes typed runtime operations and executes them through
  `SemanticServiceWorkflow`; service/plan pairs are not nullable. CLI parsing,
  composition, reporting, loopback HTTP transport, pagination, and JSON writing
  are separate boundaries.
- Static contract validation parses OpenAPI path metadata and checks implemented
  private routes against runtime route constants; marker checks are no longer
  the only API/spec enforcement.

## Design Choices

### RDF as Runtime Authority

The old relational backend has been removed from the active source tree.
Current product reads must come from named graphs and approved semantic-service
queries.

This is an implementation choice, not a benchmark result. A relational design
could represent the same domain. The graph model is useful here because direct
dependencies, source derivations, explicit versus inferred facts, and SHACL
shape constraints are first-class edges/contracts consumed by SPARQL. The
repository has not measured graph-versus-relational latency, storage cost,
operational complexity, or maintainability, so it does not claim that RDF is
universally or empirically superior to PostgreSQL.

### Approved Query Boundary

The service executes query IDs, not arbitrary browser-supplied SPARQL. This
keeps graph access inspectable, testable, and safe for a future private API.

### Frontend Feature Boundary

Recovery Queue, Recovery Case, Review Inbox, and Platform Status own their
models and repositories. Cross-feature consumers use public feature entry
points, while ESLint rejects imports into another feature's internals. Semantic
responses are validated at runtime, including query identity, provenance,
paging totals, and query-specific required record fields.

Recovery Case additionally separates its resource repository, typed model,
semantic mappers, and lifecycle hook. Missing numeric or categorical facts stay
nullable/unknown through mapping and presentation instead of becoming zero or
a successful state.

### Provenance as Product Data

Follow-up rows, evidence details, trust findings, topology dependencies, and
reasoning outputs carry graph/source provenance so operators can see why a
decision is trusted or needs review.

### Bounded reasoning

The current reasoner derives restore-readiness, blocker, trust, direct
dependency-exposure, and direct blast-radius findings. Dependency logic follows
one explicit edge only. It does not reconstruct current incident stage, derive
queue rank/score/recommended-action facts, or traverse multi-hop topology.

### Follow-Up Workflow First

The UI should not become an ontology diagram for its own sake. It exposes
semantic evidence where it supports the field decision: which incident to chase
next, what is blocking recovery, whether impact evidence is trustworthy, and
which dependencies increase operational exposure.
