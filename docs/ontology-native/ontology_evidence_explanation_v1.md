# Ontology Evidence Explanation v1

This document defines the selected-finding explanation model used by the
React semantic workbench. It is a UI/read-model hardening pass, not a new
runtime layer.

## Purpose

The selected finding view should answer one operator question:

> Why is this incident actionable, and which graph facts prove it?

The answer is intentionally presented as an ontology evidence chain instead of
a legacy ticket detail page.

## Evidence Groups

| Group | Meaning | Source |
| --- | --- | --- |
| Direct graph facts | Canonical RDF assertions mapped from controlled source exports. | `semanticFollowUpDetail`, `semanticIncidentTimeline`, `semanticIncidentEvidence`, `semanticDependencyImpactByAsset` |
| Inferred graph facts | Reasoning output used for restore readiness, recovery blocker, evidence trust, dependency exposure, and blast radius. | `semanticFollowUpDetail`, `semanticIncidentEvidence`, `semanticDependencyImpactByAsset`, `semanticBlastRadiusByAsset` |
| Provenance and gates | Source lineage plus governed ontology action eligibility. | `semanticIncidentEvidence`, `semanticAvailableActionsByFinding` |

## UI Contract

The frontend builds `OntologyEvidenceExplanation` for every selected finding.
It includes:

- a concise answer sentence for why the finding is actionable
- direct facts such as incident-to-asset, workflow state, and impact observation
- inferred facts such as restore readiness, recovery blockers, trust findings,
  dependency exposure, and blast-radius findings
- provenance links from source record to canonical and reasoning resources
- action eligibility facts from governed action affordances and disabled reasons

The UI renders this as:

1. `Direct graph facts`
2. `Inferred graph facts`
3. `Provenance and gates`

The Dependencies tab also shows the reasoning finding URIs and summaries behind
dependency exposure and blast-radius counts.

## Guardrails

- No raw SPARQL is exposed.
- No public endpoint is added.
- No auth, connector, or external writeback behavior is added.
- All data still comes through approved semantic query IDs and the existing
  private semantic endpoint.
- The UI remains an internal semantic workbench; this pass does not redesign
  the overall application shell.

## Remaining Work

- Split frontend semantic API mapping by domain once behavior is stable.
- Add query catalog ownership/version metadata for evidence explanation
  consumers.
- Broaden dependency path visualization only after the approved query catalog
  can return path ordering and edge provenance directly.
