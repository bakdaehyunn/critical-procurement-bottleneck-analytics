# Topology, Semantic Graph, And Connectors

## Topology Graph

Topology is represented as RDF dependency paths and dependency edges. The
semantic operations workbench consumes topology through approved semantic read
models, especially
`semanticTopologyDependencies`, `semanticDependencyImpactByAsset`, and
`semanticBlastRadiusByAsset`.

The intended dependency vocabulary covers paths such as:

```text
rack -> PDU -> UPS -> switchgear -> generator
rack -> CRAH/CDU/chiller
```

Topology should stay focused on explaining dependency impact, not replacing the
follow-up queue as the primary product workflow.

## Semantic Graph Runtime

- Fuseki/TDB2 stores named graphs.
- `ontology/modules/` defines OWL/RDFS vocabulary.
- `shapes/` validates source, canonical, topology, evidence, provenance,
  reasoning, and AI governance contracts.
- `queries/manifest.ttl` approves executable query IDs.
- `semantic-service/` executes approved read-only queries and shapes typed
  response envelopes.

## Connector Direction

Production connectors should map external extracts into RDF named graphs with
source-record provenance. A connector is not trusted just because it loads
data; it must pass parse, SHACL, provenance, and promotion gates before product
read models depend on it.

Required connector contract areas:

- incidents
- workflow events
- work orders and spares
- validation results
- telemetry alerts and readings
- impact observations
- topology dependencies
- source system provenance

## Local Recorded Contract

The current MVP implementation has a local recorded source-system contract, not
a real external connector:

- contract fixture:
  `fixtures/source-extracts/connector-contracts/recorded-source-system-v1.properties`
- recorded exports:
  `fixtures/source-extracts/recorded-source-systems/local-ops-v1/`
- generated scenario exports:
  `fixtures/source-extracts/generated-scenarios/`

Generated batches include `scenario_inventory.csv` so reviewers can see why a
batch should exercise dependency exposure, blast radius, trust findings,
restore readiness, and recovery blockers. That inventory is explanatory and is
not promoted into canonical RDF in v1.

The local contract is useful for deterministic MVP verification, but it remains
separate from future source-system-specific connector contracts for DCIM, BMS,
work-order, validation, topology, and telemetry platforms.
