# Ontology Action Layer v1 Design

Date: 2026-06-13

## Purpose

This document defines a controlled ontology action layer for the ontology-native
semantic operations platform. It is inspired by Palantir-style ontology actions,
where users operate on real-world objects through governed transactions rather
than editing raw graph triples or backend tables.

The v1 action layer is a design contract. It does not expose public write
endpoints, add authentication, mutate production data, or allow browser-supplied
SPARQL/SPARQL Update. Runtime implementation must remain behind internal
semantic-service boundaries, managed graph URI policy, SHACL/provenance gates,
rollback behavior, and future authorization decisions.

## Success Contract

The action layer is acceptable only if every action:

- maps to a real ontology object or object set
- has an explicit user intent, parameter contract, preconditions, and disabled
  reason
- records a durable action audit event before any promoted graph state changes
- validates candidate facts with SHACL where graph state could change
- attaches provenance linking the action, actor, source facts, candidate facts,
  validation report, and promotion or rejection outcome
- preserves last-known-good canonical/reasoning/operations graphs on validation
  or write failure
- never exposes raw SPARQL, arbitrary graph URIs, or unrestricted graph writes

## Existing Boundaries

Current implementation state:

- The React UI is read-only over approved semantic query IDs.
- The private semantic endpoint is loopback-only and handles
  `POST /semantic/query/{queryId}` for approved read-only query IDs.
- Source promotion and reasoning refresh already run through internal
  semantic-service paths with SHACL/provenance gates and rollback snapshots.
- Phase 8 and Phase 9 reserve service boundaries for provenance lookup,
  promotion review, and AI governance handoff, but they do not define a general
  operator action layer.
- `ontology/modules/operations.ttl`, `ontology/modules/provenance.ttl`, and
  `ontology/modules/ai-interaction.ttl` provide the closest existing vocabulary
  anchors: operational findings, source/promotion/reasoning activities, and
  approval/guardrail concepts.

## Action Layer Object Model

The v1 action model introduces these conceptual resources. They may be added to
the ontology in a later implementation phase, but this design keeps them
contract-level until runtime authority is approved.

| Concept | Purpose | Candidate ontology placement |
| --- | --- | --- |
| `OntologyActionType` | Stable action definition such as `AcknowledgeRestoreBlocker`. | `operations.ttl` |
| `OntologyActionRequest` | User-initiated request to execute an action. | `operations.ttl` |
| `OntologyActionExecution` | Audit record for validation, execution, and outcome. | `provenance.ttl` or `operations.ttl` |
| `OntologyActionDecision` | Approval, rejection, or review decision for governed actions. | `operations.ttl`, with AI-specific decisions still in `ai-interaction.ttl` |
| `ActionValidationReport` | SHACL/policy/precondition result for the action request. | `evidence.ttl` or `operations.ttl` |
| `ActionAuditGraph` | Managed graph for action requests, decisions, validation reports, and audit lineage. | `urn:dcai:graph:action-audit:*` |

The action layer must distinguish three graph states:

| State | Meaning |
| --- | --- |
| Requested | The user asked for an action, but no graph state has changed. |
| Candidate | The action produced candidate facts in an audit graph or reasoning-audit graph. |
| Promoted | The candidate facts passed validation, provenance, and policy gates and were written to an approved graph. |

## Managed Graph Policy

V1 actions may use only managed graph URI families:

| Graph family | Purpose | Write authority |
| --- | --- | --- |
| `urn:dcai:graph:action-audit:*` | Action requests, decisions, validation reports, disabled reasons, and immutable audit events. | Internal semantic-service action runner only |
| `urn:dcai:graph:reasoning-audit:*` | Candidate reasoning findings awaiting approval/rejection. | Existing reasoning refresh path plus action runner |
| `urn:dcai:graph:reasoning:*` | Approved reasoning findings. | Existing reasoning promotion service only |
| `urn:dcai:graph:operations:*` | Future approved operator workflow state. | Not implemented in v1 |
| `urn:dcai:graph:provenance:*` | Source, promotion, reasoning, and action lineage. | Internal semantic-service graph services only |

