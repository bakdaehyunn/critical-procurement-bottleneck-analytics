# Restore Return-to-Service Console functional integrity

This ExecPlan is a living document. Keep `Progress`, `Surprises & Discoveries`, `Decision Log`, `Verification`, and `Outcomes & Retrospective` current as work proceeds.

## Purpose / Big Picture

Restore the operational behavior that was lost or approximated during the queue-first redesign. Reviewers must be able to resolve real AI and governed-action work, operators must provide auditable decision inputs, Platform Status must report authoritative or explicitly unknown state, and large semantic result sets must be bounded before they reach React. Existing routes and governed write contracts remain compatible, and no new external-system write path is introduced.

## Progress

- [x] Approved goal and repository constraints reviewed.
- [x] Current branch and redesign diff inspected.
- [x] Initial regression matrix recorded.
- [x] Authoritative Review Inbox read model and actions restored.
- [x] Editable governed-action request workflow restored.
- [x] Read-model deduplication and service pagination implemented.
- [x] Authoritative Platform Status and route-specific partial/stale loading implemented.
- [x] Browser-history behavior and documentation corrected.
- [x] Focused automated tests added and all required verification completed.
- [x] Completion audit and local uncommitted handoff completed.

## Regression Matrix

| Capability at `bac9da9` | Current redesigned state | Required resolution | Evidence of completion |
| --- | --- | --- | --- |
| AI proposal approve/reject via `submitAiProposalReview` | API remains, UI is not rendered; Review Inbox does not consume the approved global AI queue | Load unresolved AI proposals, collect reviewer reason, invoke existing API, refresh authoritative state | Component test plus browser approve/reject against fixtures; mutation flags remain false outside audit graphs |
| Action lifecycle Start review/Approve/Reject/Close via `submitOntologyActionTransition` | API and per-incident query remain, controls removed | Expose unresolved lifecycle work in Review Inbox and case history; invoke only valid transitions | Transition tests and browser lifecycle progression |
| Editable action request modal | Replaced by `window.confirm` and hardcoded actor/reason/team/status/summary | Restore validated editable inputs with safe defaults and existing payload contract | Payload tests for all three supported actions and browser submission |
| Real review records | Evidence/validation cards are synthesized from follow-ups; requester/age fallback is fabricated presentation | Separate persisted reviews from derived attention signals; never invent identity/time/status | Review item contract tests and UI inspection |
| Review actions | Lifecycle cards have no destination/handler and top-level ontology rows are forced disabled | Every enabled control navigates or executes; unsupported graph promotion/reasoning remains explicitly disabled | Static handler audit and browser interaction audit |
| Bounded result processing | React deduplicates and slices after all raw rows arrive | Deduplicate to stable latest entities in read models and return backward-compatible paged envelopes | Query/service tests and bounded browser network payloads |
| Platform state | Pipeline status is hardcoded; health ignores pipeline; freshness is client request time | Add authoritative read-only platform state with Operational/Degraded/Unknown and reason codes | Query/service fixtures for all verdicts and browser states |
| Independent data states | Global provider and case loaders use all-or-nothing `Promise.all` | Route-specific hooks retain usable sections, expose partial failure, and mark stale last-known data | Component/browser isolated-failure tests |
| Back/Forward state | Filters, pagination, and tabs use `replace: true` | Push meaningful committed state; avoid search-keystroke history spam | Browser Back/Forward, reload, deep-link tests |
| Complete governance history | Several fetched queues/histories are unused; transition count is shown without transition rows | Render the operationally relevant current queue and transition history behind progressive disclosure | Case browser audit and content tests |

## Surprises & Discoveries

- Observation: The approved global `semanticAiProposalReviewQueue` query, result shaper, and private endpoint registration already exist; the redesign simply stopped consuming them.
  Evidence: `queries/read-model/semantic_ai_proposal_review_queue.select.rq`, `QueryResultShaper.kt`, and `PrivateSemanticQueryEndpoint.kt`.
