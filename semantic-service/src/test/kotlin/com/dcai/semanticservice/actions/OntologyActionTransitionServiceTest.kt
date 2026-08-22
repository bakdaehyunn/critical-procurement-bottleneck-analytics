package com.dcai.semanticservice.actions

import com.dcai.semanticservice.graph.NamedGraphSnapshot
import com.dcai.semanticservice.graph.NamedGraphStore
import com.dcai.semanticservice.graph.NamedGraphWriteResult
import com.dcai.semanticservice.ontology.Dcai
import com.dcai.semanticservice.ingestion.FileSourceExtractLoader
import com.dcai.semanticservice.ontology.Prov
import com.dcai.semanticservice.ingestion.SourceExtractRdfMapper
import com.dcai.semanticservice.reasoning.ReasoningInput
import com.dcai.semanticservice.reasoning.ReasoningModelBuilder
import com.dcai.semanticservice.runtime.SemanticServiceComposition
import com.dcai.semanticservice.testfixtures.InMemoryNamedGraphStore
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.RDF

class OntologyActionTransitionServiceTest {
    private val repoRoot = SemanticServiceComposition.locateRepoRoot()
    private val releaseId = "local-controlled-source-v1"
    private val reasoningRunId = "local-controlled-reasoning-v1"
    private val actionAuditReleaseId = "local-action-audit-v1"
    private val graphs = OntologyActionGraphUris.forRelease(
        sourceReleaseId = releaseId,
        reasoningRunId = reasoningRunId,
        actionAuditReleaseId = actionAuditReleaseId,
    )

    @Test
    fun transitionsQueuedActionToReviewInManagedActionAuditGraphOnly() {
        val store = storeWithQueuedAction()
        val execution = actionExecution(store)
        val service = transitionService(store)

        val result = service.submit(transitionPlan(execution, OntologyActionLifecycleState.IN_REVIEW, "review-start-001"))

        assertTrue(result.transitioned, result.errors.joinToString(separator = "\n"))
        assertEquals(OntologyActionLifecycleState.IN_REVIEW, result.currentState)
        assertEquals(listOf(graphs.actionAuditGraphUri, graphs.actionAuditGraphUri), store.writeOrder)
        val graph = store.graph(graphs.actionAuditGraphUri)!!
        assertTrue(graph.contains(execution, Dcai.hasActionStatus, OntologyActionLifecycleState.IN_REVIEW.id))
        assertTrue(graph.contains(null, Dcai.hasNotificationStatus, OntologyActionLifecycleState.IN_REVIEW.id))
        assertTrue(graph.contains(null, Dcai.hasToActionState, OntologyActionLifecycleState.IN_REVIEW.id))
        assertEquals(4, graph.countType(Dcai.OntologyActionStateTransition))
    }

    @Test
    fun rejectsInvalidTransitionWithoutWritingGraph() {
        val store = storeWithQueuedAction()
        val execution = actionExecution(store)

        val result = transitionService(store).submit(
            transitionPlan(execution, OntologyActionLifecycleState.CLOSED, "invalid-close-001"),
        )

        assertFalse(result.transitioned)
        assertTrue(result.errors.any { it.contains("Invalid ontology action lifecycle transition") })
        assertEquals(listOf(graphs.actionAuditGraphUri), store.writeOrder)
        assertTrue(store.graph(graphs.actionAuditGraphUri)!!.contains(execution, Dcai.hasActionStatus, OntologyActionLifecycleState.QUEUED.id))
    }

    @Test
    fun duplicateTransitionIdempotencyKeyDoesNotDuplicateStateTransition() {
        val store = storeWithQueuedAction()
        val execution = actionExecution(store)
        val service = transitionService(store)
        val plan = transitionPlan(execution, OntologyActionLifecycleState.IN_REVIEW, "review-start-duplicate")

        val first = service.submit(plan)
        val second = service.submit(plan)

        assertTrue(first.transitioned, first.errors.joinToString(separator = "\n"))
        assertTrue(second.transitioned, second.errors.joinToString(separator = "\n"))
        assertTrue(second.idempotentReplay)
        assertEquals(OntologyActionLifecycleState.IN_REVIEW, second.currentState)
        assertEquals(4, store.graph(graphs.actionAuditGraphUri)!!.countType(Dcai.OntologyActionStateTransition))
    }

