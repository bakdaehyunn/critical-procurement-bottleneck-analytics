# Recorded Source Connector Contract v1

This is the local MVP connector contract for recorded source-system simulation.
It does not create a real external connector and does not authorize production
data ingestion.

## Boundary

The controlled contract fixture is:

- `fixtures/source-extracts/connector-contracts/recorded-source-system-v1.properties`

The contract is consumed as documentation and test fixture metadata. Runtime
promotion still flows through the existing internal path:

1. recorded CSV exports under `fixtures/source-extracts/`
2. `RecordedSourceConnectorSimulationLoader`
3. `SourceExtractBatch`
4. RDF mapper
5. SHACL and provenance gates
6. managed source, canonical, provenance, reasoning, and audit graph policies

## Contract-Bearing Files

The loader maps these files into approved `SourceExtract` DTO families:

- `facilities.csv`
- `zones.csv`
- `assets.csv`
- `incidents.csv`
- `dependencies.csv`
- `workflow_events.csv`
- `work_orders.csv`
- `validation_results.csv`
- `telemetry_impacts.csv`

Invalid rows and duplicate natural keys are quarantined in the connector load
report. They are not promoted.

## Scenario Inventory

Generated batches now include `scenario_inventory.csv`. This file explains the
operational scenario behind a recorded export: scenario type, narrative,
expected reasoning focus, source-system families, and replay window.

The scenario inventory is intentionally not promoted into canonical RDF in v1.
It is a local fixture explanation layer for MVP review and browser verification,
not source truth.

## Real Connector Readiness

A future production connector should replace the local recorded CSV directory
with source-system-specific extract contracts, but preserve these behaviors:

- stable connector contract ID and version
- deterministic replay metadata
- source-file and row-level quarantine reports
- source-record provenance for accepted rows
- SHACL and provenance gates before canonical promotion
- no direct UI or browser raw SPARQL access
- no graph mutation outside managed graph URI policy

This v1 keeps the project honest: it strengthens the simulated source-system
boundary without pretending that external systems are integrated.