- Observation: `semanticActionReviewQueueByIncident` is named per-incident but its SPARQL filter uses `COALESCE`, so omitting `incidentIdParam` already returns the global action queue.
  Evidence: `queries/read-model/semantic_action_review_queue_by_incident.select.rq`.
- Observation: `buildOntologyReviewQueue` discards the service `actionStatus` and hardcodes every item to `DISABLED`.
  Evidence: `frontend/src/api.ts` mapping for `SemanticOntologyReviewQueueRecord`.
- Observation: `buildOverview` hardcodes `latest_pipeline_run_status` to `SEMANTIC_GRAPH`, which is a data-source label rather than a run outcome.
  Evidence: `frontend/src/api.ts` `buildOverview`.
- Observation: No frontend automated test script currently exists.
  Evidence: `frontend/package.json` exposes only `dev`, `build`, `lint`, and `preview`.
- Observation: Fixture-backed runtime QA exposed source-backed action target rows with unbound `detailValue`, causing every case action to appear disabled even though the same query result contained the required finding variables.
  Evidence: direct Fuseki inspection of `semanticAvailableActionsByFinding` for `INC-001`; fixed by resolving and synthesizing authoritative targets in `QueryResultShaper` before serialization.
- Observation: Action audit history contained repeated graph observations for the same execution URI and produced duplicate React keys.
  Evidence: browser console during Recovery Case QA; fixed by stable execution-identity deduplication in `QueryResultShaper`.

## Decision Log

- Decision: Preserve all existing write endpoints and add only read-side contracts or optional response metadata.
  Rationale: The approved goal forbids new write contracts and backward-incompatible API changes.
  Date/Author: 2026-08-10 / Codex
- Decision: Treat evidence and validation conditions without persisted review identity as `attention signals`, not as open reviews.
  Rationale: Requester, age, assignment, and lifecycle state must never be fabricated.
  Date/Author: 2026-08-10 / Codex
- Decision: Prefer stable identity plus latest authoritative timestamp/state over client-side maximum severity or maximum aggregate completeness.
  Rationale: Worst-ever and most-complete heuristics can retain stale state.
  Date/Author: 2026-08-10 / Codex
- Decision: Keep unsupported promotion/reasoning controls disabled until an already-implemented governed endpoint is proven executable; do not implement a new write path.
  Rationale: The goal explicitly forbids new writes.
  Date/Author: 2026-08-10 / Codex
- Decision: Use route-specific hooks without adding a general client cache library.
  Rationale: This fixes all-or-nothing loading while minimizing dependencies and preserving the existing React/Vite architecture.
  Date/Author: 2026-08-10 / Codex
- Decision: Resolve action target markers and missing target rows in the semantic result shaper from bindings already returned by the approved query.
  Rationale: This restores the existing audited write contract at the read-model boundary without fabricating client targets or adding a write endpoint.
  Date/Author: 2026-08-10 / Codex

## Context and Orientation

- Branch: `codex/ontology-mvp-verification`; baseline redesign commit: `faadbf4`; comparison commit: `bac9da9`.
- Frontend routes live in `frontend/src/App.tsx`; redesigned feature modules live under `frontend/src/features/`.
- Approved semantic queries are declared in `queries/manifest.ttl`, registered in `PrivateSemanticQueryEndpoint.kt`, and shaped in `QueryResultShaper.kt`.
- Governed write clients are `frontend/src/ontologyActionApi.ts` and `frontend/src/aiGovernanceApi.ts`.
- Required checks run from the repository root unless a working directory is stated explicitly.

## Plan of Work

1. Define typed review records and page envelopes, reconnect existing global AI/action queues, and preserve graph review items as honest read-only records.
2. Restore AI decision, lifecycle transition, and governed action request forms with editable audited inputs and refresh behavior.
3. Correct SPARQL multiplicity and add optional service-side page metadata without breaking existing query responses.
4. Add an authoritative platform-status read model and remove hardcoded/inferred health claims.
5. Replace the global dashboard fetch path with feature-focused loaders that expose loading, error, stale, and partial states.
6. Correct URL-history behavior, update documentation/contracts, and add focused frontend/service/query tests.
7. Run the full verification matrix, capture screenshots, review the diff, and leave changes local and uncommitted.

