package com.dcai.semanticservice.actions

import com.dcai.semanticservice.graph.NamedGraphSnapshot
import com.dcai.semanticservice.graph.NamedGraphStore
import com.dcai.semanticservice.graph.NamedGraphWriteResult
import com.dcai.semanticservice.ingestion.Dcai
import com.dcai.semanticservice.ingestion.FileSourceExtractLoader
import com.dcai.semanticservice.ingestion.Prov
import com.dcai.semanticservice.ingestion.SourceExtractRdfMapper
import com.dcai.semanticservice.reasoning.ReasoningInput
import com.dcai.semanticservice.reasoning.ReasoningModelBuilder
import com.dcai.semanticservice.runtime.SemanticServiceApplication
import com.dcai.semanticservice.testfixtures.InMemoryNamedGraphStore
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.RDF

class OntologyActionAuditServiceTest {
    private val repoRoot = SemanticServiceApplication.locateRepoRoot()
    private val releaseId = "local-controlled-source-v1"
    private val reasoningRunId = "local-controlled-reasoning-v1"
    private val actionAuditReleaseId = "local-action-audit-v1"
    private val graphs = OntologyActionGraphUris.forRelease(
        sourceReleaseId = releaseId,
        reasoningRunId = reasoningRunId,
        actionAuditReleaseId = actionAuditReleaseId,
    )