    @Test
    fun approvedTransitionCreatesSimulatedDispatchRecordsWithProvenance() {
        val store = storeWithQueuedAction()
        val execution = actionExecution(store)
        val service = transitionService(store)

        val review = service.submit(transitionPlan(execution, OntologyActionLifecycleState.IN_REVIEW, "dispatch-review-start"))
        val approved = service.submit(transitionPlan(execution, OntologyActionLifecycleState.APPROVED, "dispatch-approved"))

        assertTrue(review.transitioned, review.errors.joinToString(separator = "\n"))
        assertTrue(approved.transitioned, approved.errors.joinToString(separator = "\n"))
        assertEquals(OntologyActionLifecycleState.APPROVED, approved.currentState)

        val graph = store.graph(graphs.actionAuditGraphUri)!!
        val dispatches = graph.listSubjectsWithProperty(RDF.type, Dcai.OntologyActionDispatch).toList().distinct()
        val approvalTransition = graph.listSubjectsWithProperty(Dcai.hasIdentifier, "ACT-TRN-dispatch-approved").toList().single()

        assertEquals(3, dispatches.size)
        assertEquals(
            setOf("NOC_QUEUE", "WORK_ORDER_QUEUE", "VALIDATION_REVIEW_QUEUE"),
            dispatches.map { graph.requiredLiteral(it, Dcai.hasDispatchChannel) }.toSet(),
        )
        assertEquals(setOf("SIMULATED_QUEUED"), dispatches.map { graph.requiredLiteral(it, Dcai.hasDispatchStatus) }.toSet())
        assertEquals(setOf(OntologyActionLifecycleState.APPROVED.id), dispatches.map { graph.requiredLiteral(it, Dcai.hasDispatchLifecycleState) }.toSet())
        assertTrue(dispatches.all { dispatch -> graph.contains(dispatch, Prov.wasGeneratedBy, approvalTransition) })
        assertTrue(dispatches.all { dispatch -> graph.contains(dispatch, Prov.used, execution) })
        assertTrue(dispatches.all { dispatch -> graph.contains(dispatch, Dcai.hasTargetObject, execution) })
    }

    @Test
    fun duplicateApprovedTransitionDoesNotDuplicateDispatchRecords() {
        val store = storeWithQueuedAction()
        val execution = actionExecution(store)
        val service = transitionService(store)
        val review = service.submit(transitionPlan(execution, OntologyActionLifecycleState.IN_REVIEW, "duplicate-dispatch-review-start"))
        val approvalPlan = transitionPlan(execution, OntologyActionLifecycleState.APPROVED, "duplicate-dispatch-approved")

        val first = service.submit(approvalPlan)
        val second = service.submit(approvalPlan)

        assertTrue(review.transitioned, review.errors.joinToString(separator = "\n"))
        assertTrue(first.transitioned, first.errors.joinToString(separator = "\n"))
        assertTrue(second.transitioned, second.errors.joinToString(separator = "\n"))
        assertTrue(second.idempotentReplay)
        assertEquals(3, store.graph(graphs.actionAuditGraphUri)!!.countType(Dcai.OntologyActionDispatch))
    }

    @Test
    fun restoresPreviousActionAuditGraphWhenTransitionWriteFailsAfterPartialMutation() {
        val baseStore = storeWithQueuedAction()
        val previousAudit = baseStore.graph(graphs.actionAuditGraphUri)!!
        val store = PartiallyFailingActionAuditStore(
            initialGraphs = inputGraphs() + mapOf(graphs.actionAuditGraphUri to previousAudit),
            failGraphUri = graphs.actionAuditGraphUri,
        )
        val execution = actionExecution(store)

        val result = transitionService(store).submit(
            transitionPlan(execution, OntologyActionLifecycleState.IN_REVIEW, "rollback-review-start"),
        )

        assertFalse(result.transitioned)
        assertTrue(result.rollbackAttempted)
        assertTrue(result.rollbackSucceeded, result.errors.joinToString(separator = "\n"))
        assertTrue(store.graph(graphs.actionAuditGraphUri)!!.isIsomorphicWith(previousAudit))
    }