V1 must not let the UI submit graph names. The runtime chooses graph URIs from
release/batch/action metadata.

## Action Contracts

### 1. `AcknowledgeRestoreBlocker`

Purpose: record that an operator reviewed a restore-readiness blocker and
understands why restore is still blocked.

| Field | Contract |
| --- | --- |
| Primary objects | `RestoreReadinessFinding`, `RecoveryBlocker`, `InfrastructureIncident` |
| UI placement | Selected finding Summary action area; Trust tab when blocker is trust-related |
| Parameters | `findingUri`, `incidentUri`, `actorId`, `acknowledgedAt`, `acknowledgementReason`, optional `nextReviewAt` |
| Preconditions | Finding exists; finding status is `NOT_READY` or `REVIEW`; incident is not terminal/restored; source and reasoning provenance exist; actor is allowed by future policy |
| Candidate facts | `OntologyActionExecution` with action type, actor, timestamp, target finding, acknowledgement reason, and `prov:used` links to blocker, readiness, and evidence facts |
| SHACL/provenance gates | Action request has target object, actor, timestamp, reason, and `prov:used` links; target finding has `prov:wasGeneratedBy` reasoning activity |
| Promotion behavior | V1 records audit only. Future operations graph may mark blocker as acknowledged for UI state. |
| Rollback behavior | If audit graph write fails, no approved graph changes occur. If future operations write fails, restore previous operations graph snapshot. |
| Disabled reasons | Missing provenance; finding already terminal; incident restored; actor lacks future permission; missing acknowledgement reason |

### 2. `AssignEvidenceReview`

Purpose: assign a trust/evidence problem to an operator or team without editing
the canonical evidence fact itself.

| Field | Contract |
| --- | --- |
| Primary objects | `TrustFinding`, `EvidenceRecord`, `InfrastructureIncident`, `SourceRecord` |
| UI placement | Trust tab action area; Summary canvas when trust state is not `TRUSTED` |
| Parameters | `trustFindingUri`, `incidentUri`, `assignedTeam`, optional `assigneeId`, `dueAt`, `priority`, `assignmentReason` |
| Preconditions | Trust finding exists; trust state is `WARNING`, `UNVERIFIED`, or equivalent; referenced evidence/source record is available; assignment target is from controlled team vocabulary |
| Candidate facts | Evidence review assignment action execution linked to trust finding, source record, and evidence record |
| SHACL/provenance gates | Assignment has exactly one trust finding target, controlled assignment team, timestamp, actor, and provenance links |
| Promotion behavior | V1 records audit only. Future operations graph may expose active review assignment state. |
| Rollback behavior | Failed audit write leaves findings and provenance unchanged. Future operations write restores snapshot on failure. |
| Disabled reasons | No trust finding; trusted evidence; missing source record; uncontrolled assignment target; missing due date when policy requires one |

### 3. `RecordValidationReview`

Purpose: record operator review of validation evidence without allowing direct
browser edits to canonical validation evidence.

| Field | Contract |
| --- | --- |
| Primary objects | `ValidationEvidence`, `WorkflowEvent`, `WorkOrderEvidence`, `InfrastructureIncident`, `RestoreReadinessFinding` |
| UI placement | Trust tab validation evidence section; Summary action area when current stage is Validation |
| Parameters | `incidentUri`, `validationEvidenceUri`, `reviewedStatus`, `reviewerId`, `reviewedAt`, `reviewSummary`, optional `supportingEvidenceUri` |
| Preconditions | Incident is in or blocked by validation; validation evidence exists or the action is explicitly a missing-evidence review; reviewed status uses controlled vocabulary; conflicting evidence is surfaced before submission |
| Candidate facts | Action execution and validation review note in action audit graph; future source-system connector may turn approved review into source evidence |
| SHACL/provenance gates | Controlled `reviewedStatus`; reviewer and timestamp required; action must cite validation evidence or missing-evidence finding |
| Promotion behavior | V1 audit only. It must not overwrite canonical validation evidence. Future implementation should write back through a source-system connector or operations graph. |
| Rollback behavior | Audit write failure leaves all graphs unchanged. Future writeback failures must not change canonical graph and must record rejection in action audit. |
| Disabled reasons | No validation context; uncontrolled status; unresolved conflicting evidence; missing reviewer; restored incident |

