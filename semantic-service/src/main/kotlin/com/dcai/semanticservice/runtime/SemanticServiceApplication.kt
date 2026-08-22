package com.dcai.semanticservice.runtime

import java.nio.file.Path
import kotlin.system.exitProcess

object SemanticServiceApplication {
    @JvmStatic
    fun main(args: Array<String>) {
        val options = SemanticServiceCliParser.parse(args)
        val repoRoot = options.repoRoot?.let { Path.of(it).toAbsolutePath().normalize() }
            ?: SemanticServiceComposition.locateRepoRoot()

        if (options.servePrivateQueryEndpoint) {
            PrivateSemanticEndpointLauncher.serve(repoRoot, options)
            return
        }

        val report = SemanticServiceComposition.execute(options, repoRoot)
        SemanticServiceReportPrinter.print(report)
        if (!report.isReady) exitProcess(1)
    }
}
