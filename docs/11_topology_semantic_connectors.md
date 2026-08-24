# Topology, Semantic Graph, And Connectors

## Topology Graph

Topology is represented as RDF dependency paths and dependency edges. The
semantic operations workbench consumes topology through approved semantic read
models, especially
`semanticTopologyDependencies`, `semanticDependencyImpactByAsset`, and
`semanticBlastRadiusByAsset`.

The committed generated scenarios use explicit single edges such as GPU pod to
UPS feed and GPU pod to chilled-water loop. The current reasoner follows one
edge and its declared path resource. It does not infer rack-to-PDU-to-UPS chains,
recursively traverse topology, discover topology, or aggregate transitive blast
radius.

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

Future production connectors could map external extracts into RDF named graphs with
source-record provenance. A connector is not trusted just because it loads
data; it must pass parse, SHACL, provenance, and promotion gates before product
read models depend on it.

Potential source-specific connector areas:

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

The local contract is useful for deterministic verification, but every CSV
family uses that same format and loader. Source-specific DCIM, BMS, work-order,
validation, topology, and telemetry connectors are not implemented.
