package com.dcai.semanticservice.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrivateEndpointPayloadTest {
    @Test
    fun detectsRawSparqlAcrossPrivateEndpointPayloads() {
        assertTrue(PrivateEndpointPayload.containsRawSparql("""{"sparql":"SELECT * WHERE { ?s ?p ?o }"}"""))
        assertTrue(PrivateEndpointPayload.containsRawSparql("""{"actionReason":"please delete where stale facts exist"}"""))
    }

    @Test
    fun parsesStringObjectWithControlledIdsAndUris() {
        val payload = PrivateEndpointPayload.stringObject(
            body = """
                {
                  "requestId":"REQ-001",
                  "actorId":"operator:local",
                  "incidentUri":"urn:dcai:incident:INC-001",
                  "reviewSummary":"Line one\nLine two"
                }
            """.trimIndent(),
            allowedFields = setOf("requestId", "actorId", "incidentUri", "reviewSummary", "optionalId"),
            bodyLabel = "Test request",
            fieldLabel = "test request",
        )

        assertEquals("REQ-001", payload.requiredControlled("requestId"))
        assertEquals("operator:local", payload.requiredControlled("actorId"))
        assertEquals("urn:dcai:incident:INC-001", payload.requiredUri("incidentUri"))
        assertEquals("Line one\nLine two", payload["reviewSummary"])
        assertNull(payload.optionalControlled("optionalId"))
    }

    @Test
    fun rejectsUnsupportedFieldsAndNonStringValues() {
        val unsupported = assertFailsWith<IllegalArgumentException> {
            PrivateEndpointPayload.stringObject(
                body = """{"requestId":"REQ-001","rawQuery":"SELECT * WHERE { ?s ?p ?o }"}""",
                allowedFields = setOf("requestId"),
                bodyLabel = "Test request",
                fieldLabel = "test request",
            )
        }
        assertEquals("Unsupported test request field: rawQuery", unsupported.message)

        val nonString = assertFailsWith<IllegalArgumentException> {
            PrivateEndpointPayload.stringObject(
                body = """{"requestId":"REQ-001","count":1}""",
                allowedFields = setOf("requestId", "count"),
                bodyLabel = "Test request",
                fieldLabel = "test request",
            )
        }
        assertEquals("Test request body must contain only string fields.", nonString.message)
    }

    @Test
    fun parsesSemanticQueryParametersOnlyAsSupportedStringNames() {
        assertEquals(
            mapOf("incidentIdParam" to "INC-001", "assetIdParam" to "ASSET-001"),
            PrivateEndpointPayload.parameters("""{"parameters":{"incidentIdParam":"INC-001","assetIdParam":"ASSET-001"}}"""),
        )

        val invalid = assertFailsWith<IllegalArgumentException> {
            PrivateEndpointPayload.parameters("""{"parameters":{"incident-id":"INC-001"}}""")
        }
        assertEquals("Parameters must be a JSON object with string values and supported names.", invalid.message)
    }
}
