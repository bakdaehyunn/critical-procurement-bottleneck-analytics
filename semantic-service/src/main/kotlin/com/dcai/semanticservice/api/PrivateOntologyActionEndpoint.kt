package com.dcai.semanticservice.api

import com.dcai.semanticservice.actions.OntologyActionAuditPlan
import com.dcai.semanticservice.actions.OntologyActionAuditResult
import com.dcai.semanticservice.actions.OntologyActionGraphUris
import com.dcai.semanticservice.actions.OntologyActionLifecycleState
import com.dcai.semanticservice.actions.OntologyActionRequest
import com.dcai.semanticservice.actions.OntologyActionSubmitter
import com.dcai.semanticservice.actions.OntologyActionTransitionPlan
import com.dcai.semanticservice.actions.OntologyActionTransitionRequest
import com.dcai.semanticservice.actions.OntologyActionTransitionResult
import com.dcai.semanticservice.actions.OntologyActionTransitionSubmitter
import com.dcai.semanticservice.actions.OntologyActionType
import com.dcai.semanticservice.response.SemanticErrorCode
import com.dcai.semanticservice.response.SemanticResponseSerializer
import java.time.Instant

class PrivateOntologyActionEndpoint(
    private val actionSubmitter: OntologyActionSubmitter,
    private val transitionSubmitter: OntologyActionTransitionSubmitter,
    private val responseSerializer: SemanticResponseSerializer = SemanticResponseSerializer(),
) {
    fun handle(request: PrivateSemanticQueryRequest): PrivateSemanticQueryResponse {
        if (request.method != "POST") {
            return error(
                statusCode = 405,
                message = "Private ontology action endpoint requires POST.",
            )
        }

        if (request.path !in setOf(ACTION_REQUEST_PATH, ACTION_TRANSITION_PATH)) {
            return error(
                statusCode = 404,
                message = "Private ontology action route must match $ACTION_REQUEST_PATH or $ACTION_TRANSITION_PATH.",
            )
        }

        if (PrivateEndpointPayload.containsRawSparql(request.body)) {
            return error(
                statusCode = 400,
                message = "Private ontology action endpoint does not accept raw SPARQL.",
            )
        }

        return try {
            val payload = PrivateEndpointPayload.stringObject(
                body = request.body,
                allowedFields = ALLOWED_FIELDS,
                bodyLabel = "Ontology action request",
                fieldLabel = "ontology action request",
            )
            if (request.path == ACTION_TRANSITION_PATH) {
                return handleTransition(payload)
            }
            val actionType = OntologyActionType.fromId(payload.required("actionId"))
                ?: return error(
                    statusCode = 400,
                    message = "Unsupported ontology action id.",
                    detail = payload["actionId"],
                )
            val actionRequest = OntologyActionRequest(
                requestId = payload.requiredControlled("requestId"),
                actionType = actionType,
                idempotencyKey = payload.requiredControlled("idempotencyKey"),
                actorId = payload.requiredControlled("actorId"),
                requestedAt = Instant.parse(payload.required("requestedAt")),
                incidentUri = payload.requiredUri("incidentUri"),
                actionReason = payload.required("actionReason"),
                sourceRecordUri = payload.requiredUri("sourceRecordUri"),
                restoreReadinessFindingUri = payload.optionalUri("restoreReadinessFindingUri"),
                recoveryBlockerUri = payload.optionalUri("recoveryBlockerUri"),
                trustFindingUri = payload.optionalUri("trustFindingUri"),
                validationEvidenceUri = payload.optionalUri("validationEvidenceUri"),
                assignedTeam = payload.optionalControlled("assignedTeam"),
                assigneeId = payload.optionalControlled("assigneeId"),
                reviewedStatus = payload.optionalControlled("reviewedStatus"),
                reviewSummary = payload["reviewSummary"],
                supportingEvidenceUri = payload.optionalUri("supportingEvidenceUri"),
            )
            val graphUris = OntologyActionGraphUris.forRelease(
                sourceReleaseId = payload.requiredControlled("sourceReleaseId"),
                reasoningRunId = payload.optionalControlled("reasoningRunId"),
                actionAuditReleaseId = payload.requiredControlled("actionAuditReleaseId"),
            )
            val result = actionSubmitter.submit(
                OntologyActionAuditPlan(
                    request = actionRequest,
                    graphs = graphUris,
                ),
            )
            if (result.audited) {
                PrivateSemanticQueryResponse(
                    statusCode = 200,
                    payload = result.toPayload(actionRequest),
                )
            } else {
                PrivateSemanticQueryResponse(
                    statusCode = 400,
                    payload = responseSerializer.error(
                        code = SemanticErrorCode.CONTRACT_VALIDATION_FAILED,
                        message = "Ontology action request failed validation and was not audited.",
                        detail = result.errors.joinToString(separator = "; ").ifBlank {
                            result.validation.errors.joinToString(separator = "; ")
                        },
                    ),
                )
            }
        } catch (error: IllegalArgumentException) {
            error(
                statusCode = 400,
                message = error.message ?: "Ontology action request failed contract validation.",
            )
        } catch (error: RuntimeException) {
            error(
                statusCode = 503,
                code = SemanticErrorCode.GRAPH_UNAVAILABLE,
                message = "Ontology action audit graph is unavailable.",
                detail = error.message,
            )
        }
    }

    private fun handleTransition(payload: PrivateStringPayload): PrivateSemanticQueryResponse {
        val toState = OntologyActionLifecycleState.fromId(payload.required("toState"))
            ?: return error(
                statusCode = 400,
                message = "Unsupported ontology action lifecycle state.",
                detail = payload["toState"],
            )
        val transitionRequest = OntologyActionTransitionRequest(
            transitionId = payload.requiredControlled("transitionId"),
            idempotencyKey = payload.requiredControlled("idempotencyKey"),
            actorId = payload.requiredControlled("actorId"),
            requestedAt = Instant.parse(payload.required("requestedAt")),
            targetExecutionUri = payload.requiredUri("targetExecutionUri"),
            toState = toState,
            transitionReason = payload.required("transitionReason"),
        )
        val graphUris = OntologyActionGraphUris.forRelease(
            sourceReleaseId = payload.requiredControlled("sourceReleaseId"),
            reasoningRunId = payload.optionalControlled("reasoningRunId"),
            actionAuditReleaseId = payload.requiredControlled("actionAuditReleaseId"),
        )
        val result = transitionSubmitter.submit(
            OntologyActionTransitionPlan(
                request = transitionRequest,
                graphs = graphUris,
            ),
        )
        return if (result.transitioned) {
            PrivateSemanticQueryResponse(
                statusCode = 200,
                payload = result.toPayload(transitionRequest),
            )
        } else {
            PrivateSemanticQueryResponse(
                statusCode = 400,
                payload = responseSerializer.error(
                    code = SemanticErrorCode.CONTRACT_VALIDATION_FAILED,
                    message = "Ontology action transition failed validation and was not written.",
                    detail = result.errors.joinToString(separator = "; ").ifBlank {
                        result.validation.errors.joinToString(separator = "; ")
                    },
                ),
            )
        }
    }

    private fun OntologyActionAuditResult.toPayload(request: OntologyActionRequest): Map<String, Any> {
        return buildMap {
            put("resultType", "ontology-action-request")
            put("audited", audited)
            put("actionId", request.actionType.id)
            put("requestId", request.requestId)
            put("idempotencyKey", request.idempotencyKey)
            put("actionAuditGraphUri", actionAuditGraphUri)
            put("writtenGraphUris", writtenGraphUris)
            put("idempotentReplay", idempotentReplay)
            put("notificationStatus", OntologyActionLifecycleState.QUEUED.id)
            put("currentState", OntologyActionLifecycleState.QUEUED.id)
            put("canonicalGraphMutation", false)
            put("reasoningGraphMutation", false)
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
        }
    }

    private fun OntologyActionTransitionResult.toPayload(request: OntologyActionTransitionRequest): Map<String, Any> {
        return buildMap {
            put("resultType", "ontology-action-transition")
            put("transitioned", transitioned)
            put("transitionId", request.transitionId)
            put("idempotencyKey", request.idempotencyKey)
            put("targetExecutionUri", request.targetExecutionUri)
            put("currentState", currentState?.id ?: request.toState.id)
            put("actionAuditGraphUri", actionAuditGraphUri)
            put("writtenGraphUris", writtenGraphUris)
            put("idempotentReplay", idempotentReplay)
            put("canonicalGraphMutation", false)
            put("reasoningGraphMutation", false)
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
        const val ACTION_REQUEST_PATH = "/semantic/internal/action-request"
        const val ACTION_TRANSITION_PATH = "/semantic/internal/action-transition"
        private val ALLOWED_FIELDS = setOf(
            "requestId",
            "actionId",
            "idempotencyKey",
            "actorId",
            "requestedAt",
            "incidentUri",
            "actionReason",
            "sourceRecordUri",
            "restoreReadinessFindingUri",
            "recoveryBlockerUri",
            "trustFindingUri",
            "validationEvidenceUri",
            "assignedTeam",
            "assigneeId",
            "reviewedStatus",
            "reviewSummary",
            "supportingEvidenceUri",
            "sourceReleaseId",
            "reasoningRunId",
            "actionAuditReleaseId",
            "transitionId",
            "targetExecutionUri",
            "toState",
            "transitionReason",
        )
    }
}