    @Test
    fun auditsAllSupportedActionContractsToManagedActionAuditGraph() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())
        val service = service(store)

        val acknowledge = service.submit(plan(acknowledgeRequest()))
        val assign = service.submit(plan(assignEvidenceReviewRequest()))
        val validation = service.submit(plan(recordValidationReviewRequest()))

        assertTrue(acknowledge.audited, acknowledge.errors.joinToString(separator = "\n"))
        assertTrue(assign.audited, assign.errors.joinToString(separator = "\n"))
        assertTrue(validation.audited, validation.errors.joinToString(separator = "\n"))
        assertEquals(
            listOf(graphs.actionAuditGraphUri, graphs.actionAuditGraphUri, graphs.actionAuditGraphUri),
            store.writeOrder,
        )
        val auditGraph = store.graph(graphs.actionAuditGraphUri)!!
        assertEquals(3, auditGraph.countType(Dcai.OntologyActionExecution))
        assertEquals(3, auditGraph.countType(Dcai.OntologyActionNotification))
        assertEquals(9, auditGraph.countType(Dcai.OntologyActionStateTransition))
        assertTrue(auditGraph.contains(null, Dcai.hasActionReason, acknowledgeRequest().actionReason))
        assertTrue(auditGraph.contains(null, Dcai.hasNotificationStatus, OntologyActionLifecycleState.QUEUED.id))
        assertTrue(auditGraph.contains(null, Dcai.hasToActionState, OntologyActionLifecycleState.REQUESTED.id))
        assertTrue(auditGraph.contains(null, Dcai.hasToActionState, OntologyActionLifecycleState.VALIDATED.id))
        assertTrue(auditGraph.contains(null, Dcai.hasToActionState, OntologyActionLifecycleState.QUEUED.id))
    }

    @Test
    fun rejectsInvalidRequestBeforeWritingAuditGraph() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())
        val request = assignEvidenceReviewRequest().copy(assignedTeam = "uncontrolled team value")

        val result = service(store).submit(plan(request))

        assertFalse(result.audited)
        assertTrue(result.errors.any { it.contains("assignedTeam must use") })
        assertNull(store.graph(graphs.actionAuditGraphUri))
    }

    @Test
    fun rejectsMissingSourceRecordProvenanceBeforeWritingAuditGraph() {
        val inputGraphs = inputGraphs().toMutableMap()
        inputGraphs[graphs.provenanceGraphUri] = ModelFactory.createDefaultModel()
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs)

        val result = service(store).submit(plan(acknowledgeRequest()))

        assertFalse(result.audited)
        assertTrue(result.errors.any { it.contains("Source record provenance is missing") })
        assertNull(store.graph(graphs.actionAuditGraphUri))
    }

    @Test
    fun deterministicRerunIsIdempotentAndDoesNotDuplicateExecution() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())
        val service = service(store)
        val request = acknowledgeRequest()

        val first = service.submit(plan(request))
        val second = service.submit(plan(request))

        assertTrue(first.audited, first.errors.joinToString(separator = "\n"))
        assertTrue(second.audited, second.errors.joinToString(separator = "\n"))
        assertTrue(second.idempotentReplay)
        assertEquals(1, store.graph(graphs.actionAuditGraphUri)!!.countType(Dcai.OntologyActionExecution))
        assertEquals(1, store.graph(graphs.actionAuditGraphUri)!!.countType(Dcai.OntologyActionNotification))
        assertEquals(listOf(graphs.actionAuditGraphUri), store.writeOrder)
    }

    @Test
    fun restoresPreviousAuditGraphWhenWriteFailsAfterPartialMutation() {
        val previousAudit = markerModel("previous-action-audit")
        val store = PartiallyFailingActionAuditStore(
            initialGraphs = inputGraphs() + mapOf(graphs.actionAuditGraphUri to previousAudit),
            failGraphUri = graphs.actionAuditGraphUri,
        )

        val result = service(store).submit(plan(assignEvidenceReviewRequest()))

        assertFalse(result.audited)
        assertTrue(result.rollbackAttempted)
        assertTrue(result.rollbackSucceeded, result.errors.joinToString(separator = "\n"))
        assertTrue(store.graph(graphs.actionAuditGraphUri)!!.isIsomorphicWith(previousAudit))
    }

    @Test
    fun rejectsUnmanagedActionAuditGraphUri() {
        assertFailsWith<IllegalArgumentException> {
            OntologyActionGraphUris(
                canonicalGraphUri = graphs.canonicalGraphUri,
                provenanceGraphUri = graphs.provenanceGraphUri,
                reasoningGraphUri = graphs.reasoningGraphUri,
                actionAuditGraphUri = "urn:dcai:graph:canonical:not-action-audit",
            )
        }
    }

    @Test
    fun inspectsActionAuditHistory() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())
        val service = service(store)
        service.submit(plan(acknowledgeRequest()))
        service.submit(plan(assignEvidenceReviewRequest()))

        val result = OntologyActionAuditInspector(store).inspect(
            OntologyActionAuditInspectionPlan(actionAuditReleaseId),
        )

        assertTrue(result.inspected, result.errors.joinToString(separator = "\n"))
        assertTrue(result.exists)
        assertEquals(2, result.executionCount)
        assertEquals(2, result.requestCount)
        assertEquals(2, result.validationReportCount)
        assertEquals(2, result.notificationCount)
        assertEquals(8, result.idempotencyKeyCount)
        assertEquals(1, result.actionTypeCounts.getValue("AcknowledgeRestoreBlocker"))
        assertEquals(1, result.actionTypeCounts.getValue("AssignEvidenceReview"))
    }

    private fun service(store: NamedGraphStore): OntologyActionAuditService {
        return OntologyActionAuditService(
            mapper = OntologyActionRdfMapper(),
            preconditionValidator = OntologyActionPreconditionValidator(),
            validationGate = OntologyActionValidationGate(repoRoot),
            graphStore = store,
        )
    }

    private fun plan(request: OntologyActionRequest): OntologyActionAuditPlan {
        return OntologyActionAuditPlan(request = request, graphs = graphs)
    }

    private fun inputGraphs(): Map<String, Model> {
        val mapping = SourceExtractRdfMapper().map(
            FileSourceExtractLoader().load(repoRoot.resolve("fixtures/source-extracts/local-controlled-source-v1.properties")),
        )
        val canonical = ModelFactory.createDefaultModel().add(mapping.canonicalModel).apply {
            val validationEvidence = ResourceFactory.createResource(VALIDATION_EVIDENCE_URI)
            add(validationEvidence, RDF.type, Dcai.ValidationEvidence)
            add(validationEvidence, Dcai.hasValidationId, "VAL-LOCAL-001")
            add(validationEvidence, Dcai.hasValidationStatus, "FAILED")
            add(validationEvidence, Dcai.supportsFact, ResourceFactory.createResource(INCIDENT_URI))
            add(validationEvidence, Prov.wasDerivedFrom, ResourceFactory.createResource(SOURCE_RECORD_URI))
        }
        val reasoning = ReasoningModelBuilder().build(
            ReasoningInput(
                runId = reasoningRunId,
                generatedAt = Instant.parse("2026-06-09T01:00:00Z"),
                canonicalModel = mapping.canonicalModel,
                provenanceModel = mapping.provenanceModel,
            ),
        ).reasoningModel.apply {
            val trust = ResourceFactory.createResource(TRUST_FINDING_URI)
            val activity = ResourceFactory.createResource("urn:dcai:reasoning-activity:$reasoningRunId")
            add(trust, RDF.type, Dcai.TrustFinding)
            add(trust, Dcai.hasIdentifier, "trust-local-001")
            add(trust, Dcai.hasFindingSummary, "Local evidence requires review.")
            add(trust, Prov.wasDerivedFrom, ResourceFactory.createResource(SOURCE_RECORD_URI))
            add(trust, Prov.wasGeneratedBy, activity)
        }
        return mapOf(
            graphs.canonicalGraphUri to canonical,
            graphs.provenanceGraphUri to mapping.provenanceModel,
            graphs.reasoningGraphUri!! to reasoning,
        )
    }

    private fun acknowledgeRequest(): OntologyActionRequest {
        return OntologyActionRequest(
            requestId = "ACT-REQ-ACK-LOCAL-001",
            actionType = OntologyActionType.ACKNOWLEDGE_RESTORE_BLOCKER,
            idempotencyKey = "$actionAuditReleaseId:acknowledge:INC-001",
            actorId = "operator-local-reviewer",
            requestedAt = Instant.parse("2026-06-09T02:00:00Z"),
            incidentUri = INCIDENT_URI,
            actionReason = "Operator reviewed the restore blocker.",
            sourceRecordUri = SOURCE_RECORD_URI,
            restoreReadinessFindingUri = RESTORE_READINESS_URI,
        )
    }

    private fun assignEvidenceReviewRequest(): OntologyActionRequest {
        return OntologyActionRequest(
            requestId = "ACT-REQ-ASSIGN-LOCAL-001",
            actionType = OntologyActionType.ASSIGN_EVIDENCE_REVIEW,
            idempotencyKey = "$actionAuditReleaseId:assign-evidence:INC-001",
            actorId = "operator-local-reviewer",
            requestedAt = Instant.parse("2026-06-09T02:05:00Z"),
            incidentUri = INCIDENT_URI,
            actionReason = "Assign local trust review.",
            sourceRecordUri = SOURCE_RECORD_URI,
            trustFindingUri = TRUST_FINDING_URI,
            assignedTeam = "OPS_VALIDATION",
            assigneeId = "engineer-001",
        )
    }

    private fun recordValidationReviewRequest(): OntologyActionRequest {
        return OntologyActionRequest(
            requestId = "ACT-REQ-VALIDATION-LOCAL-001",
            actionType = OntologyActionType.RECORD_VALIDATION_REVIEW,
            idempotencyKey = "$actionAuditReleaseId:validation-review:INC-001",
            actorId = "operator-local-reviewer",
            requestedAt = Instant.parse("2026-06-09T02:10:00Z"),
            incidentUri = INCIDENT_URI,
            actionReason = "Record local validation review.",
            sourceRecordUri = SOURCE_RECORD_URI,
            validationEvidenceUri = VALIDATION_EVIDENCE_URI,
            reviewedStatus = "NEEDS_REVIEW",
            reviewSummary = "Validation evidence conflicts with restore readiness.",
        )
    }

    private fun Model.countType(type: org.apache.jena.rdf.model.Resource): Int {
        return listSubjectsWithProperty(RDF.type, type).toList().distinct().size
    }

    private fun markerModel(label: String): Model {
        return ModelFactory.createDefaultModel().apply {
            add(
                ResourceFactory.createResource("urn:dcai:test:$label"),
                ResourceFactory.createProperty("urn:dcai:test:marker"),
                label,
            )
        }
    }

    private class PartiallyFailingActionAuditStore(
        initialGraphs: Map<String, Model>,
        private val failGraphUri: String,
    ) : NamedGraphStore {
        private val graphs = initialGraphs.mapValuesTo(mutableMapOf()) { (_, model) ->
            ModelFactory.createDefaultModel().add(model)
        }
        private var shouldFail = true

        fun graph(graphUri: String): Model? = graphs[graphUri]

        override fun readNamedGraph(graphUri: String): NamedGraphSnapshot {
            val model = graphs[graphUri]
            return NamedGraphSnapshot(
                graphUri = graphUri,
                exists = model != null,
                model = model?.let { ModelFactory.createDefaultModel().add(it) } ?: ModelFactory.createDefaultModel(),
            )
        }

        override fun replaceNamedGraph(graphUri: String, model: Model): NamedGraphWriteResult {
            graphs[graphUri] = ModelFactory.createDefaultModel().add(model)
            if (graphUri == failGraphUri && shouldFail) {
                shouldFail = false
                error("simulated partial write failure for $graphUri")
            }
            return NamedGraphWriteResult(graphUri = graphUri, tripleCount = model.size().toInt(), statusCode = 200)
        }

        override fun deleteNamedGraph(graphUri: String): NamedGraphWriteResult {
            graphs.remove(graphUri)
            return NamedGraphWriteResult(graphUri = graphUri, tripleCount = 0, statusCode = 204)
        }
    }

    private companion object {
        private const val INCIDENT_URI = "urn:dcai:incident:INC-001"
        private const val SOURCE_RECORD_URI = "urn:dcai:source-record:local-controlled-facility-ops-file:SRC-INC-001"
        private const val RESTORE_READINESS_URI = "urn:dcai:reasoning:restore-readiness:urn%3Adcai%3Aincident%3AINC-001"
        private const val TRUST_FINDING_URI = "urn:dcai:reasoning:trust:local-test"
        private const val VALIDATION_EVIDENCE_URI = "urn:dcai:evidence:EVIDENCE-VALIDATION-001"
    }
}
