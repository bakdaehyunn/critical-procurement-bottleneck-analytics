package com.dcai.semanticservice.governance

import com.dcai.semanticservice.actions.OntologyActionAuditPlan
import com.dcai.semanticservice.actions.OntologyActionAuditResult
import com.dcai.semanticservice.actions.OntologyActionAuditService
import com.dcai.semanticservice.actions.OntologyActionPreconditionValidator
import com.dcai.semanticservice.actions.OntologyActionRdfMapper
import com.dcai.semanticservice.actions.OntologyActionSubmitter
import com.dcai.semanticservice.actions.OntologyActionType
import com.dcai.semanticservice.actions.OntologyActionValidationGate
import com.dcai.semanticservice.actions.OntologyActionValidationReport
import com.dcai.semanticservice.graph.NamedGraphStore
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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.RDF

class AiGovernanceReviewServiceTest {
    private val repoRoot = SemanticServiceComposition.locateRepoRoot()
    private val sourceReleaseId = "local-controlled-source-v1"
    private val reasoningRunId = "local-controlled-reasoning-v1"
    private val aiAuditReleaseId = "local-ai-governance-v1"
    private val actionAuditReleaseId = "local-action-audit-v1"
    private val graphs = AiGovernanceReviewGraphUris.forRelease(
        sourceReleaseId = sourceReleaseId,
        reasoningRunId = reasoningRunId,
        aiAuditReleaseId = aiAuditReleaseId,
        actionAuditReleaseId = actionAuditReleaseId,
    )

