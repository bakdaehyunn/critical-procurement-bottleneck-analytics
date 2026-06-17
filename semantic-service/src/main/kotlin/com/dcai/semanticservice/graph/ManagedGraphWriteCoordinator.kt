package com.dcai.semanticservice.graph

import org.apache.jena.rdf.model.Model

class ManagedGraphWriteCoordinator(
    private val graphStore: NamedGraphStore,
) {
    fun snapshot(graphUris: Iterable<String>): Map<String, NamedGraphSnapshot> {
        return graphUris.associateWith { graphUri -> graphStore.readNamedGraph(graphUri) }
    }

    fun replaceAll(
        graphModels: Map<String, Model>,
        snapshots: Map<String, NamedGraphSnapshot>,
        writeFailurePrefix: String,
        rollbackFailurePrefix: String,
    ): ManagedGraphWriteOutcome {
        val writtenGraphs = mutableListOf<String>()
        var attemptedGraphUri: String? = null
        return runCatching {
            graphModels.forEach { (graphUri, model) ->
                attemptedGraphUri = graphUri
                graphStore.replaceNamedGraph(graphUri, model)
                writtenGraphs += graphUri
                attemptedGraphUri = null
            }
            ManagedGraphWriteOutcome(
                succeeded = true,
                writtenGraphUris = writtenGraphs.toList(),
            )
        }.getOrElse { writeError ->
            val rollbackGraphUris = buildList {
                addAll(writtenGraphs.asReversed())
                attemptedGraphUri
                    ?.takeIf { it !in writtenGraphs }
                    ?.takeIf { graphUri -> changedSinceSnapshot(graphUri, snapshots.getValue(graphUri)) }
                    ?.let(::add)
            }
            val rollbackErrors = rollback(
                graphUris = rollbackGraphUris,
                snapshots = snapshots,
                rollbackFailurePrefix = rollbackFailurePrefix,
            )
            ManagedGraphWriteOutcome(
                succeeded = false,
                writtenGraphUris = writtenGraphs.toList(),
                rollbackAttempted = true,
                rollbackSucceeded = rollbackErrors.isEmpty(),
                errors = listOf("$writeFailurePrefix: ${writeError.message}") + rollbackErrors,
            )
        }
    }

    private fun changedSinceSnapshot(graphUri: String, snapshot: NamedGraphSnapshot): Boolean {
        return runCatching {
            val current = graphStore.readNamedGraph(graphUri)
            current.exists != snapshot.exists || !current.model.isIsomorphicWith(snapshot.model)
        }.getOrElse {
            true
        }
    }

    fun rollback(
        graphUri: String,
        snapshot: NamedGraphSnapshot,
        rollbackFailurePrefix: String,
    ): List<String> {
        return rollback(
            graphUris = listOf(graphUri),
            snapshots = mapOf(graphUri to snapshot),
            rollbackFailurePrefix = rollbackFailurePrefix,
        )
    }

    private fun rollback(
        graphUris: List<String>,
        snapshots: Map<String, NamedGraphSnapshot>,
        rollbackFailurePrefix: String,
    ): List<String> {
        return graphUris.mapNotNull { graphUri ->
            val snapshot = snapshots.getValue(graphUri)
            runCatching {
                if (snapshot.exists) {
                    graphStore.replaceNamedGraph(graphUri, snapshot.copyModel())
                } else {
                    graphStore.deleteNamedGraph(graphUri)
                }
            }.exceptionOrNull()?.let { error ->
                "$rollbackFailurePrefix for $graphUri: ${error.message}"
            }
        }
    }
}

data class ManagedGraphWriteOutcome(
    val succeeded: Boolean,
    val writtenGraphUris: List<String> = emptyList(),
    val rollbackAttempted: Boolean = false,
    val rollbackSucceeded: Boolean = false,
    val errors: List<String> = emptyList(),
)
