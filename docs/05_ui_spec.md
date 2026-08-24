# UI Specification

## Product Position

The frontend is a queue-first **Return-to-Service Operations Console** for AI
data-center infrastructure incidents. It helps an operator identify the next
recovery intervention, understand operational exposure, see who or what owns
the blocker, and decide whether the supporting evidence is trustworthy.

Semantic and ontology capabilities remain part of the product, but they are
supporting evidence. Operational language and decisions appear before graph,
SHACL, provenance, or ontology terminology.

## Users and Workspaces

| User | Primary decision | Workspace |
| --- | --- | --- |
| Shift lead | Which recovery case needs intervention next? | Recovery Queue |
| Facilities supervisor | What will move this case toward restoration? | Recovery Case |
| Reliability or evidence reviewer | Can the evidence or governed action be approved? | Review Inbox |
| Data or platform engineer | Is the semantic operations layer healthy and current? | Platform Status |

Primary navigation:

- **Recovery Queue** at `/`
- **Recovery Case** at `/recovery-cases/{incident_id}`
- **Review Inbox** at `/reviews`
- **Platform Status** at `/platform-status`

The legacy `/findings/{incident_id}` route remains a supported compatibility
alias for existing links.

## Recovery Queue

The queue begins in the first viewport. It is the primary decision surface, not
a dashboard summary placed below product or platform information.

Content order:

1. Compact page identity, data freshness, and refresh control
2. Critical recovery signals
3. Search, sorting, and optional filters
4. Applied-filter context and visible result count
5. Recovery table ordered by available read-model fields
6. Selected-case decision preview

Queue rows compare:

- optional controlled-fixture rank and priority
- incident, asset, and zone
- active recovery blocker
- time in the current stage
- affected GPU and power capacity
- redundancy exposure
- owner or external dependency
- evidence status
- explicit link to the recovery case

Selecting a row does not navigate. It updates the decision preview. Opening a
case requires the explicit `Open recovery case` action or the row action link.
Search and filters persist in the URL.

The queue does not contain ontology lifecycle review queues, promotion reviews,
AI proposal administration, or detailed platform health. Those concerns have
dedicated workspaces.

## Recovery Case

The core case read model is loaded first. Workflow timeline, evidence/trust,
impact reasoning, governed actions/audit, AI governance, dynamic playback, and
topology are independent resources with explicit partial-failure handling. A
resource refresh must not repeat queue, detail, or incident-evidence queries.

The case header keeps the incident, source-provided current stage, time blocked,
owner, exposure, restore readiness, and evidence status visible before tab
content. Queue score, rank, or recommended-action fields are displayed only
when supplied by the controlled read model; the current reasoner does not derive
them.

Tabs are URL-addressable through `?tab=` and use the following hierarchy:

### Overview

- concise operational brief
- recommended decision
- blocker, exposure, redundancy, and evidence summary
- recovery-stage timeline with thresholds
- current work-order ownership
- latest activity

### Recovery & Actions

- complete recovery timeline
- approved governed-action affordances
- disabled-action explanations
- work-order and spare/vendor context
- operational event history
- progressively disclosed audit and transition history

Supported action requests retain the existing private semantic action contract.
They require editable actor, reason, and action-specific inputs before they
create local audited requests. They do not
mutate canonical, reasoning, operations, or external source-system state.

### Impact

- affected GPUs, racks, and kW
- GPU capacity percentage
- power and cooling redundancy
- thermal exposure
- vendor ETA and mitigation state
- attached telemetry evidence
- progressively disclosed priority-score inputs

### Evidence

- evidence verdict in operational language
- impact confidence and source quality
- validation records
- incident-to-asset evidence status
- impact evidence issues
- progressively disclosed SHACL, provenance, direct-fact, and inferred-fact
  details for specialist review

### Dependencies

- direct, one-edge infrastructure dependency paths
- active incidents on related paths
- inferred downstream assets
- blast-radius incident count
- progressively disclosed reasoning findings

