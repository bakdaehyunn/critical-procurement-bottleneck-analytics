package com.dcai.semanticservice.contracts

import com.dcai.semanticservice.api.PrivateAiGovernanceEndpoint
import com.dcai.semanticservice.api.PrivateOntologyActionEndpoint
import java.nio.file.Files
import java.nio.file.Path

class OpenApiEndpointContractValidator {
    fun validate(repoRoot: Path): List<String> {
        val openApi = repoRoot.resolve("semantic-service/openapi.semantic-service.yaml")
        if (!Files.isRegularFile(openApi)) return listOf("Missing OpenAPI contract: semantic-service/openapi.semantic-service.yaml")

        val paths = parsePaths(Files.readAllLines(openApi))
        val implemented = paths.filterValues { it.runtimeStatus == IMPLEMENTED_PRIVATE }
        val expectedRuntimeRoutes = setOf(
            "/semantic/query/{queryId}",
            PrivateOntologyActionEndpoint.ACTION_REQUEST_PATH,
            PrivateOntologyActionEndpoint.ACTION_TRANSITION_PATH,
            PrivateAiGovernanceEndpoint.AI_PROPOSAL_REVIEW_PATH,
        )
        return buildList {
            if (implemented.keys != expectedRuntimeRoutes) {
                add("OpenAPI implemented-private paths do not match runtime routes: expected=$expectedRuntimeRoutes actual=${implemented.keys}")
            }
            implemented.forEach { (path, contract) ->
                if (contract.methods != setOf("post")) {
                    add("Implemented private OpenAPI path $path must expose POST only, found ${contract.methods}")
                }
            }
            paths.forEach { (path, contract) ->
                if (contract.runtimeStatus !in VALID_RUNTIME_STATUSES) {
                    add("OpenAPI path $path has missing or invalid x-runtime-status '${contract.runtimeStatus}'")
                }
            }
            if (paths.values.none { it.runtimeStatus == DOCUMENTED_ONLY }) {
                add("OpenAPI must retain at least one explicitly documented-only path")
            }
        }
    }

    internal fun parsePaths(lines: List<String>): Map<String, OpenApiPathContract> {
        val parsed = linkedMapOf<String, OpenApiPathContract>()
        var currentPath: String? = null
        lines.forEach { line ->
            PATH.matchEntire(line)?.let { match ->
                currentPath = match.groupValues[1]
                parsed[currentPath!!] = OpenApiPathContract(runtimeStatus = "", methods = emptySet())
                return@forEach
            }
            val path = currentPath ?: return@forEach
            STATUS.matchEntire(line)?.let { match ->
                parsed[path] = parsed.getValue(path).copy(runtimeStatus = match.groupValues[1])
            }
            METHOD.matchEntire(line)?.let { match ->
                parsed[path] = parsed.getValue(path).copy(methods = parsed.getValue(path).methods + match.groupValues[1])
            }
        }
        return parsed
    }

    internal data class OpenApiPathContract(
        val runtimeStatus: String,
        val methods: Set<String>,
    )

    private companion object {
        const val IMPLEMENTED_PRIVATE = "implemented-private"
        const val DOCUMENTED_ONLY = "documented-only"
        val VALID_RUNTIME_STATUSES = setOf(IMPLEMENTED_PRIVATE, DOCUMENTED_ONLY)
        val PATH = Regex("^  (/[^:]+):$")
        val STATUS = Regex("^    x-runtime-status: ([a-z-]+)$")
        val METHOD = Regex("^    (get|post|put|delete|patch):$")
    }
}
