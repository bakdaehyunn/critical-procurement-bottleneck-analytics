package com.dcai.semanticservice.api

import com.dcai.semanticservice.actions.OntologyActionAuditService
import com.dcai.semanticservice.actions.OntologyActionPreconditionValidator
import com.dcai.semanticservice.actions.OntologyActionRdfMapper
import com.dcai.semanticservice.actions.OntologyActionTransitionService
import com.dcai.semanticservice.actions.OntologyActionValidationGate
import com.dcai.semanticservice.governance.AiGovernanceProposalValidationGate
import com.dcai.semanticservice.governance.AiGovernanceReviewService
import com.dcai.semanticservice.graph.FusekiGraphStoreConfig
import com.dcai.semanticservice.graph.FusekiNamedGraphWriter
import com.dcai.semanticservice.graph.FusekiReadOnlyConfig
import com.dcai.semanticservice.query.ApprovedQueryCatalog
import com.dcai.semanticservice.query.JenaFusekiReadOnlyQueryExecutor
import com.dcai.semanticservice.query.QueryContractRegistry
import com.dcai.semanticservice.query.QueryResultShaper
import java.nio.file.Path

object PrivateSemanticEndpointComposition {
    fun createServer(
        repoRoot: Path,
        config: PrivateSemanticQueryEndpointServerConfig = PrivateSemanticQueryEndpointServerConfig(),
        fusekiConfig: FusekiReadOnlyConfig = FusekiReadOnlyConfig.fromEnvironment(),
        graphStoreConfig: FusekiGraphStoreConfig = FusekiGraphStoreConfig.fromEnvironment(),
    ): PrivateSemanticQueryEndpointServer {
        val manifest = ApprovedQueryCatalog(repoRoot).load()
        val registry = QueryContractRegistry.fromManifest(manifest, requireCompleteManifest = true)
        val endpoint = PrivateSemanticQueryEndpoint(
            queryExecutor = JenaFusekiReadOnlyQueryExecutor(
                registry = registry,
                config = fusekiConfig,
            ),
            queryResultShaper = QueryResultShaper(registry),
        )
        val actionEndpoint = PrivateOntologyActionEndpoint(
            actionSubmitter = OntologyActionAuditService(
                mapper = OntologyActionRdfMapper(),
                preconditionValidator = OntologyActionPreconditionValidator(),
                validationGate = OntologyActionValidationGate(repoRoot),
                graphStore = FusekiNamedGraphWriter(graphStoreConfig),
            ),
            transitionSubmitter = OntologyActionTransitionService(
                validationGate = OntologyActionValidationGate(repoRoot),
                graphStore = FusekiNamedGraphWriter(graphStoreConfig),
            ),
        )
        val aiGovernanceEndpoint = PrivateAiGovernanceEndpoint(
            reviewSubmitter = AiGovernanceReviewService(
                validationGate = AiGovernanceProposalValidationGate(repoRoot),
                graphStore = FusekiNamedGraphWriter(graphStoreConfig),
                actionSubmitter = OntologyActionAuditService(
                    mapper = OntologyActionRdfMapper(),
                    preconditionValidator = OntologyActionPreconditionValidator(),
                    validationGate = OntologyActionValidationGate(repoRoot),
                    graphStore = FusekiNamedGraphWriter(graphStoreConfig),
                ),
            ),
        )
        return PrivateSemanticQueryEndpointServer(endpoint, actionEndpoint, aiGovernanceEndpoint, config)
    }
}
