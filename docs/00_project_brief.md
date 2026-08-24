# Synthetic Operating Assumptions

This document records design assumptions used to construct a portfolio
prototype. It is not evidence of interviews, customer discovery, field trials,
or measured business impact.

## Modeled users and decisions

| Hypothetical user | Modeled decision | UI surface |
| --- | --- | --- |
| Shift lead | Which recorded recovery case should be inspected? | Recovery Queue |
| Facilities supervisor | What source evidence blocks the selected recovery? | Recovery Case |
| Evidence reviewer | Does a conflict require a local audited review record? | Review Inbox / Recovery Case |
| Platform engineer | Are the local semantic graphs and read models inspectable? | Platform Status |

The prototype assumes operational evidence may be split across incident,
workflow, work-order, validation, telemetry/impact, asset, zone, and dependency
exports. A single recorded CSV contract simulates those families. No real
organization or source product supplied the data.

## Scope proved by the repository

- Deterministic recorded fixtures can be mapped to RDF with source identity and
  payload-hash provenance.
- Invalid or duplicate recorded rows can be quarantined before promotion.
- SHACL/provenance gates can prevent invalid graph promotion.
- Explicit facts and deterministic findings can be queried through approved
  read models and presented in an operator-oriented UI.
- Selected actions can be recorded in a local governed audit graph without
  executing external work.

## Questions deliberately left open

- Would domain experts accept the ontology, thresholds, and action vocabulary?
- Would this reduce shift-handoff time or incident duration?
- Would a graph implementation outperform or simplify a relational design at
  production scale?
- Which real DCIM, BMS, CMMS, incident, or telemetry contracts are available?
- What identity, authorization, retention, monitoring, recovery, and deployment
  controls would an operator require?

Those are production discovery and validation tasks, not conclusions from this
repository.
