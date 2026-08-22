# Ontology Platform Layer Consolidation Hardening v1

This note records the first architecture hardening pass after the ontology-native
MVP gained source ingestion, promotion, reasoning, action audit, dynamic
playback, and AI governance review layers.

The goal is not to add another product capability. The goal is to keep the
layered ontology runtime maintainable while preserving the current private,
controlled, graph-native behavior.

## Current Layer Boundaries

| Layer | Current responsibility | Write boundary |
| --- | --- | --- |
| Source ingestion and promotion | Convert controlled source extracts into source, canonical, and provenance graphs. | Managed source/canonical/provenance graph URIs only. |
| Reasoning refresh | Derive dependency exposure, recovery blockers, restore readiness, trust, and blast-radius findings. | Managed reasoning-audit and reasoning graph URIs only. |
| Ontology action audit | Create governed local action requests and lifecycle transitions. | Managed action-audit graph only. |
| Dynamic playback | Record local replay state, graph transitions, reasoning deltas, and action lifecycle playback. | Managed action-audit graph only. |
| AI governance proposal/review | Record AI proposals, human approval/rejection decisions, and approved local action-request handoff. | Managed ai-audit graph and, for approved action recommendations, managed action-audit graph only. |
| Private semantic API | Execute approved query IDs and private internal action/review commands. | No raw SPARQL, public endpoint, auth layer, real connector, production writeback, or external mutation. |
| Frontend semantic workbench | Present graph-backed findings, provenance, trust, dependency, action, playback, and AI review state. | Calls approved read models and private internal commands only. |

## Consolidated in v1

- Private endpoint payload handling now uses one internal helper for:
  - raw SPARQL detection
  - string-only JSON object parsing
  - approved-field enforcement
  - controlled local identifier validation
  - controlled DCAI URN validation
  - semantic query parameter parsing
- Managed graph lifecycle writes now use one coordinator for:
  - target graph snapshots
  - ordered graph replacement
  - partial-write rollback
  - restoring previously existing graphs
  - deleting newly created graphs when a later write fails

This removes duplicated mechanics from source promotion, reasoning promotion,
ontology action audit, action lifecycle transitions, dynamic playback, AI
proposal creation, and AI proposal human review without changing product
behavior.

## Consolidated in the Cohesion Refactor

- The root `frontend/src/api.ts` module was removed. Recovery Queue, Recovery
  Case, Review Inbox, and Platform Status now own models and repositories, with
  enforced public cross-feature entry points.
- Query approval, codec selection, ownership, endpoint exposure, and paging
  policy now meet in `QueryContractRegistry`; the endpoint allowlist and result
  dispatch derive from it.
- SHACL parsing and RDFS type closure now run through one cached validation
  engine with domain-owned shape profiles.
- DCAI vocabulary and managed graph/controlled-ID policy moved to neutral
  packages.
- The live CLI path composes typed runtime operations through
  `SemanticServiceWorkflow`.
- Recovery Case now separates its typed model, query orchestration, semantic
  mappers, loading hook, and page components while preserving the existing URL
  tabs and keyboard contract.
- CLI parsing, runtime composition, typed workflow execution, reporting,
  loopback HTTP transport, and JSON writing now have separate production
  boundaries. The nullable compatibility runner has been removed from main
  source code.
- Structured OpenAPI/runtime-route validation now runs as part of static
  contract validation, and implemented internal action routes use typed request
  and response schema references.

## Remaining Architecture Debt

The following concerns remain intentionally narrow:

- AI review-to-action mapping is intentionally narrow. In v1, approved
  `ACTION_RECOMMENDATION` proposals only become governed
  `AcknowledgeRestoreBlocker` action requests when supported by restore-readiness
  evidence. More action mappings require a separate controlled design.
- Fixture loading still uses its fixture-specific writer boundary. It is not
  part of the production managed graph lifecycle helper.

## Guardrails Preserved

- No public endpoint was added.
- No auth surface was added.
- No raw SPARQL endpoint was exposed.
- No real connector or external-system writeback was added.
- No canonical, reasoning, provenance, source, operations, production, or
  external-system mutation was introduced outside the existing managed graph
  policies.
- UI behavior was not redesigned in this hardening pass.

## Next Hardening Candidate

Future work should add new domain behavior only behind the same feature,
registry, graph-policy, and typed-operation boundaries. Keep behavior unchanged
unless a separate product/UI goal explicitly approves redesign.
