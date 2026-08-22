package com.dcai.semanticservice.query

import com.dcai.semanticservice.graph.FusekiReadOnlyConfig
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JenaFusekiReadOnlyQueryExecutorPagingTest {
    @Test
    fun executesCountIdentityPageAndBoundedDataQueries() {
        val queries = CopyOnWriteArrayList<String>()
        val server = sparqlServer(queries)
        try {
            val endpoint = "http://127.0.0.1:${server.address.port}/query"
            val executor = JenaFusekiReadOnlyQueryExecutor(
                manifest = manifest("semanticTrustFindingList", TRUST_FINDING_QUERY),
                config = FusekiReadOnlyConfig(datasetUrl = endpoint.removeSuffix("/query"), queryEndpointUrl = endpoint),
            )

            val report = executor.execute(
                queryId = "semanticTrustFindingList",
                parameters = emptyMap(),
                pageRequest = QueryPageRequest(page = 2, pageSize = 2),
            )

            assertEquals(2, report.rows.size)
            assertEquals(QueryPageResult(page = 2, pageSize = 2, totalRecords = 5), report.page)
            assertEquals(3, queries.size)
            assertTrue(queries[0].contains("COUNT(*)"))
            assertTrue(queries[0].contains("SELECT DISTINCT ?trustFindingId"))
            assertTrue(queries[1].contains("LIMIT  2") || queries[1].contains("LIMIT 2"))
            assertTrue(queries[1].contains("OFFSET  2") || queries[1].contains("OFFSET 2"))
            assertTrue(queries[1].contains("ORDER BY"))
            assertTrue(queries[2].contains("VALUES  ?trustFindingId") || queries[2].contains("VALUES ?trustFindingId"))
            assertTrue(queries[2].contains("finding-3"))
            assertTrue(queries[2].contains("finding-4"))
            assertFalse(queries[2].contains("finding-1"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun returnsAnEmptyBoundedPageWithoutExecutingADataQuery() {
        val queries = CopyOnWriteArrayList<String>()
        val server = sparqlServer(queries, emptyIdentityPage = true)
        try {
            val endpoint = "http://127.0.0.1:${server.address.port}/query"
            val executor = JenaFusekiReadOnlyQueryExecutor(
                manifest = manifest("semanticTrustFindingList", TRUST_FINDING_QUERY),
                config = FusekiReadOnlyConfig(datasetUrl = endpoint.removeSuffix("/query"), queryEndpointUrl = endpoint),
            )

            val report = executor.execute(
                queryId = "semanticTrustFindingList",
                parameters = emptyMap(),
                pageRequest = QueryPageRequest(page = 4, pageSize = 2),
            )

            assertEquals(emptyList(), report.rows)
            assertEquals(QueryPageResult(page = 4, pageSize = 2, totalRecords = 5), report.page)
            assertEquals(2, queries.size)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun rejectsPagingWithoutAStableIdentityPolicyBeforeNetworkAccess() {
        val executor = JenaFusekiReadOnlyQueryExecutor(
            manifest = manifest("fixtureNamedGraphInventory", "SELECT ?graph WHERE { GRAPH ?graph { ?s ?p ?o } } ORDER BY ?graph"),
            config = FusekiReadOnlyConfig(datasetUrl = "http://127.0.0.1:1", queryEndpointUrl = "http://127.0.0.1:1/query"),
        )

        assertFailsWith<IllegalArgumentException> {
            executor.execute("fixtureNamedGraphInventory", emptyMap(), QueryPageRequest(page = 1, pageSize = 20))
        }
    }

    private fun sparqlServer(
        queries: MutableList<String>,
        emptyIdentityPage: Boolean = false,
    ): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/query") { exchange ->
            val requestBody = exchange.requestBody.bufferedReader().use { it.readText() }
            val encodedQuery = exchange.requestURI.rawQuery
                ?.split('&')
                ?.firstOrNull { it.startsWith("query=") }
                ?.substringAfter('=')
            val query = when {
                encodedQuery != null -> URLDecoder.decode(encodedQuery, StandardCharsets.UTF_8)
                requestBody.startsWith("query=") -> URLDecoder.decode(requestBody.substringAfter("query=").substringBefore('&'), StandardCharsets.UTF_8)
                else -> requestBody
            }
            queries += query
            val response = when {
                query.contains("COUNT(*)") -> countResponse()
                query.contains("__pageSort0") -> if (emptyIdentityPage) emptyIdentityResponse() else identityResponse()
                else -> dataResponse()
            }.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/sparql-results+json")
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        return server
    }

    private fun countResponse(): String = """
        {"head":{"vars":["totalRecords"]},"results":{"bindings":[
          {"totalRecords":{"type":"literal","datatype":"http://www.w3.org/2001/XMLSchema#integer","value":"5"}}
        ]}}
    """.trimIndent()

    private fun identityResponse(): String = """
        {"head":{"vars":["trustFindingId","__pageSort0"]},"results":{"bindings":[
          {"trustFindingId":{"type":"literal","value":"finding-3"},"__pageSort0":{"type":"literal","value":"2026-06-03T00:00:00Z"}},
          {"trustFindingId":{"type":"literal","value":"finding-4"},"__pageSort0":{"type":"literal","value":"2026-06-02T00:00:00Z"}}
        ]}}
    """.trimIndent()

    private fun emptyIdentityResponse(): String = """
        {"head":{"vars":["trustFindingId","__pageSort0"]},"results":{"bindings":[]}}
    """.trimIndent()

    private fun dataResponse(): String = """
        {"head":{"vars":["graph","trustFinding","summary","sourceFact","createdAt"]},"results":{"bindings":[
          {"graph":{"type":"uri","value":"urn:dcai:graph:reasoning:test"},"trustFinding":{"type":"uri","value":"urn:dcai:test:finding-3"},"summary":{"type":"literal","value":"three"},"sourceFact":{"type":"uri","value":"urn:dcai:test:fact-3"},"createdAt":{"type":"literal","value":"2026-06-03T00:00:00Z"}},
          {"graph":{"type":"uri","value":"urn:dcai:graph:reasoning:test"},"trustFinding":{"type":"uri","value":"urn:dcai:test:finding-4"},"summary":{"type":"literal","value":"four"},"sourceFact":{"type":"uri","value":"urn:dcai:test:fact-4"},"createdAt":{"type":"literal","value":"2026-06-02T00:00:00Z"}}
        ]}}
    """.trimIndent()

    private fun manifest(id: String, sparql: String): ApprovedQueryManifest {
        return ApprovedQueryManifest(
            entries = mapOf(
                id to ApprovedQueryDefinition(
                    id = id,
                    path = Path.of("queries/read-model/$id.rq"),
                    mode = QueryMode.SELECT,
                    graphScope = "test graph",
                    sparql = sparql,
                ),
            ),
        )
    }

    private companion object {
        val TRUST_FINDING_QUERY = """
            PREFIX dcai: <urn:dcai:ontology:>
            SELECT ?graph ?trustFinding ?trustFindingId ?summary ?sourceFact ?createdAt
            WHERE {
              GRAPH ?graph {
                ?trustFinding dcai:hasSummary ?summary ;
                  dcai:hasSourceFact ?sourceFact ;
                  dcai:hasCreatedAt ?createdAt .
              }
              BIND(STRAFTER(STR(?trustFinding), "urn:dcai:test:") AS ?trustFindingId)
            }
            ORDER BY DESC(?createdAt) ?trustFinding
        """.trimIndent()
    }
}