    @Test
    fun approvedActionRecommendationWritesReviewAndGovernedActionRequest() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())

        val result = service(store).submit(plan(validReview()))

        assertTrue(result.reviewed, result.errors.joinToString(separator = "\n"))
        assertTrue(result.actionRequestCreated)
        assertEquals(OntologyActionType.ACKNOWLEDGE_RESTORE_BLOCKER.id, result.actionId)
        assertEquals(listOf(graphs.aiAuditGraphUri, graphs.actionAuditGraphUri), store.writeOrder)
        assertNull(store.graph(graphs.canonicalGraphUri)?.listSubjectsWithProperty(RDF.type, Dcai.AIApprovalDecision)?.toList()?.singleOrNull())
        val aiAudit = store.graph(graphs.aiAuditGraphUri)!!
        assertEquals(1, aiAudit.countType(Dcai.AIApprovalDecision))
        assertTrue(aiAudit.contains(ResourceFactory.createResource(PROPOSAL_URI), Dcai.hasAIGovernanceReviewStatus, "APPROVED"))
        val actionAudit = store.graph(graphs.actionAuditGraphUri)!!
        assertEquals(1, actionAudit.countType(Dcai.OntologyActionRequest))
        assertEquals(1, actionAudit.countType(Dcai.OntologyActionExecution))
        assertTrue(actionAudit.contains(null, Dcai.hasActionStatus, "QUEUED"))
    }

    @Test
    fun rejectedProposalWritesAiAuditReviewWithoutActionRequest() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())

        val result = service(store).submit(plan(validReview().copy(decision = AiGovernanceReviewDecision.REJECT)))

        assertTrue(result.reviewed, result.errors.joinToString(separator = "\n"))
        assertFalse(result.actionRequestCreated)
        assertEquals(listOf(graphs.aiAuditGraphUri), store.writeOrder)
        assertTrue(store.graph(graphs.aiAuditGraphUri)!!.contains(ResourceFactory.createResource(PROPOSAL_URI), Dcai.hasAIGovernanceReviewStatus, "REJECTED"))
        assertNull(store.graph(graphs.actionAuditGraphUri))
    }

    @Test
    fun duplicateReviewRerunIsIdempotentAndDoesNotCreateSecondAction() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())
        val service = service(store)

        val first = service.submit(plan(validReview()))
        val second = service.submit(plan(validReview()))

        assertTrue(first.reviewed, first.errors.joinToString(separator = "\n"))
        assertTrue(second.reviewed, second.errors.joinToString(separator = "\n"))
        assertTrue(second.idempotentReplay)
        assertEquals(1, store.graph(graphs.aiAuditGraphUri)!!.countType(Dcai.AIApprovalDecision))
        assertEquals(1, store.graph(graphs.actionAuditGraphUri)!!.countType(Dcai.OntologyActionRequest))
    }

    @Test
    fun actionHandoffFailureRollsBackAiAuditReviewDecision() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())
        val service = AiGovernanceReviewService(
            validationGate = AiGovernanceProposalValidationGate(repoRoot),
            graphStore = store,
            actionSubmitter = FailingActionSubmitter(),
        )

        val result = service.submit(plan(validReview()))

        assertFalse(result.reviewed)
        assertTrue(result.rollbackAttempted)
        assertTrue(result.rollbackSucceeded, result.errors.joinToString(separator = "\n"))
        val aiAudit = store.graph(graphs.aiAuditGraphUri)!!
        assertEquals(0, aiAudit.countType(Dcai.AIApprovalDecision))
        assertTrue(aiAudit.contains(ResourceFactory.createResource(PROPOSAL_URI), Dcai.hasAIGovernanceReviewStatus, "PENDING_HUMAN_REVIEW"))
    }

    @Test
    fun rejectsReviewForAlreadyReviewedProposal() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())
        val service = service(store)

        val first = service.submit(plan(validReview()))
        val second = service.submit(plan(validReview().copy(idempotencyKey = "local-ai-review-v1:second-review:INC-001")))

        assertTrue(first.reviewed, first.errors.joinToString(separator = "\n"))
        assertFalse(second.reviewed)
        assertTrue(second.errors.any { it.contains("already been reviewed") })
        assertEquals(1, store.graph(graphs.aiAuditGraphUri)!!.countType(Dcai.AIApprovalDecision))
    }

    @Test
    fun rejectsUnmanagedActionAuditGraphUri() {
        assertFailsWith<IllegalArgumentException> {
            AiGovernanceReviewGraphUris(
                canonicalGraphUri = graphs.canonicalGraphUri,
                provenanceGraphUri = graphs.provenanceGraphUri,
                reasoningGraphUri = graphs.reasoningGraphUri,
                aiAuditGraphUri = graphs.aiAuditGraphUri,
                actionAuditGraphUri = "urn:dcai:graph:ai-audit:not-action-audit",
            )
        }
    }

    private fun service(store: NamedGraphStore): AiGovernanceReviewService {
        return AiGovernanceReviewService(
            validationGate = AiGovernanceProposalValidationGate(repoRoot),
            graphStore = store,
            actionSubmitter = OntologyActionAuditService(
                mapper = OntologyActionRdfMapper(),
                preconditionValidator = OntologyActionPreconditionValidator(),
                validationGate = OntologyActionValidationGate(repoRoot),
                graphStore = store,
            ),
        )
    }

    private fun plan(request: AiGovernanceReviewRequest): AiGovernanceReviewPlan {
        return AiGovernanceReviewPlan(request = request, graphs = graphs)
    }

    private fun validReview(): AiGovernanceReviewRequest {
        return AiGovernanceReviewRequest(
            reviewId = "AI-REV-LOCAL-001",
            idempotencyKey = "local-ai-review-v1:approve:AI-PROP-LOCAL-001",
            actorId = "operator-local-reviewer",
            reviewedAt = Instant.parse("2026-06-09T03:00:00Z"),
            proposalUri = PROPOSAL_URI,
            decision = AiGovernanceReviewDecision.APPROVE,
            reviewReason = "Human reviewer accepted the AI recommendation for local action audit creation.",
            actionType = OntologyActionType.ACKNOWLEDGE_RESTORE_BLOCKER,
        )
    }

    private fun inputGraphs(): Map<String, Model> {
        val mapping = SourceExtractRdfMapper().map(
            FileSourceExtractLoader().load(repoRoot.resolve("fixtures/source-extracts/local-controlled-source-v1.properties")),
        )
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
            add(restoreReadiness, Dcai.hasFindingSummary, "Restore is blocked for local AI governance review.")
            add(restoreReadiness, Prov.wasDerivedFrom, ResourceFactory.createResource(SOURCE_RECORD_URI))
            add(restoreReadiness, Prov.wasGeneratedBy, activity)
        }
        val aiAudit = AiGovernanceProposalRdfMapper().map(
            AiGovernanceProposalLoader().load(repoRoot.resolve("fixtures/ai-proposals/local-ai-governance-v1.properties")),
        )
        return mapOf(
            graphs.canonicalGraphUri to mapping.canonicalModel,
            graphs.provenanceGraphUri to mapping.provenanceModel,
            graphs.reasoningGraphUri!! to reasoning,
            graphs.aiAuditGraphUri to aiAudit,
        )
    }

    private fun Model.countType(type: Resource): Int {
        return listSubjectsWithProperty(RDF.type, type).toList().distinct().size
    }

    private class FailingActionSubmitter : OntologyActionSubmitter {
        override fun submit(plan: OntologyActionAuditPlan): OntologyActionAuditResult {
            return OntologyActionAuditResult(
                audited = false,
                validation = OntologyActionValidationReport(
                    conforms = false,
                    errors = listOf("simulated action handoff failure"),
                ),
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                errors = listOf("simulated action handoff failure"),
            )
        }
    }

    private companion object {
        private const val SOURCE_RECORD_URI = "urn:dcai:source-record:local-controlled-facility-ops-file:SRC-INC-001"
        private const val RESTORE_READINESS_URI = "urn:dcai:reasoning:restore-readiness:urn%3Adcai%3Aincident%3AINC-001"
        private const val PROPOSAL_URI = "urn:dcai:ai-proposal:local-ai-governance-v1%3Aaction-recommendation%3AINC-001"
    }
}
