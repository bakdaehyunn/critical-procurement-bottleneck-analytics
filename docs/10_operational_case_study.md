# Verified Synthetic Scenario Walkthroughs

These are deterministic portfolio fixtures, not customer incidents. The public
scenario authority is the MVP export with seed `20260610` under
`fixtures/source-extracts/generated-scenarios/mvp-seed-20260610/`.

All three scenarios use the same implemented path:

```text
recorded CSV rows
  -> RecordedSourceConnectorSimulationLoader
  -> SourceExtractRdfMapper
  -> ProductionGraphValidationGate (SHACL + provenance)
  -> ReasoningModelBuilder / ReasoningValidationGate
  -> approved semantic read models
  -> /recovery-cases/{incidentId}
  -> backend-authorized local audit action, or disabled control
```

Validation is a batch graph gate. “Conforms” below means the accepted generated
batch maps and passes the canonical promotion gate; it is not a claim that each
source value is operationally true.

In the excerpts below, `#` lines identify the CSV file; every data line is
copied verbatim from the committed MVP export.

## 1. Missing final validation and unresolved signoff

Identifiers: `SCN-20260611`, `INC-GEN-SCN-20260611`.

Actual source rows:

```csv
# incidents.csv
INC-GEN-SCN-20260611,ASSET-GEN-GPU-SCN-20260611,STAGE-RECOVERY,Recovery
# validation_results.csv
VAL-GEN-SCN-20260611-SECONDARY,INC-GEN-SCN-20260611,missing-final-validation,2026-06-10T00:50:00Z,REVIEW_REQUIRED
# work_orders.csv
WO-GEN-SCN-20260611-SRE,INC-GEN-SCN-20260611,awaiting-signoff,Site Reliability,2026-06-10T00:40:00Z,REVIEW_REQUIRED
# workflow_events.csv
WF-GEN-SCN-20260611-VALIDATION,INC-GEN-SCN-20260611,STAGE-VALIDATION,Validation,blocked,2026-06-10T00:10:00Z,,1.0,1.5
```

The mapper emits `dcai:InfrastructureIncident` with source-provided
`dcai:hasCurrentStage`, `dcai:ValidationEvidence`, `dcai:WorkOrderEvidence`, and
`prov:wasDerivedFrom` links to source-record IRIs. The source-record graph stores
IDs and payload hashes; the CSV remains the full recorded artifact.

After the accepted batch conforms, the reasoner emits a
`dcai:RestoreReadinessFinding`, a `dcai:RecoveryBlocker`, and trust findings for
review-required evidence. These are deterministic in the same run ID/time; the
reasoner does not reconstruct current stage from the workflow rows.

The case is read through `semanticFollowUpDetail`,
`semanticIncidentEvidence`, and `semanticAvailableActionsByFinding` at
`/recovery-cases/INC-GEN-SCN-20260611`. When both reasoning targets are present,
the backend returns `AcknowledgeRestoreBlocker` as
`AVAILABLE_FOR_LOCAL_AUDIT`. Submission records an acknowledgement request and
audit provenance only; it does not clear the blocker or sign off external work.

Verified action-query result facts:

```text
incidentId=INC-GEN-SCN-20260611
actionId=AcknowledgeRestoreBlocker
actionStatus=AVAILABLE_FOR_LOCAL_AUDIT
```

## 2. Direct upstream cooling dependency

Identifiers: `SCN-20260613`, `INC-GEN-SCN-20260613`.

Actual source rows:

```csv
# incidents.csv
INC-GEN-SCN-20260613,ASSET-GEN-GPU-SCN-20260613,STAGE-RECOVERY,Recovery
# assets.csv
ASSET-GEN-CHW-SCN-20260613,ZONE-GEN-COOLING,Chilled Water Loop,high,degraded,HALL-GEN-003,,,,COOLING
# dependencies.csv
DEP-GEN-SCN-20260613-COOLING,ASSET-GEN-GPU-SCN-20260613,ASSET-GEN-CHW-SCN-20260613,cooling-loop,row,PATH-GEN-SCN-20260613-COOLING,COOLING
# telemetry_impacts.csv
IMPACT-GEN-SCN-20260613-CAPACITY,TEL-GEN-SCN-20260613-CAPACITY,INC-GEN-SCN-20260613,2026-06-10T00:35:00Z,280.0,72,3,N_PLUS_0,thermal-throttle-active,vendor-engaged,2026-06-10T02:20:00Z,chw-supply-temp-c,19.5,celsius,alerting,TRUSTED
```

