package com.dcai.semanticservice.api

import com.dcai.semanticservice.actions.OntologyActionAuditPlan
import com.dcai.semanticservice.actions.OntologyActionAuditResult
import com.dcai.semanticservice.actions.OntologyActionLifecycleState
import com.dcai.semanticservice.actions.OntologyActionSubmitter
import com.dcai.semanticservice.actions.OntologyActionTransitionPlan
import com.dcai.semanticservice.actions.OntologyActionTransitionResult
import com.dcai.semanticservice.actions.OntologyActionTransitionSubmitter
import com.dcai.semanticservice.actions.OntologyActionType
import com.dcai.semanticservice.actions.OntologyActionValidationReport
import com.dcai.semanticservice.governance.AiGovernanceReviewDecision
import com.dcai.semanticservice.governance.AiGovernanceReviewPlan
import com.dcai.semanticservice.governance.AiGovernanceReviewResult
import com.dcai.semanticservice.governance.AiGovernanceReviewSubmitter
import com.dcai.semanticservice.governance.AiGovernanceValidationReport
import com.dcai.semanticservice.query.ApprovedQueryDefinition
import com.dcai.semanticservice.query.ApprovedQueryManifest
import com.dcai.semanticservice.query.QueryExecutionReport
import com.dcai.semanticservice.query.QueryMode
import com.dcai.semanticservice.query.QueryResultShaper
import com.dcai.semanticservice.query.ReadOnlyQueryExecutor
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PrivateSemanticQueryEndpointTest {
    @Test
    fun returnsSerializedPayloadForApprovedQueryId() {
        val endpoint = endpointWith(
            QueryExecutionReport(
                queryId = "fixtureNamedGraphInventory",
                mode = QueryMode.SELECT,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:canonical:minimal-incident",
                        "subjectCount" to "8",
                    ),
                ),
            ),
        )

        val response = endpoint.handle(post("/semantic/query/fixtureNamedGraphInventory"))

        assertEquals(200, response.statusCode)
        assertEquals("fixtureNamedGraphInventory", response.payload["queryId"])
        assertEquals("named-graph-inventory", response.payload["resultType"])
        assertEquals(1, response.payload["recordCount"])
        assertTrue(response.jsonBody().contains("\"provenance\""))
    }

    @Test
    fun returnsSerializedFollowUpQueuePayloadForApprovedProductReadModel() {
        val endpoint = endpointWith(
            QueryExecutionReport(
                queryId = "semanticFollowUpQueueList",
                mode = QueryMode.SELECT,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:canonical:minimal-incident",
                        "incident" to "urn:dcai:fixture:valid:minimal-incident:inc-0001",
                        "incidentId" to "INC-0001",
                        "asset" to "urn:dcai:fixture:valid:minimal-incident:gpu-rack-row-a",
                        "assetId" to "ASSET-GPU-RACK-ROW-A",
                        "zone" to "urn:dcai:fixture:valid:minimal-incident:zone-a",
                        "zoneId" to "ZONE-A",
                        "stage" to "urn:dcai:fixture:valid:minimal-incident:stage-validation",
                        "stageLabel" to "Validation",
                        "sourceRecord" to "urn:dcai:fixture:valid:minimal-incident:source-record-inc-0001",
                    ),
                ),
            ),
        )

        val response = endpoint.handle(post("/semantic/query/semanticFollowUpQueueList"))

        assertEquals(200, response.statusCode)
        assertEquals("semanticFollowUpQueueList", response.payload["queryId"])
        assertEquals("follow-up-queue", response.payload["resultType"])
        assertEquals(1, response.payload["recordCount"])
        assertTrue(response.jsonBody().contains("\"sourceRecordUri\""))
    }

    @Test
    fun returnsSerializedDashboardOverviewPayloadForApprovedProductReadModel() {
        val endpoint = endpointWith(
            QueryExecutionReport(
                queryId = "semanticDashboardOverview",
                mode = QueryMode.SELECT,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                        "totalIncidents" to "2",
                        "assetCount" to "3",
                        "zoneCount" to "1",
                        "impactObservationCount" to "1",
                        "capacityRiskKw" to "900.0",
                        "affectedGpuCount" to "320",
                        "dependencyEdgeCount" to "1",
                        "trustFindingCount" to "1",
                    ),
                ),
            ),
        )

        val response = endpoint.handle(post("/semantic/query/semanticDashboardOverview"))

        assertEquals(200, response.statusCode)
        assertEquals("semanticDashboardOverview", response.payload["queryId"])
        assertEquals("dashboard-overview", response.payload["resultType"])
        assertTrue(response.jsonBody().contains("\"capacityRiskKw\":900.0"))
    }

    @Test
    fun returnsSerializedActionAuditHistoryPayloadForApprovedReadModel() {
        val executor = CapturingQueryExecutor(
            QueryExecutionReport(
                queryId = "semanticActionAuditHistoryByIncident",
                mode = QueryMode.SELECT,
                rows = listOf(actionAuditHistoryRow()),
            ),
        )
        val endpoint = PrivateSemanticQueryEndpoint(
            queryExecutor = executor,
            queryResultShaper = QueryResultShaper(manifestWith("semanticActionAuditHistoryByIncident")),
        )

        val response = endpoint.handle(
            post(
                path = "/semantic/query/semanticActionAuditHistoryByIncident",
                body = """{"parameters":{"incidentIdParam":"INC-REASONING-0001"}}""",
            ),
        )

        assertEquals(200, response.statusCode)
        assertEquals("semanticActionAuditHistoryByIncident", response.payload["queryId"])
        assertEquals("action-audit-history", response.payload["resultType"])
        assertEquals(mapOf("incidentIdParam" to "INC-REASONING-0001"), executor.lastParameters)
        assertTrue(response.jsonBody().contains("\"idempotencyKey\":\"ack-restore-001\""))
    }

    @Test
    fun returnsSerializedActionAvailabilityPayloadForApprovedReadModel() {
        val executor = CapturingQueryExecutor(
            QueryExecutionReport(
                queryId = "semanticAvailableActionsByFinding",
                mode = QueryMode.SELECT,
                rows = listOf(actionAvailabilityRow()),
            ),
        )
        val endpoint = PrivateSemanticQueryEndpoint(
            queryExecutor = executor,
            queryResultShaper = QueryResultShaper(manifestWith("semanticAvailableActionsByFinding")),
        )

        val response = endpoint.handle(
            post(
                path = "/semantic/query/semanticAvailableActionsByFinding",
                body = """{"parameters":{"incidentIdParam":"INC-REASONING-0001"}}""",
            ),
        )

        assertEquals(200, response.statusCode)
        assertEquals("semanticAvailableActionsByFinding", response.payload["queryId"])
        assertEquals("action-availability", response.payload["resultType"])
        assertEquals(mapOf("incidentIdParam" to "INC-REASONING-0001"), executor.lastParameters)
        assertTrue(response.jsonBody().contains("\"actionId\":\"AcknowledgeRestoreBlocker\""))
    }

    @Test
    fun returnsSerializedOntologyReviewQueuePayloadForApprovedReadModel() {
        val endpoint = endpointWith(
            QueryExecutionReport(
                queryId = "semanticPromotionReviewQueue",
                mode = QueryMode.SELECT,
                rows = listOf(ontologyReviewQueueRow()),
            ),
        )

        val response = endpoint.handle(post("/semantic/query/semanticPromotionReviewQueue"))

        assertEquals(200, response.statusCode)
        assertEquals("semanticPromotionReviewQueue", response.payload["queryId"])
        assertEquals("ontology-review-queue", response.payload["resultType"])
        assertTrue(response.jsonBody().contains("\"reviewActionId\":\"ApprovePromotionBatch\""))
        assertTrue(response.jsonBody().contains("\"actionStatus\":\"DISABLED\""))
    }

    @Test
    fun rejectsUnapprovedQueryIdWithSemanticErrorEnvelope() {
        val response = endpointWith(
            QueryExecutionReport(
                queryId = "fixtureNamedGraphInventory",
                mode = QueryMode.SELECT,
            ),
        ).handle(post("/semantic/query/dependencyExposureReasoning"))

        assertEquals(400, response.statusCode)
        assertErrorCode("unapproved-query-id", response)
    }

    @Test
    fun rejectsRawSparqlRequestBody() {
        val response = endpointWith(
            QueryExecutionReport(
                queryId = "fixtureNamedGraphInventory",
                mode = QueryMode.SELECT,
            ),
        ).handle(
            post(
                path = "/semantic/query/fixtureNamedGraphInventory",
                body = """{"sparql":"SELECT * WHERE { ?s ?p ?o }"}""",
            ),
        )

        assertEquals(400, response.statusCode)
        assertErrorCode("contract-validation-failed", response)
        assertTrue(response.jsonBody().contains("does not accept raw SPARQL"))
    }

    @Test
    fun passesStringParametersToApprovedQueryExecutor() {
        val executor = CapturingQueryExecutor(
            QueryExecutionReport(
                queryId = "semanticFollowUpDetail",
                mode = QueryMode.SELECT,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
                        "incident" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
                        "incidentId" to "INC-REASONING-0001",
                        "asset" to "urn:dcai:fixture:valid:reasoning-output:asset-a",
                        "assetId" to "ASSET-A",
                        "zone" to "urn:dcai:fixture:valid:reasoning-output:zone-a",
                        "zoneId" to "ZONE-A",
                        "stage" to "urn:dcai:fixture:valid:reasoning-output:stage-waiting",
                        "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
                    ),
                ),
            ),
        )
        val endpoint = PrivateSemanticQueryEndpoint(
            queryExecutor = executor,
            queryResultShaper = QueryResultShaper(manifestWith("semanticFollowUpDetail")),
        )

        val response = endpoint.handle(
            post(
                path = "/semantic/query/semanticFollowUpDetail",
                body = """{"parameters":{"incidentIdParam":"INC-REASONING-0001"}}""",
            ),
        )

        assertEquals(200, response.statusCode)
        assertEquals(mapOf("incidentIdParam" to "INC-REASONING-0001"), executor.lastParameters)
    }

    @Test
    fun rejectsMalformedParametersWithoutExecutingQuery() {
        val executor = CapturingQueryExecutor(
            QueryExecutionReport(
                queryId = "semanticFollowUpDetail",
                mode = QueryMode.SELECT,
            ),
        )
        val endpoint = PrivateSemanticQueryEndpoint(
            queryExecutor = executor,
            queryResultShaper = QueryResultShaper(manifestWith("semanticFollowUpDetail")),
        )

        val response = endpoint.handle(
            post(
                path = "/semantic/query/semanticFollowUpDetail",
                body = """{"parameters":{"incidentIdParam":42}}""",
            ),
        )

        assertEquals(400, response.statusCode)
        assertErrorCode("contract-validation-failed", response)
        assertEquals(emptyMap(), executor.lastParameters)
    }

    @Test
    fun rejectsCompactRawSparqlRequestBody() {
        val response = endpointWith(
            QueryExecutionReport(
                queryId = "fixtureNamedGraphInventory",
                mode = QueryMode.SELECT,
            ),
        ).handle(
            post(
                path = "/semantic/query/fixtureNamedGraphInventory",
                body = "PREFIX dcai:<urn:dcai:> SELECT?s WHERE{?s ?p ?o}",
            ),
        )

        assertEquals(400, response.statusCode)
        assertErrorCode("contract-validation-failed", response)
    }

    @Test
    fun mapsMissingRequiredBindingToSemanticErrorEnvelope() {
        val response = endpointWith(
            QueryExecutionReport(
                queryId = "fixtureNamedGraphInventory",
                mode = QueryMode.SELECT,
                rows = listOf(mapOf("graph" to "urn:dcai:graph:fixture:canonical:minimal-incident")),
            ),
        ).handle(post("/semantic/query/fixtureNamedGraphInventory"))

        assertEquals(400, response.statusCode)
        assertErrorCode("missing-required-binding", response)
    }

    @Test
    fun mapsUnsupportedEnvelopeToSemanticErrorEnvelope() {
        val manifest = manifestWith("unsupported")
        val endpoint = PrivateSemanticQueryEndpoint(
            queryExecutor = StaticQueryExecutor(
                QueryExecutionReport(
                    queryId = "unsupported",
                    mode = QueryMode.SELECT,
                ),
            ),
            queryResultShaper = QueryResultShaper(manifest),
            allowedQueryIds = setOf("unsupported"),
        )

        val response = endpoint.handle(post("/semantic/query/unsupported"))

        assertEquals(500, response.statusCode)
        assertErrorCode("unsupported-result-envelope", response)
    }

    @Test
    fun mapsGraphFailureToUnavailableSemanticErrorEnvelope() {
        val endpoint = PrivateSemanticQueryEndpoint(
            queryExecutor = FailingQueryExecutor(RuntimeException("Connection refused")),
            queryResultShaper = QueryResultShaper(manifestWith("fixtureNamedGraphInventory")),
        )

        val response = endpoint.handle(post("/semantic/query/fixtureNamedGraphInventory"))

        assertEquals(503, response.statusCode)
        assertErrorCode("graph-unavailable", response)
    }

    @Test
    fun rejectsNonPostMethods() {
        val response = endpointWith(
            QueryExecutionReport(
                queryId = "fixtureNamedGraphInventory",
                mode = QueryMode.SELECT,
            ),
        ).handle(
            PrivateSemanticQueryRequest(
                method = "GET",
                path = "/semantic/query/fixtureNamedGraphInventory",
            ),
        )

        assertEquals(405, response.statusCode)
        assertErrorCode("contract-validation-failed", response)
    }

    @Test
    fun servesApprovedQueryOnLoopbackHttpBoundary() {
        val endpoint = endpointWith(
            QueryExecutionReport(
                queryId = "fixtureNamedGraphInventory",
                mode = QueryMode.SELECT,
                rows = listOf(
                    mapOf(
                        "graph" to "urn:dcai:graph:fixture:source:minimal-incident",
                        "subjectCount" to "4",
                    ),
                ),
            ),
        )

        PrivateSemanticQueryEndpointServer(
            endpoint = endpoint,
            config = PrivateSemanticQueryEndpointServerConfig(port = 0),
        ).use { server ->
            server.start()
            val response = HttpClient.newHttpClient().send(
                HttpRequest
                    .newBuilder(URI.create("http://127.0.0.1:${server.address.port}/semantic/query/fixtureNamedGraphInventory"))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(),
                HttpResponse.BodyHandlers.ofString(),
            )

            assertEquals(200, response.statusCode())
            assertTrue(response.body().contains("\"queryId\":\"fixtureNamedGraphInventory\""))
            assertTrue(response.body().contains("\"resultType\":\"named-graph-inventory\""))
        }
    }

    @Test
    fun servesCorsPreflightOnLoopbackHttpBoundary() {
        val endpoint = endpointWith(
            QueryExecutionReport(
                queryId = "fixtureNamedGraphInventory",
                mode = QueryMode.SELECT,
            ),
        )

        PrivateSemanticQueryEndpointServer(
            endpoint = endpoint,
            config = PrivateSemanticQueryEndpointServerConfig(port = 0),
        ).use { server ->
            server.start()
            val response = HttpClient.newHttpClient().send(
                HttpRequest
                    .newBuilder(URI.create("http://127.0.0.1:${server.address.port}/semantic/query/fixtureNamedGraphInventory"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .build(),
                HttpResponse.BodyHandlers.ofString(),
            )

            assertEquals(204, response.statusCode())
            assertEquals("*", response.headers().firstValue("Access-Control-Allow-Origin").orElse(""))
            assertTrue(response.headers().firstValue("Access-Control-Allow-Methods").orElse("").contains("POST"))
            assertTrue(response.body().isBlank())
        }
    }

    @Test
    fun serverConfigRejectsNonLoopbackHosts() {
        assertFailsWith<IllegalArgumentException> {
            PrivateSemanticQueryEndpointServerConfig(host = "0.0.0.0")
        }
    }

    @Test
    fun jsonWriterEscapesStrings() {
        assertEquals(
            """{"message":"quote: \" and newline\n"}""",
            JsonPayloadWriter.write(mapOf("message" to "quote: \" and newline\n")),
        )
    }

    @Test
    fun privateOntologyActionEndpointSubmitsControlledActionRequest() {
        val submitter = CapturingOntologyActionSubmitter(
            OntologyActionAuditResult(
                audited = true,
                validation = OntologyActionValidationReport(conforms = true, tripleCount = 22),
                actionAuditGraphUri = "urn:dcai:graph:action-audit:local-action-audit-v1",
                writtenGraphUris = listOf("urn:dcai:graph:action-audit:local-action-audit-v1"),
            ),
        )
        val endpoint = PrivateOntologyActionEndpoint(submitter, CapturingOntologyActionTransitionSubmitter(successfulTransitionResult()))

        val response = endpoint.handle(
            post(
                path = PrivateOntologyActionEndpoint.ACTION_REQUEST_PATH,
                body = validActionRequestBody(),
            ),
        )

        assertEquals(200, response.statusCode)
        assertEquals("ontology-action-request", response.payload["resultType"])
        assertEquals("QUEUED", response.payload["notificationStatus"])
        assertEquals("QUEUED", response.payload["currentState"])
        assertEquals(false, response.payload["externalSystemMutation"])
        assertEquals("AcknowledgeRestoreBlocker", submitter.lastPlan!!.request.actionType.id)
        assertEquals("urn:dcai:graph:canonical:local-controlled-source-v1", submitter.lastPlan!!.graphs.canonicalGraphUri)
        assertEquals("urn:dcai:graph:action-audit:local-action-audit-v1", submitter.lastPlan!!.graphs.actionAuditGraphUri)
    }

    @Test
    fun privateOntologyActionEndpointRejectsRawSparqlPayloads() {
        val endpoint = PrivateOntologyActionEndpoint(
            CapturingOntologyActionSubmitter(
                OntologyActionAuditResult(
                    audited = true,
                    validation = OntologyActionValidationReport(conforms = true),
                    actionAuditGraphUri = "urn:dcai:graph:action-audit:local-action-audit-v1",
                ),
            ),
            CapturingOntologyActionTransitionSubmitter(successfulTransitionResult()),
        )

        val response = endpoint.handle(
            post(
                path = PrivateOntologyActionEndpoint.ACTION_REQUEST_PATH,
                body = """{"query":"SELECT * WHERE { ?s ?p ?o }"}""",
            ),
        )

        assertEquals(400, response.statusCode)
        assertErrorCode("contract-validation-failed", response)
        assertTrue(response.jsonBody().contains("does not accept raw SPARQL"))
    }

    @Test
    fun privateOntologyActionEndpointReturnsValidationErrorWithoutAudit() {
        val endpoint = PrivateOntologyActionEndpoint(
            CapturingOntologyActionSubmitter(
                OntologyActionAuditResult(
                    audited = false,
                    validation = OntologyActionValidationReport(
                        conforms = false,
                        errors = listOf("Incident target is missing from canonical graph"),
                    ),
                    actionAuditGraphUri = "urn:dcai:graph:action-audit:local-action-audit-v1",
                    errors = listOf("Incident target is missing from canonical graph"),
                ),
            ),
            CapturingOntologyActionTransitionSubmitter(successfulTransitionResult()),
        )

        val response = endpoint.handle(
            post(
                path = PrivateOntologyActionEndpoint.ACTION_REQUEST_PATH,
                body = validActionRequestBody(),
            ),
        )

        assertEquals(400, response.statusCode)
        assertErrorCode("contract-validation-failed", response)
        assertTrue(response.jsonBody().contains("was not audited"))
    }

    @Test
    fun privateOntologyActionEndpointSubmitsControlledActionTransition() {
        val submitter = CapturingOntologyActionTransitionSubmitter(successfulTransitionResult())
        val endpoint = PrivateOntologyActionEndpoint(
            CapturingOntologyActionSubmitter(successfulActionAuditResult()),
            submitter,
        )

        val response = endpoint.handle(
            post(
                path = PrivateOntologyActionEndpoint.ACTION_TRANSITION_PATH,
                body = validActionTransitionBody(),
            ),
        )

        assertEquals(200, response.statusCode)
        assertEquals("ontology-action-transition", response.payload["resultType"])
        assertEquals("IN_REVIEW", response.payload["currentState"])
        assertEquals(false, response.payload["externalSystemMutation"])
        assertEquals("urn:dcai:ontology-action-execution:local-action-audit-v1%3Aacknowledge%3AINC-001", submitter.lastPlan!!.request.targetExecutionUri)
        assertEquals(OntologyActionLifecycleState.IN_REVIEW, submitter.lastPlan!!.request.toState)
        assertEquals("urn:dcai:graph:action-audit:local-action-audit-v1", submitter.lastPlan!!.graphs.actionAuditGraphUri)
    }

    @Test
    fun privateOntologyActionEndpointReturnsValidationErrorWithoutTransitionWrite() {
        val endpoint = PrivateOntologyActionEndpoint(
            CapturingOntologyActionSubmitter(successfulActionAuditResult()),
            CapturingOntologyActionTransitionSubmitter(
                OntologyActionTransitionResult(
                    transitioned = false,
                    validation = OntologyActionValidationReport(
                        conforms = false,
                        errors = listOf("Invalid ontology action lifecycle transition: QUEUED -> CLOSED"),
                    ),
                    actionAuditGraphUri = "urn:dcai:graph:action-audit:local-action-audit-v1",
                    currentState = OntologyActionLifecycleState.QUEUED,
                    errors = listOf("Invalid ontology action lifecycle transition: QUEUED -> CLOSED"),
                ),
            ),
        )

        val response = endpoint.handle(
            post(
                path = PrivateOntologyActionEndpoint.ACTION_TRANSITION_PATH,
                body = validActionTransitionBody().replace("\"toState\":\"IN_REVIEW\"", "\"toState\":\"CLOSED\""),
            ),
        )

        assertEquals(400, response.statusCode)
        assertErrorCode("contract-validation-failed", response)
        assertTrue(response.jsonBody().contains("was not written"))
    }

    @Test
    fun privateAiGovernanceEndpointSubmitsControlledProposalReview() {
        val submitter = CapturingAiGovernanceReviewSubmitter(
            AiGovernanceReviewResult(
                reviewed = true,
                decision = AiGovernanceReviewDecision.APPROVE,
                validation = AiGovernanceValidationReport(conforms = true, tripleCount = 42),
                aiAuditGraphUri = "urn:dcai:graph:ai-audit:local-ai-governance-v1",
                actionAuditGraphUri = "urn:dcai:graph:action-audit:local-action-audit-v1",
                writtenGraphUris = listOf(
                    "urn:dcai:graph:ai-audit:local-ai-governance-v1",
                    "urn:dcai:graph:action-audit:local-action-audit-v1",
                ),
                actionRequestCreated = true,
                actionRequestId = "AI-REV-LOCAL-001:action-request",
                actionId = "AcknowledgeRestoreBlocker",
            ),
        )
        val endpoint = PrivateAiGovernanceEndpoint(submitter)

        val response = endpoint.handle(
            post(
                path = PrivateAiGovernanceEndpoint.AI_PROPOSAL_REVIEW_PATH,
                body = validAiProposalReviewBody(),
            ),
        )

        assertEquals(200, response.statusCode)
        assertEquals("ai-proposal-review", response.payload["resultType"])
        assertEquals(true, response.payload["actionRequestCreated"])
        assertEquals(false, response.payload["externalSystemMutation"])
        assertEquals("urn:dcai:graph:ai-audit:local-ai-governance-v1", submitter.lastPlan!!.graphs.aiAuditGraphUri)
        assertEquals("urn:dcai:graph:action-audit:local-action-audit-v1", submitter.lastPlan!!.graphs.actionAuditGraphUri)
        assertEquals(AiGovernanceReviewDecision.APPROVE, submitter.lastPlan!!.request.decision)
        assertEquals(OntologyActionType.ACKNOWLEDGE_RESTORE_BLOCKER, submitter.lastPlan!!.request.actionType)
    }

    @Test
    fun privateAiGovernanceEndpointRejectsRawSparqlPayloads() {
        val endpoint = PrivateAiGovernanceEndpoint(
            CapturingAiGovernanceReviewSubmitter(
                AiGovernanceReviewResult(
                    reviewed = true,
                    decision = AiGovernanceReviewDecision.REJECT,
                    validation = AiGovernanceValidationReport(conforms = true),
                    aiAuditGraphUri = "urn:dcai:graph:ai-audit:local-ai-governance-v1",
                ),
            ),
        )

        val response = endpoint.handle(
            post(
                path = PrivateAiGovernanceEndpoint.AI_PROPOSAL_REVIEW_PATH,
                body = """{"query":"SELECT * WHERE { ?s ?p ?o }"}""",
            ),
        )

        assertEquals(400, response.statusCode)
        assertErrorCode("contract-validation-failed", response)
        assertTrue(response.jsonBody().contains("does not accept raw SPARQL"))
    }

    private fun endpointWith(report: QueryExecutionReport): PrivateSemanticQueryEndpoint {
        return PrivateSemanticQueryEndpoint(
            queryExecutor = StaticQueryExecutor(report),
            queryResultShaper = QueryResultShaper(manifestWith(report.queryId)),
        )
    }

    private fun post(
        path: String,
        body: String = "",
    ): PrivateSemanticQueryRequest {
        return PrivateSemanticQueryRequest(
            method = "POST",
            path = path,
            body = body,
        )
    }

    private fun manifestWith(vararg queryIds: String): ApprovedQueryManifest {
        return ApprovedQueryManifest(
            entries = queryIds.associateWith { queryId ->
                ApprovedQueryDefinition(
                    id = queryId,
                    path = Path.of("queries/inspection/$queryId.select.rq"),
                    mode = QueryMode.SELECT,
                    graphScope = "fixture graph",
                    sparql = "SELECT * WHERE { ?s ?p ?o }",
                )
            },
        )
    }

    private fun assertErrorCode(
        expected: String,
        response: PrivateSemanticQueryResponse,
    ) {
        val error = response.payload["error"] as Map<*, *>
        assertEquals(expected, error["code"])
    }

    private fun validActionRequestBody(): String {
        return """
            {
              "requestId":"ACT-REQ-ACK-001",
              "actionId":"AcknowledgeRestoreBlocker",
              "idempotencyKey":"local-action-audit-v1:AcknowledgeRestoreBlocker:INC-001",
              "actorId":"operator-local-reviewer",
              "requestedAt":"2026-06-14T10:15:30Z",
              "incidentUri":"urn:dcai:incident:INC-001",
              "actionReason":"Operator reviewed restore blocker.",
              "sourceRecordUri":"urn:dcai:source-record:system:SRC-INC-001",
              "restoreReadinessFindingUri":"urn:dcai:reasoning:restore-readiness:INC-001",
              "sourceReleaseId":"local-controlled-source-v1",
              "reasoningRunId":"local-controlled-reasoning-v1",
              "actionAuditReleaseId":"local-action-audit-v1"
            }
        """.trimIndent()
    }

    private fun validActionTransitionBody(): String {
        return """
            {
              "transitionId":"ACT-TRN-REVIEW-001",
              "idempotencyKey":"local-action-audit-v1:transition:review-start:INC-001",
              "actorId":"operator-local-reviewer",
              "requestedAt":"2026-06-14T10:20:30Z",
              "targetExecutionUri":"urn:dcai:ontology-action-execution:local-action-audit-v1%3Aacknowledge%3AINC-001",
              "toState":"IN_REVIEW",
              "transitionReason":"Local reviewer started internal action review.",
              "sourceReleaseId":"local-controlled-source-v1",
              "reasoningRunId":"local-controlled-reasoning-v1",
              "actionAuditReleaseId":"local-action-audit-v1"
            }
        """.trimIndent()
    }

    private fun validAiProposalReviewBody(): String {
        return """
            {
              "reviewId":"AI-REV-LOCAL-001",
              "idempotencyKey":"local-ai-review-v1:approve:AI-PROP-LOCAL-001",
              "actorId":"operator-local-reviewer",
              "reviewedAt":"2026-06-09T03:00:00Z",
              "proposalUri":"urn:dcai:ai-proposal:local-ai-governance-v1%3Aaction-recommendation%3AINC-001",
              "decision":"APPROVE",
              "reviewReason":"Human reviewer accepted the AI recommendation.",
              "actionId":"AcknowledgeRestoreBlocker",
              "sourceReleaseId":"local-controlled-source-v1",
              "reasoningRunId":"local-controlled-reasoning-v1",
              "aiAuditReleaseId":"local-ai-governance-v1",
              "actionAuditReleaseId":"local-action-audit-v1"
            }
        """.trimIndent()
    }

    private fun successfulActionAuditResult(): OntologyActionAuditResult {
        return OntologyActionAuditResult(
            audited = true,
            validation = OntologyActionValidationReport(conforms = true, tripleCount = 22),
            actionAuditGraphUri = "urn:dcai:graph:action-audit:local-action-audit-v1",
            writtenGraphUris = listOf("urn:dcai:graph:action-audit:local-action-audit-v1"),
        )
    }

    private fun successfulTransitionResult(): OntologyActionTransitionResult {
        return OntologyActionTransitionResult(
            transitioned = true,
            validation = OntologyActionValidationReport(conforms = true, tripleCount = 30),
            actionAuditGraphUri = "urn:dcai:graph:action-audit:local-action-audit-v1",
            currentState = OntologyActionLifecycleState.IN_REVIEW,
            writtenGraphUris = listOf("urn:dcai:graph:action-audit:local-action-audit-v1"),
        )
    }

    private class CapturingOntologyActionSubmitter(
        private val result: OntologyActionAuditResult,
    ) : OntologyActionSubmitter {
        var lastPlan: OntologyActionAuditPlan? = null

        override fun submit(plan: OntologyActionAuditPlan): OntologyActionAuditResult {
            lastPlan = plan
            return result
        }
    }

    private class CapturingOntologyActionTransitionSubmitter(
        private val result: OntologyActionTransitionResult,
    ) : OntologyActionTransitionSubmitter {
        var lastPlan: OntologyActionTransitionPlan? = null

        override fun submit(plan: OntologyActionTransitionPlan): OntologyActionTransitionResult {
            lastPlan = plan
            return result
        }
    }

    private class CapturingAiGovernanceReviewSubmitter(
        private val result: AiGovernanceReviewResult,
    ) : AiGovernanceReviewSubmitter {
        var lastPlan: AiGovernanceReviewPlan? = null

        override fun submit(plan: AiGovernanceReviewPlan): AiGovernanceReviewResult {
            lastPlan = plan
            return result
        }
    }

    private fun actionAuditHistoryRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:action-audit:local-action-audit-v1",
            "actionAuditReleaseId" to "local-action-audit-v1",
            "execution" to "urn:dcai:ontology-action-execution:ack-restore-001",
            "executionId" to "ack-restore-001",
            "request" to "urn:dcai:ontology-action-request:ack-restore-001",
            "requestId" to "REQ-ACTION-001",
            "validationReport" to "urn:dcai:action-validation-report:ack-restore-001",
            "actionType" to "urn:dcai:ontology-action-type:AcknowledgeRestoreBlocker",
            "actionTypeId" to "AcknowledgeRestoreBlocker",
            "idempotencyKey" to "ack-restore-001",
            "actorId" to "operator-001",
            "actionReason" to "Reviewed restore blocker before shift handoff.",
            "actionStatus" to "QUEUED",
            "requestedAt" to "2026-06-14T10:15:30Z",
            "executedAt" to "2026-06-14T10:15:30Z",
            "targetObject" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
            "validationStatus" to "CONFORMS",
        )
    }

    private fun actionAvailabilityRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:fixture:canonical:reasoning-output",
            "incident" to "urn:dcai:fixture:valid:reasoning-output:incident-0001",
            "incidentId" to "INC-REASONING-0001",
            "asset" to "urn:dcai:fixture:valid:reasoning-output:asset-a",
            "assetId" to "ASSET-A",
            "sourceRecord" to "urn:dcai:fixture:valid:reasoning-output:source-record-0001",
            "actionId" to "AcknowledgeRestoreBlocker",
            "actionLabel" to "Acknowledge restore blocker",
            "actionDescription" to "Record that an operator reviewed the restore-readiness blocker.",
            "actionStatus" to "DISABLED",
            "uiPlacement" to "summary",
            "detailKind" to "targetObject",
            "detailRole" to "RestoreReadinessFinding",
            "detailLabel" to "Restore is not ready.",
            "detailValue" to "urn:dcai:fixture:valid:reasoning-output:restore-readiness-0001",
            "detailSortOrder" to "100",
        )
    }

    private fun ontologyReviewQueueRow(): Map<String, String> {
        return mapOf(
            "graph" to "urn:dcai:graph:canonical:local-controlled-source-v1",
            "queueId" to "promotion-batch:local-controlled-source-v1",
            "queueKind" to "promotion-batch",
            "reviewActionId" to "ApprovePromotionBatch",
            "reviewActionLabel" to "Review promotion batch",
            "reviewStatus" to "READ_ONLY_REVIEW",
            "targetUri" to "urn:dcai:graph:canonical:local-controlled-source-v1",
            "targetType" to "CanonicalGraphRelease",
            "targetLabel" to "local-controlled-source-v1",
            "releaseId" to "local-controlled-source-v1",
            "sourceGraph" to "urn:dcai:graph:source:local-controlled-source-v1",
            "canonicalGraph" to "urn:dcai:graph:canonical:local-controlled-source-v1",
            "provenanceGraph" to "urn:dcai:graph:provenance:local-controlled-source-v1",
            "evidenceSummary" to "Canonical/source/provenance graph batch is available for lifecycle review.",
            "actionStatus" to "DISABLED",
            "disabledReason" to "Approval remains internal-only.",
            "incidentCount" to "4",
            "assetCount" to "6",
            "sourceRecordCount" to "24",
            "activityCount" to "1",
            "generatedFactCount" to "18",
            "prioritySortOrder" to "200",
        )
    }

    private class StaticQueryExecutor(
        private val report: QueryExecutionReport,
    ) : ReadOnlyQueryExecutor {
        override fun execute(queryId: String): QueryExecutionReport {
            return report.copy(queryId = queryId)
        }
    }

    private class FailingQueryExecutor(
        private val error: RuntimeException,
    ) : ReadOnlyQueryExecutor {
        override fun execute(queryId: String): QueryExecutionReport {
            throw error
        }
    }

    private class CapturingQueryExecutor(
        private val report: QueryExecutionReport,
    ) : ReadOnlyQueryExecutor {
        var lastParameters: Map<String, String> = emptyMap()

        override fun execute(queryId: String): QueryExecutionReport {
            return execute(queryId, emptyMap())
        }

        override fun execute(
            queryId: String,
            parameters: Map<String, String>,
        ): QueryExecutionReport {
            lastParameters = parameters
            return report.copy(queryId = queryId)
        }
    }
}
