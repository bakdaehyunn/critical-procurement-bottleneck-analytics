package com.dcai.semanticservice.api

import com.dcai.semanticservice.graph.FusekiReadOnlyConfig
import com.dcai.semanticservice.actions.OntologyActionAuditService
import com.dcai.semanticservice.actions.OntologyActionPreconditionValidator
import com.dcai.semanticservice.actions.OntologyActionRdfMapper
import com.dcai.semanticservice.actions.OntologyActionTransitionService
import com.dcai.semanticservice.actions.OntologyActionValidationGate
import com.dcai.semanticservice.graph.FusekiGraphStoreConfig
import com.dcai.semanticservice.graph.FusekiNamedGraphWriter
import com.dcai.semanticservice.governance.AiGovernanceProposalValidationGate
import com.dcai.semanticservice.governance.AiGovernanceReviewService
import com.dcai.semanticservice.query.ApprovedQueryCatalog
import com.dcai.semanticservice.query.JenaFusekiReadOnlyQueryExecutor
import com.dcai.semanticservice.query.QueryPageRequest
import com.dcai.semanticservice.query.QueryContractRegistry
import com.dcai.semanticservice.query.QueryResultShaper
import com.dcai.semanticservice.query.ReadOnlyQueryExecutor
import com.dcai.semanticservice.response.SemanticErrorCode
import com.dcai.semanticservice.response.SemanticResponseSerializer
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path

private const val PAGE_PARAMETER = "page"
private const val PAGE_SIZE_PARAMETER = "pageSize"
private val PAGING_PARAMETER_NAMES = setOf(PAGE_PARAMETER, PAGE_SIZE_PARAMETER)

class PrivateSemanticQueryEndpoint(
    private val queryExecutor: ReadOnlyQueryExecutor,
    private val queryResultShaper: QueryResultShaper,
    private val responseSerializer: SemanticResponseSerializer = SemanticResponseSerializer(),
    private val allowedQueryIds: Set<String> = queryResultShaper.approvedPrivateQueryIds,
) {
    fun handle(request: PrivateSemanticQueryRequest): PrivateSemanticQueryResponse {
        if (request.method != "POST") {
            return error(
                statusCode = 405,
                code = SemanticErrorCode.CONTRACT_VALIDATION_FAILED,
                message = "Private semantic query endpoint requires POST.",
            )
        }

        val queryId = request.queryId()
            ?: return error(
                statusCode = 404,
                code = SemanticErrorCode.CONTRACT_VALIDATION_FAILED,
                message = "Private semantic query route must match /semantic/query/{queryId}.",
            )

        if (PrivateEndpointPayload.containsRawSparql(request.body)) {
            return error(
                statusCode = 400,
                code = SemanticErrorCode.CONTRACT_VALIDATION_FAILED,
                message = "Private semantic query endpoint does not accept raw SPARQL.",
                queryId = queryId,
            )
        }

        if (queryId !in allowedQueryIds) {
            return error(
                statusCode = 400,
                code = SemanticErrorCode.UNAPPROVED_QUERY_ID,
                message = "Query id is not approved for the private semantic endpoint.",
                detail = "Only approved private semantic query IDs are enabled.",
                queryId = queryId,
            )
        }

        return try {
            val parameters = request.parameters()
            val pageRequest = parameters.pageRequest()
            val queryParameters = parameters - PAGING_PARAMETER_NAMES
            val report = queryExecutor.execute(queryId, queryParameters, pageRequest)
            val envelope = queryResultShaper.shape(report)
            val payload = responseSerializer.serialize(envelope, report.page)
            PrivateSemanticQueryResponse(
                statusCode = 200,
                payload = payload,
            )
        } catch (error: IllegalArgumentException) {
            val code = if (error.message.orEmpty().contains("Missing required binding")) {
                SemanticErrorCode.MISSING_REQUIRED_BINDING
            } else {
                SemanticErrorCode.CONTRACT_VALIDATION_FAILED
            }
            error(
                statusCode = 400,
                code = code,
                message = error.message ?: "Semantic query response failed contract validation.",
                queryId = queryId,
            )
        } catch (error: IllegalStateException) {
            val message = error.message.orEmpty()
            val code = when {
                message.contains("No result envelope contract") -> SemanticErrorCode.UNSUPPORTED_RESULT_ENVELOPE
                message.contains("Unapproved query id") -> SemanticErrorCode.UNAPPROVED_QUERY_ID
                else -> SemanticErrorCode.INTERNAL_SEMANTIC_SERVICE_ERROR
            }
            error(
                statusCode = if (code == SemanticErrorCode.UNAPPROVED_QUERY_ID) 400 else 500,
                code = code,
                message = message.ifBlank { "Semantic query execution failed." },
                queryId = queryId,
            )
        } catch (error: RuntimeException) {
            error(
                statusCode = 503,
                code = SemanticErrorCode.GRAPH_UNAVAILABLE,
                message = "Graph query endpoint is unavailable.",
                detail = error.message,
                queryId = queryId,
            )
        }
    }

    private fun error(
        statusCode: Int,
        code: SemanticErrorCode,
        message: String,
        detail: String? = null,
        queryId: String? = null,
    ): PrivateSemanticQueryResponse {
        return PrivateSemanticQueryResponse(
            statusCode = statusCode,
            payload = responseSerializer.error(
                code = code,
                message = message,
                detail = detail,
                queryId = queryId,
            ),
        )
    }

    private fun PrivateSemanticQueryRequest.queryId(): String? {
        val prefix = "/semantic/query/"
        if (!path.startsWith(prefix)) {
            return null
        }
        val encoded = path.removePrefix(prefix).substringBefore("/")
        if (encoded.isBlank()) {
            return null
        }
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8)
    }

    private fun PrivateSemanticQueryRequest.parameters(): Map<String, String> {
        return PrivateEndpointPayload.parameters(body)
    }

}

private fun Map<String, String>.pageRequest(): QueryPageRequest? {
    if (PAGING_PARAMETER_NAMES.none(::containsKey)) return null
    val page = this[PAGE_PARAMETER]?.toIntOrNull() ?: 1
    val pageSize = this[PAGE_SIZE_PARAMETER]?.toIntOrNull()
        ?: throw IllegalArgumentException("Paged semantic queries require an integer pageSize.")
    return QueryPageRequest(page = page, pageSize = pageSize)
}

data class PrivateSemanticQueryRequest(
    val method: String,
    val path: String,
    val body: String = "",
)

data class PrivateSemanticQueryResponse(
    val statusCode: Int,
    val payload: Map<String, Any>,
    val contentType: String = "application/json; charset=utf-8",
) {
    fun jsonBody(): String = JsonPayloadWriter.write(payload)
}
