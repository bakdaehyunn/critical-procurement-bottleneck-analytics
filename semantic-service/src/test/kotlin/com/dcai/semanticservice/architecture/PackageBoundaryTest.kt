package com.dcai.semanticservice.architecture

import com.dcai.semanticservice.runtime.SemanticServiceComposition
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackageBoundaryTest {
    private val sourceRoot = SemanticServiceComposition.locateRepoRoot()
        .resolve("semantic-service/src/main/kotlin/com/dcai/semanticservice")

    @Test
    fun productionPackageDependenciesAreAcyclic() {
        val dependencies = sourceFiles().fold(mutableMapOf<String, MutableSet<String>>()) { graph, file ->
            val text = file.readText()
            val owner = MODULE_PACKAGE.find(text)?.groupValues?.get(1) ?: return@fold graph
            val targets = IMPORT.findAll(text)
                .map { match -> match.groupValues[1] }
                .filter { target -> target != owner }
                .toSet()
            graph.getOrPut(owner, ::mutableSetOf).addAll(targets)
            graph
        }
        val cycles = dependencies.keys.flatMap { start -> findCycles(start, dependencies) }
            .map { cycle -> cycle.joinToString(" -> ") }
            .distinct()

        assertEquals(emptyList(), cycles, "Semantic-service package dependencies must remain acyclic")
    }

    @Test
    fun neutralInfrastructureDoesNotDependOnFeatureOrRuntimePackages() {
        val forbidden = setOf("actions", "api", "dynamic", "fixtures", "governance", "ingestion", "promotion", "reasoning", "response", "runtime")
        mapOf(
            "ontology" to sourceRoot.resolve("ontology"),
            "validation" to sourceRoot.resolve("validation"),
            "graph" to sourceRoot.resolve("graph"),
        ).forEach { (owner, directory) ->
            val imports: List<String> = sourceFiles(directory).flatMap { file ->
                IMPORT.findAll(file.readText()).map { match -> match.groupValues[1] }.toList()
            }
            assertTrue(imports.none { it in forbidden }, "$owner must not import feature/runtime packages: $imports")
        }
    }

    private fun sourceFiles(root: java.nio.file.Path = sourceRoot) = Files.walk(root).use { paths ->
        paths.filter { it.extension == "kt" }.toList()
    }

    private fun findCycles(start: String, graph: Map<String, Set<String>>): List<List<String>> {
        fun visit(current: String, path: List<String>): List<List<String>> {
            if (current == start && path.isNotEmpty()) return listOf(path + start)
            if (current in path) return emptyList()
            return graph[current].orEmpty().flatMap { next -> visit(next, path + current) }
        }
        return graph[start].orEmpty().flatMap { next -> visit(next, listOf(start)) }
    }

    private companion object {
        private val MODULE_PACKAGE = Regex("^package com\\.dcai\\.semanticservice\\.([a-zA-Z0-9_]+)", RegexOption.MULTILINE)
        private val IMPORT = Regex("^import com\\.dcai\\.semanticservice\\.([a-zA-Z0-9_]+)", RegexOption.MULTILINE)
    }
}