The mapper creates the GPU and chilled-water resources, one dependency edge,
one cooling path, the incident-to-GPU relation, impact facts, and provenance.
After batch conformance, the reasoner follows that single explicit edge and
emits a `dcai:DependencyImpactFinding` derived from the incident and path. This
proves direct GPU-to-upstream-cooling exposure only; it is not recursive
facility traversal.

The UI consumes `semanticTopologyDependencies` and
`semanticDependencyImpactByAsset` at
`/recovery-cases/INC-GEN-SCN-20260613?tab=dependencies`. Dependency exposure is
explanatory evidence, not an executable recommendation. This fixture also
produces restore-readiness and recovery-blocker targets, so the action query
returns `AcknowledgeRestoreBlocker` as `AVAILABLE_FOR_LOCAL_AUDIT`; there is no
cooling-control action and no external infrastructure mutation.

Source-backed dependency response facts are:

```text
assetId=ASSET-GEN-GPU-SCN-20260613
dependencyId=DEP-GEN-SCN-20260613-COOLING
dependencyAssetId=ASSET-GEN-CHW-SCN-20260613
dependencyRole=cooling-loop
impactScope=row
```

The tested action-query result is
`AcknowledgeRestoreBlocker / AVAILABLE_FOR_LOCAL_AUDIT` for this incident.

## 3. Conflicting validation requiring evidence review

Identifiers: `SCN-20260616`, `INC-GEN-SCN-20260616`.

Actual source rows:

```csv
# incidents.csv
INC-GEN-SCN-20260616,ASSET-GEN-GPU-SCN-20260616,STAGE-VALIDATION,Validation
# validation_results.csv
VAL-GEN-SCN-20260616-PRIMARY,INC-GEN-SCN-20260616,primary-validation-pass,2026-06-10T01:35:00Z,TRUSTED
VAL-GEN-SCN-20260616-SECONDARY,INC-GEN-SCN-20260616,secondary-validation-conflict,2026-06-10T01:40:00Z,REVIEW_REQUIRED
# telemetry_impacts.csv
IMPACT-GEN-SCN-20260616-WORKFLOW,TEL-GEN-SCN-20260616-WORKFLOW,INC-GEN-SCN-20260616,2026-06-10T01:15:00Z,140.0,32,2,N_PLUS_1,evidence-review,monitoring,,manual-check-disagree,1.0,boolean,conflict,REVIEW_REQUIRED
```

The mapper emits both validation evidence resources without overwriting one
with the other. After batch conformance, `ReasoningModelBuilder` emits a
`dcai:TrustFinding` for the review-required validation status, with derivation
to the specific evidence resource.

The UI reads `semanticIncidentEvidence`, `semanticTrustFindingList`, and
`semanticAvailableActionsByFinding` at
`/recovery-cases/INC-GEN-SCN-20260616?tab=evidence`. The backend returns
`AssignEvidenceReview` for a trust-finding target and
`RecordValidationReview` for a validation-evidence target as
`AVAILABLE_FOR_LOCAL_AUDIT`. Those actions append local review/audit facts; they
do not replace the two source validation records or mark the real incident
restored.

Verified action-query result facts:

```text
incidentId=INC-GEN-SCN-20260616
actionId=AssignEvidenceReview; actionStatus=AVAILABLE_FOR_LOCAL_AUDIT
actionId=RecordValidationReview; actionStatus=AVAILABLE_FOR_LOCAL_AUDIT
actionId=RequestReasoningRefresh; actionStatus=DISABLED
```

## Reproducibility evidence

`RecordedSourceScenarioGeneratorTest` checks deterministic generated files,
stable source IDs and payload hashes, accepted/rejected row counts, promotion,
reasoning promotion, and lifecycle counts. `ReasoningModelBuilderTest` checks
isomorphic output for identical inputs. Action-status agreement is checked by
the SPARQL contract, Kotlin shaping/serialization tests, and frontend action
utility tests.

Queue priority fields, when present, are controlled fixture facts consumed by
the read model. `ReasoningModelBuilder` does not derive queue rank, score, or
recommended-action facts, so no scenario claims that it does.
