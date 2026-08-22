package com.dcai.semanticservice.dynamic

import com.dcai.semanticservice.graph.NamedGraphSnapshot
import com.dcai.semanticservice.graph.NamedGraphStore
import com.dcai.semanticservice.graph.NamedGraphWriteResult
import com.dcai.semanticservice.ontology.Dcai
import com.dcai.semanticservice.ingestion.SourceExtractRdfMapper
import com.dcai.semanticservice.promotion.GraphPromotionService
import com.dcai.semanticservice.promotion.ProductionGraphValidationGate
import com.dcai.semanticservice.reasoning.ReasoningModelBuilder
import com.dcai.semanticservice.reasoning.ReasoningPromotionService
import com.dcai.semanticservice.reasoning.ReasoningValidationGate
import com.dcai.semanticservice.runtime.SemanticServiceComposition
import com.dcai.semanticservice.testfixtures.InMemoryNamedGraphStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.RDF

class DynamicPlaybackServiceTest {
    private val repoRoot = SemanticServiceComposition.locateRepoRoot()
    private val scenario = LocalDynamicPlaybackScenario.scenario()
    private val graphs = com.dcai.semanticservice.actions.OntologyActionGraphUris.forRelease(
        sourceReleaseId = LocalDynamicPlaybackScenario.DEFAULT_SCENARIO_ID,
        reasoningRunId = "${LocalDynamicPlaybackScenario.DEFAULT_SCENARIO_ID}-reasoning-04",
        actionAuditReleaseId = LocalDynamicPlaybackScenario.DEFAULT_ACTION_AUDIT_RELEASE_ID,
    )

    @Test
    fun playsControlledScenarioThroughPromotionReasoningAndManagedActionAuditGraph() {
        val store = InMemoryNamedGraphStore()
        val result = service(store).run(DynamicPlaybackPlan(scenario, graphs))

        assertTrue(result.played, result.errors.joinToString(separator = "\n"))
        assertEquals(4, result.stepResults.size)
        assertEquals(listOf(graphs.actionAuditGraphUri), result.writtenGraphUris)
        val playbackGraph = assertNotNull(store.graph(graphs.actionAuditGraphUri))
        assertEquals(4, playbackGraph.countType(Dcai.DynamicPlaybackEvent))
        assertEquals(1, playbackGraph.countType(Dcai.DynamicPlaybackBatch))
        assertTrue(playbackGraph.contains(null, Dcai.hasAfterReasoningState, "RESTORE_READY_WITH_MONITORING"))
        assertTrue(playbackGraph.contains(null, Dcai.hasActionLifecycleState, "APPROVED"))
    }

    @Test
    fun deterministicReplayDoesNotDuplicatePlaybackFacts() {
        val store = InMemoryNamedGraphStore()
        val playback = service(store)

        val first = playback.run(DynamicPlaybackPlan(scenario, graphs))
        val firstGraph = assertNotNull(store.graph(graphs.actionAuditGraphUri)).let { ModelFactory.createDefaultModel().add(it) }
        val second = playback.run(DynamicPlaybackPlan(scenario, graphs))
        val secondGraph = assertNotNull(store.graph(graphs.actionAuditGraphUri))

        assertTrue(first.played, first.errors.joinToString(separator = "\n"))
        assertTrue(second.played, second.errors.joinToString(separator = "\n"))
        assertEquals(4, secondGraph.countType(Dcai.DynamicPlaybackEvent))
        assertTrue(secondGraph.isIsomorphicWith(firstGraph))
    }

    @Test
    fun validationRejectsPlaybackModelWithoutEventProvenance() {
        val model = DynamicPlaybackRdfMapper().map(scenario)
        model.removeAll(null, com.dcai.semanticservice.ontology.Prov.used, null)

        val validation = DynamicPlaybackValidationGate(repoRoot).validate(model)

        assertFalse(validation.conforms)
        assertTrue(validation.errors.any { it.contains("Dynamic playback") })
    }

    @Test
    fun restoresPreviousActionAuditGraphWhenPlaybackWriteFails() {
        val baseStore = InMemoryNamedGraphStore()
        val first = service(baseStore).run(DynamicPlaybackPlan(scenario, graphs))
        assertTrue(first.played, first.errors.joinToString(separator = "\n"))
        val previousGraph = assertNotNull(baseStore.graph(graphs.actionAuditGraphUri))
        val failingStore = FailsFirstActionAuditReplaceStore(
            initialGraphs = mapOf(graphs.actionAuditGraphUri to previousGraph),
            failGraphUri = graphs.actionAuditGraphUri,
        )

        val result = service(failingStore).run(DynamicPlaybackPlan(scenario, graphs))

        assertFalse(result.played)
        assertTrue(result.rollbackAttempted)
        assertTrue(result.rollbackSucceeded, result.errors.joinToString(separator = "\n"))
        assertTrue(assertNotNull(failingStore.graph(graphs.actionAuditGraphUri)).isIsomorphicWith(previousGraph))
    }

    private fun service(store: NamedGraphStore): DynamicPlaybackService {
        return DynamicPlaybackService(
            sourcePromoter = GraphPromotionService(
                mapper = SourceExtractRdfMapper(),
                validationGate = ProductionGraphValidationGate(repoRoot),
                graphStore = store,
            ),
            reasoningRefresher = ReasoningPromotionService(
                builder = ReasoningModelBuilder(),
                validationGate = ReasoningValidationGate(repoRoot),
                graphStore = store,
            ),
            mapper = DynamicPlaybackRdfMapper(),
            validationGate = DynamicPlaybackValidationGate(repoRoot),
            graphStore = store,
        )
    }

    private fun Model.countType(type: Resource): Int {
        return listSubjectsWithProperty(RDF.type, type).toList().distinct().size
    }

    private class FailsFirstActionAuditReplaceStore(
        initialGraphs: Map<String, Model>,
        private val failGraphUri: String,
    ) : NamedGraphStore {
        private val graphs = initialGraphs.mapValuesTo(mutableMapOf()) { (_, model) ->
            ModelFactory.createDefaultModel().add(model)
        }
        private var failed = false

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
            if (graphUri == failGraphUri && !failed) {
                failed = true
                error("simulated dynamic playback write failure for $graphUri")
            }
            graphs[graphUri] = ModelFactory.createDefaultModel().add(model)
            return NamedGraphWriteResult(graphUri = graphUri, tripleCount = model.size().toInt(), statusCode = 200)
        }

        override fun deleteNamedGraph(graphUri: String): NamedGraphWriteResult {
            graphs.remove(graphUri)
            return NamedGraphWriteResult(graphUri = graphUri, tripleCount = 0, statusCode = 204)
        }
    }
}