### 4. `ApproveReasoningFinding`

Purpose: approve a candidate reasoning finding for promotion from audit state to
the approved reasoning graph.

| Field | Contract |
| --- | --- |
| Primary objects | `DependencyImpactFinding`, `RecoveryBlocker`, `RestoreReadinessFinding`, `TrustFinding`, `BlastRadiusFinding`, `ReasoningActivity` |
| UI placement | Admin/lifecycle review view; selected finding advanced section only when displaying candidate/audit state |
| Parameters | `candidateFindingUri`, `reasoningActivityUri`, `actorId`, `decisionAt`, `approvalReason`, optional `approvedUntilGraphRelease` |
| Preconditions | Candidate exists in reasoning-audit graph; reasoning-output SHACL validation conforms; candidate has `prov:wasDerivedFrom` and `prov:wasGeneratedBy`; canonical graph release is current; no conflicting higher-priority rejection exists |
| Candidate facts | Approval decision in action audit graph linked to candidate finding, validation report, canonical release, and reasoning activity |
| SHACL/provenance gates | Candidate output shapes conform; approval decision has actor, time, reason, target, and validation report |
| Promotion behavior | Existing reasoning promotion service promotes approved candidate output to `urn:dcai:graph:reasoning:*` with rollback snapshots. |
| Rollback behavior | If promotion write fails, restore previous reasoning graph snapshot and keep approval decision plus failure evidence in action audit. |
| Disabled reasons | SHACL failure; missing provenance; stale canonical release; already approved/rejected; unresolved conflict |

### 5. `RejectReasoningFinding`

Purpose: reject a candidate reasoning finding while preserving the reason and
evidence for auditability.

| Field | Contract |
| --- | --- |
| Primary objects | Same reasoning finding classes as `ApproveReasoningFinding` |
| UI placement | Admin/lifecycle review view; selected finding advanced section for candidate/audit state |
| Parameters | `candidateFindingUri`, `reasoningActivityUri`, `actorId`, `decisionAt`, `rejectionReason`, optional `replacementRequested` |
| Preconditions | Candidate exists in reasoning-audit graph; actor can review; rejection reason is present; candidate has provenance or rejection explicitly cites missing provenance |
| Candidate facts | Rejection decision in action audit graph linked to candidate finding and validation/provenance evidence |
| SHACL/provenance gates | Decision shape requires target, actor, timestamp, reason, and outcome `REJECTED` |
| Promotion behavior | No approved reasoning graph write. Candidate remains in audit graph as rejected or is superseded by a later reasoning run. |
| Rollback behavior | Failed audit write leaves approved graph unchanged and does not delete candidate findings. |
| Disabled reasons | Missing rejection reason; candidate already promoted; actor lacks future permission |

### 6. `RequestReasoningRefresh`

Purpose: request an internal reasoning refresh over a controlled canonical graph
release or limited incident/asset scope.

| Field | Contract |
| --- | --- |
| Primary objects | Canonical graph release, `InfrastructureIncident`, `InfrastructureAsset`, reasoning rule set |
| UI placement | Admin/lifecycle status view; optional detail advanced action when a selected finding appears stale |
| Parameters | `scopeType`, `scopeUri`, `requestedBy`, `requestReason`, optional `ruleSetId`, optional `canonicalGraphRelease` |
| Preconditions | Canonical graph exists and conforms; no refresh is already running for same scope; rule set is approved; scope is controlled and not browser-supplied graph URI |
| Candidate facts | Reasoning refresh request in action audit graph; reasoning run produces candidate output in reasoning-audit graph |
| SHACL/provenance gates | Refresh request has controlled scope, actor, timestamp, rule set, and canonical release; reasoning output validates before promotion |
| Promotion behavior | Internal reasoning service may execute refresh; approval/promotion remains separate unless policy allows auto-approval for low-risk findings |
| Rollback behavior | If refresh fails, previous approved reasoning graph remains last-known-good. Record failure in action audit. |
| Disabled reasons | Missing conforming canonical graph; stale or unknown release; unsupported scope; existing refresh in progress; unapproved rule set |

