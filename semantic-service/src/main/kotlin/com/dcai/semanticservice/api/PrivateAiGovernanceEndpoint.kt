package com.dcai.semanticservice.api

import com.dcai.semanticservice.actions.OntologyActionType
import com.dcai.semanticservice.governance.AiGovernanceReviewDecision
import com.dcai.semanticservice.governance.AiGovernanceReviewGraphUris
import com.dcai.semanticservice.governance.AiGovernanceReviewPlan
import com.dcai.semanticservice.governance.AiGovernanceReviewRequest
import com.dcai.semanticservice.governance.AiGovernanceReviewResult
import com.dcai.semanticservice.governance.AiGovernanceReviewSubmitter
import com.dcai.semanticservice.response.SemanticErrorCode
import com.dcai.semanticservice.response.SemanticResponseSerializer
import java.time.Instant

class PrivateAiGovernanceEndpoint(
    private val reviewSubmitter: AiGovernanceReviewSubmitter,
    private val responseSerializer: SemanticResponseSerializer = SemanticResponseSerializer(),
) {
    fun handle(request: PrivateSemanticQueryRequest): PrivateSemanticQueryResponse {
        if (request.method != "POST") {
            return error(
                statusCode = 405,
                message = "Private AI governance endpoint requires POST.",
            )
        }
        if (request.path != AI_PROPOSAL_REVIEW_PATH) {
            return error(
                statusCode = 404,
                message = "Private AI governance route must match $AI_PROPOSAL_REVIEW_PATH.",
            )
        }
        if (PrivateEndpointPayload.containsRawSparql(request.body)) {
            return error(
                statusCode = 400,
                message = "Private AI governance endpoint does not accept raw SPARQL.",
            )
        }

        return try {
            val payload = PrivateEndpointPayload.stringObject(
                body = request.body,
                allowedFields = ALLOWED_FIELDS,
                bodyLabel = "AI proposal review",
                fieldLabel = "AI proposal review",
            )
            val decision = AiGovernanceReviewDecision.fromId(payload.required("decision"))
                ?: return error(
                    statusCode = 400,
                    message = "Unsupported AI governance review decision.",
                    detail = payload["decision"],
                )
            val actionType = payload.optionalControlled("actionId")?.let { actionId ->
                OntologyActionType.fromId(actionId)
                    ?: return error(
                        statusCode = 400,
                        message = "Unsupported ontology action id for AI proposal review.",
                        detail = actionId,
                    )
            }
            val reviewRequest = AiGovernanceReviewRequest(
                reviewId = payload.requiredControlled("reviewId"),
                idempotencyKey = payload.requiredControlled("idempotencyKey"),
                actorId = payload.requiredControlled("actorId"),
                reviewedAt = Instant.parse(payload.required("reviewedAt")),
                proposalUri = payload.requiredUri("proposalUri"),
                decision = decision,
                reviewReason = payload.required("reviewReason"),
                actionType = actionType,
            )
            val graphUris = AiGovernanceReviewGraphUris.forRelease(
                sourceReleaseId = payload.requiredControlled("sourceReleaseId"),
                reasoningRunId = payload.optionalControlled("reasoningRunId"),
                aiAuditReleaseId = payload.requiredControlled("aiAuditReleaseId"),
                actionAuditReleaseId = payload.requiredControlled("actionAuditReleaseId"),
            )
            val result = reviewSubmitter.submit(
                AiGovernanceReviewPlan(
                    request = reviewRequest,
                    graphs = graphUris,
                ),
            )
            if (result.reviewed) {
                PrivateSemanticQueryResponse(
                    statusCode = 200,
                    payload = result.toPayload(reviewRequest),
                )
            } else {
                PrivateSemanticQueryResponse(
                    statusCode = 400,
                    payload = responseSerializer.error(
                        code = SemanticErrorCode.CONTRACT_VALIDATION_FAILED,
                        message = "AI proposal review failed validation and was not recorded.",
                        detail = result.errors.joinToString(separator = "; ").ifBlank {
                            result.validation.errors.joinToString(separator = "; ")
                        },
                    ),
                )
            }
        } catch (error: IllegalArgumentException) {
            error(
                statusCode = 400,
                message = error.message ?: "AI proposal review failed contract validation.",
            )
        } catch (error: RuntimeException) {
            error(
                statusCode = 503,
                code = SemanticErrorCode.GRAPH_UNAVAILABLE,
                message = "AI governance audit graph is unavailable.",
                detail = error.message,
            )
        }
    }

    private fun AiGovernanceReviewResult.toPayload(request: AiGovernanceReviewRequest): Map<String, Any> {
        return buildMap {
            put("resultType", "ai-proposal-review")
            put("reviewed", reviewed)
            put("decision", decision.id)
            put("reviewStatus", decision.reviewStatus)
            put("reviewId", request.reviewId)
            put("idempotencyKey", request.idempotencyKey)
            put("proposalUri", request.proposalUri)
            put("aiAuditGraphUri", aiAuditGraphUri)
            actionAuditGraphUri?.let { put("actionAuditGraphUri", it) }
            put("writtenGraphUris", writtenGraphUris)
            put("idempotentReplay", idempotentReplay)
            put("actionRequestCreated", actionRequestCreated)
            actionRequestId?.let { put("actionRequestId", it) }
            actionId?.let { put("actionId", it) }
            put("canonicalGraphMutation", false)
            put("reasoningGraphMutation", false)
            put("provenanceGraphMutation", false)
            put("sourceGraphMutation", false)
            put("operationsGraphMutation", false)
            put("externalSystemMutation", false)
            put(
                "validation",
                mapOf(
                    "conforms" to validation.conforms,
                    "tripleCount" to validation.tripleCount,
                    "errors" to validation.errors,
                ),
            )
            actionResult?.let { action ->
                put(
                    "actionResult",
                    mapOf(
                        "audited" to action.audited,
                        "actionAuditGraphUri" to action.actionAuditGraphUri,
                        "writtenGraphUris" to action.writtenGraphUris,
                        "idempotentReplay" to action.idempotentReplay,
                        "validation" to mapOf(
                            "conforms" to action.validation.conforms,
                            "tripleCount" to action.validation.tripleCount,
                            "errors" to action.validation.errors,
                        ),
                    ),
                )
            }
        }
    }

    private fun error(
        statusCode: Int,
        message: String,
        detail: String? = null,
        code: SemanticErrorCode = SemanticErrorCode.CONTRACT_VALIDATION_FAILED,
    ): PrivateSemanticQueryResponse {
        return PrivateSemanticQueryResponse(
            statusCode = statusCode,
            payload = responseSerializer.error(
                code = code,
                message = message,
                detail = detail,
            ),
        )
    }

    companion object {
        const val AI_PROPOSAL_REVIEW_PATH = "/semantic/internal/ai-proposal-review"
        private val ALLOWED_FIELDS = setOf(
            "reviewId",
            "idempotencyKey",
            "actorId",
            "reviewedAt",
            "proposalUri",
            "decision",
            "reviewReason",
            "actionId",
            "sourceReleaseId",
            "reasoningRunId",
            "aiAuditReleaseId",
            "actionAuditReleaseId",
        )
    }
}
