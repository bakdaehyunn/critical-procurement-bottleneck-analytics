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

## Still Intentionally Left as Architecture Debt

The following concerns are real but are not fixed in this pass because they
would be product-structure work rather than low-risk helper extraction:

- `frontend/src/api.ts` is still a large semantic adapter for workbench/detail
  read-model DTOs and graph-to-UI mapping. Frontend hardening v1 split runtime
  config, approved query posting, ontology action commands, and AI governance
  commands into focused modules, but mapper/domain splitting remains future
  work.
- `frontend/src/App.tsx` is still a large workbench component tree. It should be
  split into route, finding list, finding detail, action panel, AI governance,
  dynamic playback, trust, impact, and dependency components.
- The approved query catalog is broad and continues to grow. The catalog should
  get ownership/version notes and stronger contract tests before more query IDs
  are added.
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

The next consolidation pass should focus on frontend and query-contract
structure, not another runtime layer:

1. Split the remaining `frontend/src/api.ts` read-model mappers by workbench,
   finding detail, dependency/reasoning, and trust/provenance domains.
2. Split `frontend/src/App.tsx` workbench sections into stable components.
3. Add stronger contract tests that keep the query catalog ownership table,
   semantic-service result envelopes, and frontend consumer modules aligned.
4. Keep all behavior unchanged unless a separate product/UI goal explicitly
   approves redesign.
