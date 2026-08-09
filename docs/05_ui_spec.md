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
5. Ranked recovery table
6. Selected-case decision preview

Queue rows compare:

- operational rank and priority
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

The case header keeps the incident, priority, current stage, time blocked,
owner, exposure, restore readiness, evidence status, and recommended action
visible before tab content.

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
They require confirmation and create local audited requests only. They do not
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

- direct infrastructure dependency paths
- active incidents on related paths
- inferred downstream assets
- blast-radius incident count
- progressively disclosed reasoning findings

Topology supports the recovery decision; it is not a free-form graph editor.

## Review Inbox

The Review Inbox separates specialist decisions from recovery prioritization.
It groups:

- evidence reviews
- validation reviews
- governed-action reviews
- AI proposal reviews when present in approved review read models
- promotion and reasoning lifecycle reviews

Each review item shows the required decision, reason, operational risk,
related object, evidence completeness, requester, age when available, and an
available or explicitly disabled action.

Repeated semantic observations are collapsed only when review kind, action,
target, and release identify the same decision. Distinct targets remain
separate. Category filters and search persist in the URL, and the inbox renders
20 decisions per page so large reasoning releases do not create an unbounded
document.

## Platform Status

Platform Status reports technical health without mixing it with incident
severity:

- semantic-service connectivity
- latest pipeline state
- data-quality and trust findings
- analysis and topology coverage
- controlled graph lifecycle review state
- source and reconciliation findings

Graph URIs and release details are hidden under specialist disclosures.
Data-quality rows are deduplicated by stable finding identifier, ordered by
severity and recency, and rendered 15 per page. Graph lifecycle details show a
bounded preview and route full decision work to the Review Inbox.

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
- confirmation before governed action requests

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
