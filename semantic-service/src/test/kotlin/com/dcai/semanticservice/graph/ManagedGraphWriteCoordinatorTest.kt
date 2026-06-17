package com.dcai.semanticservice.graph

import com.dcai.semanticservice.testfixtures.InMemoryNamedGraphStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory

class ManagedGraphWriteCoordinatorTest {
    @Test
    fun replacesManagedGraphsWhenAllWritesSucceed() {
        val store = InMemoryNamedGraphStore()
        val coordinator = ManagedGraphWriteCoordinator(store)
        val graphs = linkedMapOf(
            GRAPH_A to markerModel("a"),
            GRAPH_B to markerModel("b"),
        )

        val snapshots = coordinator.snapshot(graphs.keys)
        val result = coordinator.replaceAll(
            graphModels = graphs,
            snapshots = snapshots,
            writeFailurePrefix = "Managed write failed",
            rollbackFailurePrefix = "Managed rollback failed",
        )

        assertTrue(result.succeeded)
        assertEquals(listOf(GRAPH_A, GRAPH_B), result.writtenGraphUris)
        assertFalse(result.rollbackAttempted)
        assertTrue(store.graph(GRAPH_A)!!.isIsomorphicWith(markerModel("a")))
        assertTrue(store.graph(GRAPH_B)!!.isIsomorphicWith(markerModel("b")))
    }

    @Test
    fun restoresExistingGraphsAndDeletesNewGraphsWhenLaterWriteFails() {
        val previousA = markerModel("previous-a")
        val store = InMemoryNamedGraphStore(
            initialGraphs = mapOf(GRAPH_A to previousA),
            failOnReplaceGraphUri = GRAPH_C,
        )
        val coordinator = ManagedGraphWriteCoordinator(store)
        val graphs = linkedMapOf(
            GRAPH_A to markerModel("new-a"),
            GRAPH_B to markerModel("new-b"),
            GRAPH_C to markerModel("new-c"),
        )

        val snapshots = coordinator.snapshot(graphs.keys)
        val result = coordinator.replaceAll(
            graphModels = graphs,
            snapshots = snapshots,
            writeFailurePrefix = "Managed write failed",
            rollbackFailurePrefix = "Managed rollback failed",
        )

        assertFalse(result.succeeded)
        assertEquals(listOf(GRAPH_A, GRAPH_B), result.writtenGraphUris)
        assertTrue(result.rollbackAttempted)
        assertTrue(result.rollbackSucceeded, result.errors.joinToString(separator = "\n"))
        assertTrue(result.errors.first().startsWith("Managed write failed: simulated write failure for $GRAPH_C"))
        assertTrue(store.graph(GRAPH_A)!!.isIsomorphicWith(previousA))
        assertNull(store.graph(GRAPH_B))
    }

    private fun markerModel(value: String): Model {
        val model = ModelFactory.createDefaultModel()
        model.add(
            ResourceFactory.createResource("urn:dcai:test:$value"),
            ResourceFactory.createProperty("urn:dcai:test:marker"),
            value,
        )
        return model
    }

    private companion object {
        const val GRAPH_A = "urn:dcai:graph:test:a"
        const val GRAPH_B = "urn:dcai:graph:test:b"
        const val GRAPH_C = "urn:dcai:graph:test:c"
    }
}
