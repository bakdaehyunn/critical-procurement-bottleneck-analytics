package com.dcai.semanticservice.promotion

import com.dcai.semanticservice.graph.ManagedGraphWriteCoordinator
import com.dcai.semanticservice.graph.NamedGraphStore
import com.dcai.semanticservice.ingestion.SourceExtractBatch
import com.dcai.semanticservice.ingestion.SourceExtractRdfMapper
import com.dcai.semanticservice.ingestion.SourceExtractRdfMapping
import org.apache.jena.rdf.model.Model

interface SourceGraphPromoter {
    fun promote(plan: ProductionGraphPromotionPlan): GraphPromotionResult
}

class GraphPromotionService(
    private val mapper: SourceExtractRdfMapper,
    private val validationGate: ProductionGraphValidationGate,
    private val graphStore: NamedGraphStore,
) : SourceGraphPromoter {
    private val graphWrites = ManagedGraphWriteCoordinator(graphStore)

    override fun promote(plan: ProductionGraphPromotionPlan): GraphPromotionResult {
        val mapping = mapper.map(plan.batch)
        val validation = validationGate.validate(mapping.combinedValidationModel())
        if (!validation.conforms) {
            return GraphPromotionResult(
                promoted = false,
                validation = validation,
                errors = validation.errors,
            )
        }

        val graphModels = plan.graphs.models(mapping)
        val snapshots = runCatching {
            graphWrites.snapshot(graphModels.keys)
        }.getOrElse { error ->
            return GraphPromotionResult(
                promoted = false,
                validation = validation,
                errors = listOf("Graph snapshot failed before promotion: ${error.message}"),
            )
        }

        val write = graphWrites.replaceAll(
            graphModels = graphModels,
            snapshots = snapshots,
            writeFailurePrefix = "Promotion write failed",
            rollbackFailurePrefix = "Rollback failed",
        )
        return if (write.succeeded) {
            GraphPromotionResult(
                promoted = true,
                validation = validation,
                writtenGraphUris = write.writtenGraphUris,
                releaseManifest = PromotionReleaseManifest.from(plan),
            )
        } else {
            GraphPromotionResult(
                promoted = false,
                validation = validation,
                writtenGraphUris = write.writtenGraphUris,
                rollbackAttempted = write.rollbackAttempted,
                rollbackSucceeded = write.rollbackSucceeded,
                errors = write.errors,
            )
        }
    }
}

data class ProductionGraphPromotionPlan(
    val batch: SourceExtractBatch,
    val graphs: ProductionGraphUris,
)

data class ProductionGraphUris(
    val sourceGraphUri: String,
    val canonicalGraphUri: String,
    val provenanceGraphUri: String,
) {
    init {
        require(sourceGraphUri.startsWith(SOURCE_PREFIX)) {
            "sourceGraphUri must use $SOURCE_PREFIX"
        }
        require(sourceGraphUri.length > SOURCE_PREFIX.length) {
            "sourceGraphUri must include a release-specific suffix"
        }
        require(canonicalGraphUri.startsWith(CANONICAL_PREFIX)) {
            "canonicalGraphUri must use $CANONICAL_PREFIX"
        }
        require(canonicalGraphUri.length > CANONICAL_PREFIX.length) {
            "canonicalGraphUri must include a release-specific suffix"
        }
        require(provenanceGraphUri.startsWith(PROVENANCE_PREFIX)) {
            "provenanceGraphUri must use $PROVENANCE_PREFIX"
        }
        require(provenanceGraphUri.length > PROVENANCE_PREFIX.length) {
            "provenanceGraphUri must include a release-specific suffix"
        }
    }

    fun models(mapping: SourceExtractRdfMapping): LinkedHashMap<String, Model> {
        return linkedMapOf(
            sourceGraphUri to mapping.sourceModel,
            canonicalGraphUri to mapping.canonicalModel,
            provenanceGraphUri to mapping.provenanceModel,
        )
    }

    companion object {
        const val SOURCE_PREFIX = "urn:dcai:graph:source:"
        const val CANONICAL_PREFIX = "urn:dcai:graph:canonical:"
        const val PROVENANCE_PREFIX = "urn:dcai:graph:provenance:"

        fun forRelease(releaseId: String): ProductionGraphUris {
            require(releaseId.matches(Regex("[A-Za-z0-9._-]+"))) {
                "releaseId must contain only letters, numbers, dot, underscore, or hyphen"
            }
            return ProductionGraphUris(
                sourceGraphUri = "$SOURCE_PREFIX$releaseId",
                canonicalGraphUri = "$CANONICAL_PREFIX$releaseId",
                provenanceGraphUri = "$PROVENANCE_PREFIX$releaseId",
            )
        }
    }
}

data class GraphPromotionResult(
    val promoted: Boolean,
    val validation: ProductionGraphValidationReport,
    val writtenGraphUris: List<String> = emptyList(),
    val rollbackAttempted: Boolean = false,
    val rollbackSucceeded: Boolean = false,
    val releaseManifest: PromotionReleaseManifest? = null,
    val errors: List<String> = emptyList(),
)

data class PromotionReleaseManifest(
    val releaseId: String,
    val sourceGraphUri: String,
    val canonicalGraphUri: String,
    val provenanceGraphUri: String,
) {
    companion object {
        fun from(plan: ProductionGraphPromotionPlan): PromotionReleaseManifest {
            return PromotionReleaseManifest(
                releaseId = plan.batch.batchId,
                sourceGraphUri = plan.graphs.sourceGraphUri,
                canonicalGraphUri = plan.graphs.canonicalGraphUri,
                provenanceGraphUri = plan.graphs.provenanceGraphUri,
            )
        }
    }
}