    private fun storeWithQueuedAction(): InMemoryNamedGraphStore {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())
        val auditResult = OntologyActionAuditService(
            mapper = OntologyActionRdfMapper(),
            preconditionValidator = OntologyActionPreconditionValidator(),
            validationGate = OntologyActionValidationGate(repoRoot),
            graphStore = store,
        ).submit(OntologyActionAuditPlan(actionRequest(), graphs))
        assertTrue(auditResult.audited, auditResult.errors.joinToString(separator = "\n"))
        return store
    }

    private fun transitionService(store: NamedGraphStore): OntologyActionTransitionService {
        return OntologyActionTransitionService(
            validationGate = OntologyActionValidationGate(repoRoot),
            graphStore = store,
        )
    }

    private fun transitionPlan(
        execution: Resource,
        toState: OntologyActionLifecycleState,
        id: String,
    ): OntologyActionTransitionPlan {
        return OntologyActionTransitionPlan(
            request = OntologyActionTransitionRequest(
                transitionId = "ACT-TRN-$id",
                idempotencyKey = "$actionAuditReleaseId:transition:$id",
                actorId = "operator-local-reviewer",
                requestedAt = Instant.parse("2026-06-09T02:30:00Z"),
                targetExecutionUri = execution.uri,
                toState = toState,
                transitionReason = "Local reviewer moved the internal ontology action to ${toState.id}.",
            ),
            graphs = graphs,
        )
    }

    private fun actionExecution(store: InMemoryNamedGraphStore): Resource {
        return actionExecution(store.graph(graphs.actionAuditGraphUri)!!)
    }

    private fun actionExecution(store: NamedGraphStore): Resource {
        return actionExecution(store.readNamedGraph(graphs.actionAuditGraphUri).model)
    }

    private fun actionExecution(model: Model): Resource {
        return model.listSubjectsWithProperty(RDF.type, Dcai.OntologyActionExecution).toList().single()
    }

    private fun actionRequest(): OntologyActionRequest {
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

    private fun inputGraphs(): Map<String, Model> {
        val mapping = SourceExtractRdfMapper().map(
            FileSourceExtractLoader().load(repoRoot.resolve("fixtures/source-extracts/local-controlled-source-v1.properties")),
        )
        val canonical = ModelFactory.createDefaultModel().add(mapping.canonicalModel)
        val reasoning = ReasoningModelBuilder().build(
            ReasoningInput(
                runId = reasoningRunId,
                generatedAt = Instant.parse("2026-06-09T01:00:00Z"),
                canonicalModel = mapping.canonicalModel,
                provenanceModel = mapping.provenanceModel,
            ),
        ).reasoningModel.apply {
            val restoreReadiness = ResourceFactory.createResource(RESTORE_READINESS_URI)
            val activity = ResourceFactory.createResource("urn:dcai:reasoning-activity:$reasoningRunId")
            add(restoreReadiness, RDF.type, Dcai.RestoreReadinessFinding)
            add(restoreReadiness, Dcai.hasIdentifier, "restore-local-001")
            add(restoreReadiness, Dcai.hasFindingSummary, "Restore is blocked for local review.")
            add(restoreReadiness, Prov.wasDerivedFrom, ResourceFactory.createResource(SOURCE_RECORD_URI))
            add(restoreReadiness, Prov.wasGeneratedBy, activity)
        }
        return mapOf(
            graphs.canonicalGraphUri to canonical,
            graphs.provenanceGraphUri to mapping.provenanceModel,
            graphs.reasoningGraphUri!! to reasoning,
        )
    }

    private fun Model.countType(type: Resource): Int {
        return listSubjectsWithProperty(RDF.type, type).toList().distinct().size
    }

    private fun Model.requiredLiteral(subject: Resource, property: org.apache.jena.rdf.model.Property): String {
        return listObjectsOfProperty(subject, property).toList().single().asLiteral().lexicalForm
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
    }
}