### 7. `ApprovePromotionBatch`

Purpose: approve promotion of a validated source/canonical/provenance or
reasoning batch after graph lifecycle review.

| Field | Contract |
| --- | --- |
| Primary objects | `PromotionActivity`, source extract batch, canonical graph candidate, provenance graph candidate, reasoning release manifest |
| UI placement | Admin/lifecycle review view only, not operator inbox |
| Parameters | `promotionBatchId`, `candidateGraphUris`, `releaseManifestId`, `actorId`, `decisionAt`, `approvalReason` |
| Preconditions | Candidate graphs use managed URIs; source/canonical/reasoning SHACL gates pass; provenance coverage is complete; release manifest exists; rollback snapshot is available |
| Candidate facts | Promotion approval decision in action audit graph linked to promotion activity, validation report, release manifest, and candidate graphs |
| SHACL/provenance gates | Candidate graph validation reports conform; approval decision has actor/time/reason; every promoted fact has source or reasoning provenance |
| Promotion behavior | Existing promotion services perform controlled graph writes and update release metadata. |
| Rollback behavior | Any write failure restores previous canonical/provenance/reasoning/operations graphs from snapshots. Approval decision remains audited with failure outcome. |
| Disabled reasons | Validation failed; missing provenance; unmanaged graph URI; missing rollback snapshot; release manifest mismatch; actor lacks future permission |

## UI Placement Strategy

V1 UI should show action affordances without claiming writeback is available.

| UI area | Action treatment |
| --- | --- |
| Workbench operational focus strip | Filter-only actions such as restore blocked, trust review, redundancy lost, capacity at risk, validation stage. These are not ontology write actions. |
| Findings table | Keep as object-set view. Rows should show recommended next action and disabled/action-needed state, not execute writes directly. |
| Selected finding Summary | Show primary operator action contracts: acknowledge blocker, assign evidence review, request validation review. Supported audit-only requests can be submitted through the private internal endpoint and remain confined to managed action-audit facts. |
| Trust tab | Place evidence review assignment and validation review actions beside the evidence they affect. |
| Dependencies tab | Read-only in v1. Do not allow relationship edits from the operator view. |
| Admin/lifecycle view | Read-only promotion batch, reasoning refresh, and reasoning approval queues are now visible in the workbench from approved semantic query IDs. Execution remains disabled and still requires a future governed request surface. |

Action buttons must display one of three states:

- Available: only after runtime, policy, and validation support exists for the audit-only action path.
- Disabled with reason: action is conceptually valid but cannot execute.
- Hidden: action does not apply to the selected object type or state.

## Runtime Boundary Strategy

The first executable step is an internal action runner and private loopback
request boundary, not a public API.

Preferred v1 runtime sequence:

```text
UI displays action contract, availability, and disabled reason
-> internal action request DTO
-> precondition validator
-> action audit candidate model
-> SHACL/provenance validation
-> managed action-audit graph write
-> optional existing promotion/reasoning service call
-> rollback on failed graph write
-> action status read model
```

Any future external or operations-affecting execution must wait for:

- authentication and authorization policy
- actor identity model
- action permission checks
- timeout/result-size policy for action readbacks
- idempotency keys
- conflict detection
- replay and rollback tests
- source-system writeback policy

## Internal Action Audit Runner v1

The internal action audit runner implements audit-only execution for:

- `AcknowledgeRestoreBlocker`
- `AssignEvidenceReview`
- `RecordValidationReview`

It accepts controlled local `.properties` action request fixtures, validates
parameters and graph preconditions against managed canonical, provenance, and
reasoning graphs, maps valid requests into RDF action-audit/provenance records,
validates the audit graph with SHACL, and writes only to
`urn:dcai:graph:action-audit:*` through `NamedGraphStore`.

