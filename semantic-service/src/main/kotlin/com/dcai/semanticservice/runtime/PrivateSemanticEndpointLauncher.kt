package com.dcai.semanticservice.runtime

import com.dcai.semanticservice.api.PrivateSemanticQueryEndpointServer
import com.dcai.semanticservice.api.PrivateSemanticQueryEndpointServerConfig
import com.dcai.semanticservice.api.PrivateSemanticEndpointComposition
import java.nio.file.Path

object PrivateSemanticEndpointLauncher {
    fun serve(
        repoRoot: Path,
        options: SemanticServiceRuntimeOptions,
    ) {
        require(options.servePrivateQueryEndpoint) {
            "Private endpoint launcher requires --serve-private-query-endpoint"
        }
        val server = PrivateSemanticEndpointComposition
            .createServer(
                repoRoot = repoRoot,
                config = PrivateSemanticQueryEndpointServerConfig(
                    host = options.privateEndpointHost,
                    port = options.privateEndpointPort,
                ),
            )
            .start()

        println("DCAI Semantic Service")
        println("mode=private-semantic-query-endpoint")
        println("repoRoot=$repoRoot")
        println("privateEndpointUrl=http://${server.address.hostString}:${server.address.port}/semantic/query/{queryId}")
        println("publicEndpointExposure=false")
        Thread.currentThread().join()
    }
}
