package com.dcai.semanticservice.api

import com.dcai.semanticservice.response.SemanticErrorCode
import com.dcai.semanticservice.response.SemanticResponseSerializer
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

class PrivateSemanticQueryEndpointServer(
    private val endpoint: PrivateSemanticQueryEndpoint,
    private val actionEndpoint: PrivateOntologyActionEndpoint? = null,
    private val aiGovernanceEndpoint: PrivateAiGovernanceEndpoint? = null,
    private val config: PrivateSemanticQueryEndpointServerConfig = PrivateSemanticQueryEndpointServerConfig(),
) : AutoCloseable {
    private val server: HttpServer = HttpServer.create(InetSocketAddress(config.host, config.port), 0)

    val address: InetSocketAddress
        get() = server.address

    fun start(): PrivateSemanticQueryEndpointServer {
        server.createContext("/semantic/query") { exchange -> handle(exchange) }
        actionEndpoint?.let {
            server.createContext(PrivateOntologyActionEndpoint.ACTION_REQUEST_PATH) { exchange -> handleAction(exchange, it) }
            server.createContext(PrivateOntologyActionEndpoint.ACTION_TRANSITION_PATH) { exchange -> handleAction(exchange, it) }
        }
        aiGovernanceEndpoint?.let {
            server.createContext(PrivateAiGovernanceEndpoint.AI_PROPOSAL_REVIEW_PATH) { exchange -> handleAiGovernance(exchange, it) }
        }
        server.executor = null
        server.start()
        return this
    }

    override fun close() {
        server.stop(0)
    }

    private fun handle(exchange: HttpExchange) {
        try {
            exchange.responseHeaders.set("Access-Control-Allow-Origin", config.corsAllowOrigin)
            exchange.responseHeaders.set("Access-Control-Allow-Methods", "POST, OPTIONS")
            exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type")
            if (exchange.requestMethod == "OPTIONS") {
                exchange.sendResponseHeaders(204, -1)
                exchange.close()
                return
            }

            val response = endpoint.handle(
                PrivateSemanticQueryRequest(
                    method = exchange.requestMethod,
                    path = exchange.requestURI.path,
                    body = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() },
                ),
            )
            writeResponse(exchange, response)
        } catch (error: RuntimeException) {
            val response = PrivateSemanticQueryResponse(
                statusCode = 500,
                payload = SemanticResponseSerializer().error(
                    code = SemanticErrorCode.INTERNAL_SEMANTIC_SERVICE_ERROR,
                    message = "Private semantic query endpoint failed before a response could be written.",
                    detail = error.message,
                ),
            )
            writeResponse(exchange, response)
        }
    }

    private fun handleAction(
        exchange: HttpExchange,
        actionEndpoint: PrivateOntologyActionEndpoint,
    ) {
        try {
            exchange.responseHeaders.set("Access-Control-Allow-Origin", config.corsAllowOrigin)
            exchange.responseHeaders.set("Access-Control-Allow-Methods", "POST, OPTIONS")
            exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type")
            if (exchange.requestMethod == "OPTIONS") {
                exchange.sendResponseHeaders(204, -1)
                exchange.close()
                return
            }

            val response = actionEndpoint.handle(
                PrivateSemanticQueryRequest(
                    method = exchange.requestMethod,
                    path = exchange.requestURI.path,
                    body = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() },
                ),
            )
            writeResponse(exchange, response)
        } catch (error: RuntimeException) {
            val response = PrivateSemanticQueryResponse(
                statusCode = 500,
                payload = SemanticResponseSerializer().error(
                    code = SemanticErrorCode.INTERNAL_SEMANTIC_SERVICE_ERROR,
                    message = "Private ontology action endpoint failed before a response could be written.",
                    detail = error.message,
                ),
            )
            writeResponse(exchange, response)
        }
    }

    private fun handleAiGovernance(
        exchange: HttpExchange,
        aiGovernanceEndpoint: PrivateAiGovernanceEndpoint,
    ) {
        try {
            exchange.responseHeaders.set("Access-Control-Allow-Origin", config.corsAllowOrigin)
            exchange.responseHeaders.set("Access-Control-Allow-Methods", "POST, OPTIONS")
            exchange.responseHeaders.set("Access-Control-Allow-Headers", "Content-Type")
            if (exchange.requestMethod == "OPTIONS") {
                exchange.sendResponseHeaders(204, -1)
                exchange.close()
                return
            }

            val response = aiGovernanceEndpoint.handle(
                PrivateSemanticQueryRequest(
                    method = exchange.requestMethod,
                    path = exchange.requestURI.path,
                    body = exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() },
                ),
            )
            writeResponse(exchange, response)
        } catch (error: RuntimeException) {
            val response = PrivateSemanticQueryResponse(
                statusCode = 500,
                payload = SemanticResponseSerializer().error(
                    code = SemanticErrorCode.INTERNAL_SEMANTIC_SERVICE_ERROR,
                    message = "Private AI governance endpoint failed before a response could be written.",
                    detail = error.message,
                ),
            )
            writeResponse(exchange, response)
        }
    }

    private fun writeResponse(exchange: HttpExchange, response: PrivateSemanticQueryResponse) {
        val bytes = response.jsonBody().toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", response.contentType)
        exchange.sendResponseHeaders(response.statusCode, bytes.size.toLong())
        exchange.responseBody.use { output -> output.write(bytes) }
    }
}

data class PrivateSemanticQueryEndpointServerConfig(
    val host: String = "127.0.0.1",
    val port: Int = 18080,
    val corsAllowOrigin: String = "*",
) {
    init {
        require(host == "127.0.0.1" || host == "localhost") {
            "private semantic endpoint must bind to a loopback host"
        }
        require(port in 0..65535) { "port must be between 0 and 65535" }
    }
}