Topology supports the recovery decision; it is not a free-form graph editor.

## Review Inbox

The Review Inbox separates specialist decisions from recovery prioritization.
It separates authoritative queues into:

- governed-action reviews
- AI proposal reviews from the approved global proposal queue
- read-only promotion lifecycle reviews
- read-only reasoning lifecycle reviews
- case-attention signals derived from current evidence or validation state;
  these navigate to the case and are not persisted review records

Each authoritative review item shows only source-backed actor, state, target,
reason, and timestamp fields. Governed actions support only valid lifecycle
transitions. AI proposals support approve or reject with an editable reviewer
and reason. Promotion and reasoning controls remain disabled and explain that
the current service exposes no write contract.

Repeated semantic observations are collapsed at the semantic read-model
boundary only when stable decision identity matches; distinct targets remain
separate. Category, page, and committed search state persist in the URL. The
service returns 20 decisions per page plus `pageInfo`, so large releases do not
create an unbounded document or a misleading client-side total.

## Platform Status

Platform Status reports technical health without mixing it with incident
severity:

- semantic-service connectivity at the successful query boundary
- source-backed tri-state platform verdict (`Operational`, `Degraded`, or
  `Unknown`)
- latest source import, canonical promotion, and reasoning-run state
- data-quality and trust findings
- analysis and topology coverage
- controlled graph lifecycle review state
- source and reconciliation findings

Graph URIs and release details are hidden under specialist disclosures.
Data-quality rows are deduplicated by stable finding identifier at the
read-model boundary and rendered from service-owned pages of 15. Platform
Status never infers graph-validation success: it renders `Unknown` when no
authoritative report is persisted. Graph lifecycle details show a bounded
preview and route full decision work to the Review Inbox.

## Terminology

Use operational labels first:

- `Recovery case`, not `semantic finding`
- `Evidence`, not `trust graph`
- `Restore blocked`, not raw readiness enum values
- `Evidence trusted`, `Evidence review`, or `Evidence unverified`
- `Recovery stage`, not workflow URI
- `Owner / dependency`, not backend relation names

Technical names may appear only in advanced evidence, audit, or platform-health
details where specialist users need them.

## Visual System

- neutral gray-blue application canvas
- white operational surfaces
- restrained teal interaction accent
- red only for active critical danger
- amber for intervention or review
- green for restored, validated, trusted, or healthy
- blue for informational and selected state
- gray for unavailable, unknown, or inactive state
- tabular numerals for rank, time, capacity, GPU counts, and timestamps
- structured rows and tables before nested cards
- status communication never depends on color alone

The shell uses a persistent navigation rail on wide screens and a dismissible
navigation drawer on smaller screens. Recovery tables become structured mobile
records without changing field order or meaning.

## Interaction and Accessibility

- keyboard-selectable queue rows
- recovery-case tabs use roving focus, arrow-key wrapping, Home/End navigation,
  and explicit tab-to-panel relationships
- visible focus indicators
- URL-persisted search, filters, sort, case tabs, and deep links
- browser back, forward, reload, and new-tab support
- non-color icons and text for every status
- descriptive action and icon-button labels
- a skip-to-content link
- responsive layouts at large desktop, standard desktop, laptop, and mobile
- meaningful loading, empty, error, stale, and partial-data states
- explicit editable governed-action forms before audited requests
- search is committed on Enter or blur rather than writing browser history per
  keystroke

## Acceptance Criteria

The interface is accepted when:

- the recovery queue is visible in the first viewport;
- an operator can identify the top case, blocker, exposure, owner/dependency,
  evidence caveat, and next action within approximately ten seconds;
- specialist review and platform-health content no longer interrupts recovery
  triage;
- every screen has one clear primary task;
- technical semantic evidence remains discoverable without dominating default
  views;
- existing semantic query and governed-action contracts remain unchanged; and
- the primary workflow is understandable without knowledge of RDF, SHACL,
  SPARQL, or ontology internals.
