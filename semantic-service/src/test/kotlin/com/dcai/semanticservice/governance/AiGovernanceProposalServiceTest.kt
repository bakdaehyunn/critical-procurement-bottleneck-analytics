package com.dcai.semanticservice.governance

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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.RDF

class AiGovernanceProposalServiceTest {
    private val repoRoot = SemanticServiceComposition.locateRepoRoot()
    private val sourceReleaseId = "local-controlled-source-v1"
    private val reasoningRunId = "local-controlled-reasoning-v1"
    private val aiAuditReleaseId = "local-ai-governance-v1"
    private val graphs = AiGovernanceGraphUris.forRelease(
        sourceReleaseId = sourceReleaseId,
        reasoningRunId = reasoningRunId,
        aiAuditReleaseId = aiAuditReleaseId,
    )

    @Test
    fun writesValidProposalOnlyToManagedAiAuditGraph() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())

        val result = service(store).submit(plan(validRequest()))

        assertTrue(result.proposed, result.errors.joinToString(separator = "\n"))
        assertEquals(listOf(graphs.aiAuditGraphUri), result.writtenGraphUris)
        assertEquals(listOf(graphs.aiAuditGraphUri), store.writeOrder)
        assertNull(store.graph(graphs.canonicalGraphUri)?.listSubjectsWithProperty(RDF.type, Dcai.AIProposal)?.toList()?.singleOrNull())
        val aiAuditGraph = store.graph(graphs.aiAuditGraphUri)!!
        assertEquals(1, aiAuditGraph.countType(Dcai.AIProposal))
        assertEquals(1, aiAuditGraph.countType(Dcai.AIProposalBatch))
        assertEquals(1, aiAuditGraph.countType(Dcai.AIProposalValidationReport))
        assertTrue(aiAuditGraph.contains(null, Dcai.hasProposalType, AiProposalType.ACTION_RECOMMENDATION.id))
        assertTrue(aiAuditGraph.contains(null, Dcai.hasAIGovernanceReviewStatus, "PENDING_HUMAN_REVIEW"))
    }

    @Test
    fun loaderRejectsUnsupportedProposalType() {
        val temp = kotlin.io.path.createTempFile("bad-ai-proposal", ".properties")
        temp.toFile().writeText(
            repoRoot.resolve("fixtures/ai-proposals/local-ai-governance-v1.properties")
                .toFile()
                .readText()
                .replace("proposal.type=ACTION_RECOMMENDATION", "proposal.type=UNSUPPORTED_WRITE"),
        )

        assertFailsWith<IllegalStateException> {
            AiGovernanceProposalLoader().load(temp)
        }
    }

    @Test
    fun rejectsMissingSourceRecordProvenanceWithoutWritingAiAuditGraph() {
        val inputGraphs = inputGraphs().toMutableMap()
        inputGraphs[graphs.provenanceGraphUri] = ModelFactory.createDefaultModel()
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs)

        val result = service(store).submit(plan(validRequest()))

        assertFalse(result.proposed)
        assertTrue(result.errors.any { it.contains("Source record provenance is missing") })
        assertNull(store.graph(graphs.aiAuditGraphUri))
    }

    @Test
    fun rejectsMissingSupportingEvidenceWithoutWritingAiAuditGraph() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())
        val request = validRequest().copy(supportingEvidenceUri = "urn:dcai:reasoning:missing-evidence")

        val result = service(store).submit(plan(request))

        assertFalse(result.proposed)
        assertTrue(result.errors.any { it.contains("Supporting evidence is missing") })
        assertNull(store.graph(graphs.aiAuditGraphUri))
    }

    @Test
    fun rejectsConfidenceOutsidePolicy() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())

        val result = service(store).submit(plan(validRequest().copy(confidenceScore = 0.2)))

        assertFalse(result.proposed)
        assertTrue(result.errors.any { it.contains("confidenceScore must be between") })
        assertNull(store.graph(graphs.aiAuditGraphUri))
    }

    @Test
    fun deterministicRerunIsIdempotentAndDoesNotDuplicateProposal() {
        val store = InMemoryNamedGraphStore(initialGraphs = inputGraphs())
        val service = service(store)

        val first = service.submit(plan(validRequest()))
        val second = service.submit(plan(validRequest()))

        assertTrue(first.proposed, first.errors.joinToString(separator = "\n"))
        assertTrue(second.proposed, second.errors.joinToString(separator = "\n"))
        assertTrue(second.idempotentReplay)
        assertEquals(1, store.graph(graphs.aiAuditGraphUri)!!.countType(Dcai.AIProposal))
        assertEquals(listOf(graphs.aiAuditGraphUri), store.writeOrder)
    }

    @Test
    fun restoresPreviousAiAuditGraphWhenWriteFailsAfterPartialMutation() {
        val previousAudit = markerModel("previous-ai-audit")
        val store = PartiallyFailingAiAuditStore(
            initialGraphs = inputGraphs() + mapOf(graphs.aiAuditGraphUri to previousAudit),
            failGraphUri = graphs.aiAuditGraphUri,
        )

        val result = service(store).submit(plan(validRequest()))

        assertFalse(result.proposed)
        assertTrue(result.rollbackAttempted)
        assertTrue(result.rollbackSucceeded, result.errors.joinToString(separator = "\n"))
        assertTrue(store.graph(graphs.aiAuditGraphUri)!!.isIsomorphicWith(previousAudit))
    }

    @Test
    fun rejectsUnmanagedAiAuditGraphUri() {
        assertFailsWith<IllegalArgumentException> {
            AiGovernanceGraphUris(
                canonicalGraphUri = graphs.canonicalGraphUri,
                provenanceGraphUri = graphs.provenanceGraphUri,
                reasoningGraphUri = graphs.reasoningGraphUri,
                aiAuditGraphUri = "urn:dcai:graph:action-audit:not-ai-audit",
            )
        }
    }

    private fun service(store: NamedGraphStore): AiGovernanceProposalService {
        return AiGovernanceProposalService(
            mapper = AiGovernanceProposalRdfMapper(),
            preconditionValidator = AiGovernanceProposalPreconditionValidator(),
            validationGate = AiGovernanceProposalValidationGate(repoRoot),
            graphStore = store,
        )
    }

    private fun plan(request: AiGovernanceProposalRequest): AiGovernanceProposalPlan {
        return AiGovernanceProposalPlan(request = request, graphs = graphs)
    }

    private fun validRequest(): AiGovernanceProposalRequest {
        return AiGovernanceProposalLoader().load(repoRoot.resolve("fixtures/ai-proposals/local-ai-governance-v1.properties"))
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
        return mapOf(
            graphs.canonicalGraphUri to mapping.canonicalModel,
            graphs.provenanceGraphUri to mapping.provenanceModel,
            graphs.reasoningGraphUri!! to reasoning,
        )
    }

    private fun Model.countType(type: Resource): Int {
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

    private class PartiallyFailingAiAuditStore(
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
                error("simulated AI governance write failure for $graphUri")
            }
            return NamedGraphWriteResult(graphUri = graphUri, tripleCount = model.size().toInt(), statusCode = 200)
        }

        override fun deleteNamedGraph(graphUri: String): NamedGraphWriteResult {
            graphs.remove(graphUri)
            return NamedGraphWriteResult(graphUri = graphUri, tripleCount = 0, statusCode = 204)
        }
    }

    private companion object {
        private const val SOURCE_RECORD_URI = "urn:dcai:source-record:local-controlled-facility-ops-file:SRC-INC-001"
        private const val RESTORE_READINESS_URI = "urn:dcai:reasoning:restore-readiness:urn%3Adcai%3Aincident%3AINC-001"
    }
}