The runner does not mutate canonical, reasoning, or operations graphs. It does
not expose public write endpoints, add authentication, or perform source-system
writeback. Supported React affordances can submit private internal audit-only
requests; unsupported actions remain disabled with graph-backed reasons. AI
proposal human review is implemented as a separate managed ai-audit boundary
that can hand approved action recommendations into the same action-audit graph
without mutating canonical, reasoning, operations, production, or external
source-system state.

## Action Status Read Model

The read-only action status slice is implemented through approved semantic
query IDs. These expose selected-finding action availability and internal action
audit state without enabling public execution or protected graph mutation:

| Query | Purpose |
| --- | --- |
| `semanticAvailableActionsByFinding` | Return disabled action affordances, target objects, required parameters, preconditions, provenance requirements, and disabled reasons derived from selected incident, source-record, reasoning, trust, and evidence graph facts. |
| `semanticPromotionReviewQueue` | Return read-only promotion batch review state derived from managed source, canonical, and provenance graph lifecycle facts. |
| `semanticReasoningReviewQueue` | Return read-only reasoning refresh and reasoning approval review state derived from canonical, reasoning-audit, and reasoning graph facts. |
| `semanticActionAuditHistoryByRelease` | Return action audit executions for a managed `urn:dcai:graph:action-audit:*` release. |
| `semanticActionAuditHistoryByIncident` | Return action audit executions linked to a selected incident through controlled target object links. |
| `semanticActionAuditHistoryByTarget` | Return action audit executions linked to an incident, finding, evidence record, source record, or other target object URI. |

The availability response contract emits row-level action metadata and graph
detail facts. The React selected finding view groups those rows into disabled
action cards instead of constructing action affordance metadata from frontend
defaults. The action-audit response contract includes action execution, action
request, validation report, action type, target object, idempotency key, actor,
timestamps, provenance links, and optional review/assignment fields. The React
selected finding view displays this as audit history under the governed action
affordances.

The promotion and reasoning review queue contracts include the governed action
id, disabled status, target graph/object, managed graph URIs, release/run id,
graph-backed counts, review status, evidence summary, and disabled reason. The
The React workbench displays these as internal read-only queues. They do not execute
actions, approve findings, request refreshes, or mutate graph state.

## Verification Requirements For Implementation

Any implementation goal that extends this layer must verify:

- no public endpoint exposure unless explicitly approved
- no raw SPARQL or SPARQL Update from browser/client input
- action DTO validation rejects unknown action types, unmanaged graph URIs, and
  missing required parameters
- SHACL validation fails closed
- provenance is required before any promoted state change
- failed graph writes restore last-known-good graphs
- deterministic reruns do not duplicate action executions when an idempotency
  key is reused
- UI shows disabled reasons and does not imply protected graph or external
  execution when only audit-only runtime support exists
- source scans confirm no auth/public endpoint/AI governance scope drift unless
  explicitly approved

## Explicit Non-goals

This v1 design and current implementation do not:

- implement public action execution or operations-affecting browser execution
- expose public write endpoints
- add authentication or authorization
- mutate real production data
- mutate canonical, reasoning, provenance, source, operations, or production
  graphs from action requests or AI proposal reviews
- implement AI governance writeback or AI-generated protected graph mutation
- implement source-system writeback
- replace source promotion or reasoning promotion services

## Historical Recommended Next Goal

```text
/goal Implement read-only ontology action affordance v1: add semantic action contract metadata and UI-disabled action states for selected findings, showing available action labels, required parameters, and disabled reasons without executing writes. Keep all graph writes internal-only and unimplemented, do not expose public endpoints/auth, do not mutate production data, commit, or push.
```

## Implementation Note

Read-only action affordance v1 is implemented in the selected finding Summary
and Trust views. Internal action audit runner v1 adds audit-only writes for
three controlled action contracts through CLI and private loopback request
boundaries. The UI can submit supported local audit requests and display their
audit lifecycle, while unsupported or protected actions remain disabled with
graph-backed reasons. The runner remains internal-only and does not expose
public write endpoints, add authentication, mutate canonical/reasoning/operations
graphs, perform source-system writeback, or allow browser-supplied SPARQL/SPARQL
Update. AI proposal human review is implemented separately as managed ai-audit
review plus optional governed action-audit request creation.