## Concrete Steps

- `cd frontend && npm run test && npm run lint && npm run build`
- `python3 queries/validate_sparql.py`
- `cd semantic-service && gradle test`
- Start Fuseki, the private semantic endpoint, and Vite only for fixture-backed browser QA; stop temporary services afterward.
- Run `git diff --check`, inspect `git diff --stat`, and review every changed file before handoff.

## Validation and Acceptance

- Review Inbox displays unresolved authoritative work only; derived attention signals are clearly separated.
- AI Approve/Reject and valid action lifecycle transitions execute existing endpoints and refresh state.
- All three supported action requests submit operator-controlled fields with validation.
- No enabled inert controls exist.
- Paged routes receive bounded result records and correct totals; duplicate joins are resolved before React.
- Platform verdicts are source-backed and support Operational, Degraded, and Unknown.
- Independent query failure leaves unaffected sections usable and visibly marks stale/partial data.
- Filters, pagination, tabs, Back/Forward, reload, keyboard behavior, and mobile layouts work.
- Governed response flags confirm no canonical, reasoning, operations, production, or external-system mutation.
- Documentation describes only implemented behavior.

## Idempotence and Recovery

Read queries, tests, builds, and fixture-backed browser checks are safe to repeat. Governed action browser tests must use disposable local fixture audit releases and unique request/review IDs. Do not reset or delete user data. If a write-path test partially succeeds, preserve its audit evidence, switch to a fresh test release identifier, and document the prior result.

## Verification

- Frontend: 5 focused Vitest assertions passed; ESLint passed; TypeScript/Vite production build passed.
- Semantic service: 205 tests passed with zero failures or errors after the final action-target and audit-history deduplication additions.
- Query/contracts: every SPARQL file parsed; OpenAPI YAML parsed.
- Fixture browser QA: Recovery Queue, Review Inbox categories, authoritative totals/pagination, read-only lifecycle controls, governed transition form, Recovery Case tabs and keyboard navigation, editable case action form, Platform Status, mobile queue layout, Back/Forward, and retained stale snapshot were exercised.
- No governed form was submitted during browser QA; no external system mutation occurred.
- Post-fix browser rerun loaded the Recovery Case, editable action affordance, and transition history with zero console warnings or errors.
- Dependency audit: `npm audit` and `npm audit --omit=dev` both report zero vulnerabilities after compatible React Router and transitive toolchain updates.
- QA screenshot: `/Users/hennei/.codex/visualizations/2026/08/09/019fe660-24cc-75e2-bc4a-ed2d4a11d3a8/return-to-service-console/recovery-queue-stale-state.png`.

## Artifacts and Notes

- Final screenshots will be stored outside the repository under the active Codex visualization workspace.
- No commit, push, deployment, or hosting change is authorized.

## Interfaces and Dependencies

- Routes `/`, `/recovery-cases/:incidentId`, `/findings/:incidentId`, `/reviews`, and `/platform-status` remain stable.
- Existing `POST /semantic/internal/action-request`, `POST /semantic/internal/action-transition`, and `POST /semantic/internal/ai-proposal-review` payloads remain stable.
- Existing semantic query IDs and response fields remain accepted; read-side additions must be backward-compatible.
- Minimal dev-only frontend test dependencies are allowed; no runtime dependency is required by this plan.

## Outcomes & Retrospective

- Review Inbox now distinguishes authoritative queues from derived case-attention signals and reports server-owned result totals.
- Existing AI review, governed action-transition, and audited case-action contracts are reachable through validated editable UI forms; unsupported lifecycle writes remain explicitly read only.
- Stable-identity deduplication and optional page metadata are owned by the semantic boundary, including the runtime-discovered action target and audit-history multiplicity fixes.
- Platform Status now reports persisted evidence or `Unknown`, and route-specific loaders preserve usable last-known data with partial/stale warnings.
- URL navigation, keyboard tabs, mobile layout, and post-fix browser console behavior passed fixture-backed QA.
- Work remains local and uncommitted; no push, deployment, or external-system mutation occurred.
