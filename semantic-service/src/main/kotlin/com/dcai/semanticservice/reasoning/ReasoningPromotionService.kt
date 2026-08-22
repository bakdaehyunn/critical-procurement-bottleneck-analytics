package com.dcai.semanticservice.reasoning

import com.dcai.semanticservice.graph.ControlledIdentifier
import com.dcai.semanticservice.graph.ManagedGraphKind
import com.dcai.semanticservice.graph.ManagedGraphUri
import com.dcai.semanticservice.graph.ManagedGraphWriteCoordinator
import com.dcai.semanticservice.graph.NamedGraphStore
import java.time.Instant
import org.apache.jena.rdf.model.Model

interface ReasoningRefresher {
    fun run(plan: ReasoningPromotionPlan): ReasoningPromotionResult
}

class ReasoningPromotionService(
    private val builder: ReasoningModelBuilder,
    private val validationGate: ReasoningValidationGate,
    private val graphStore: NamedGraphStore,
) : ReasoningRefresher {
    private val graphWrites = ManagedGraphWriteCoordinator(graphStore)

    override fun run(plan: ReasoningPromotionPlan): ReasoningPromotionResult {
        val canonicalSnapshot = graphStore.readNamedGraph(plan.inputGraphs.canonicalGraphUri)
        if (!canonicalSnapshot.exists || canonicalSnapshot.model.isEmpty) {
            return ReasoningPromotionResult(
                promoted = false,
                validation = ReasoningValidationReport(conforms = false, tripleCount = 0, errors = listOf("Canonical graph is missing or empty")),
                errors = listOf("Canonical graph is missing or empty: ${plan.inputGraphs.canonicalGraphUri}"),
            )
        }
        val provenanceSnapshot = graphStore.readNamedGraph(plan.inputGraphs.provenanceGraphUri)

        val output = builder.build(
            ReasoningInput(
                runId = plan.runId,
                generatedAt = plan.generatedAt,
                canonicalModel = canonicalSnapshot.copyModel(),
                provenanceModel = provenanceSnapshot.copyModel(),
            ),
        )
        val validation = validationGate.validate(output.auditModel)
        if (!validation.conforms) {
            return ReasoningPromotionResult(
                promoted = false,
                validation = validation,
                findingCount = output.findingCount,
                errors = validation.errors,
            )
        }

        val graphModels = plan.outputGraphs.models(output)
        val snapshots = runCatching {
            graphWrites.snapshot(graphModels.keys)
        }.getOrElse { error ->
            return ReasoningPromotionResult(
                promoted = false,
                validation = validation,
                findingCount = output.findingCount,
                errors = listOf("Reasoning graph snapshot failed before promotion: ${error.message}"),
            )
        }

        val write = graphWrites.replaceAll(
            graphModels = graphModels,
            snapshots = snapshots,
            writeFailurePrefix = "Reasoning promotion write failed",
            rollbackFailurePrefix = "Reasoning rollback failed",
        )
        return if (write.succeeded) {
            ReasoningPromotionResult(
                promoted = true,
                validation = validation,
                findingCount = output.findingCount,
                writtenGraphUris = write.writtenGraphUris,
                releaseManifest = ReasoningReleaseManifest.from(plan, output.findingCount),
            )
        } else {
            ReasoningPromotionResult(
                promoted = false,
                validation = validation,
                findingCount = output.findingCount,
                writtenGraphUris = write.writtenGraphUris,
                rollbackAttempted = write.rollbackAttempted,
                rollbackSucceeded = write.rollbackSucceeded,
                errors = write.errors,
            )
        }
    }
}

data class ReasoningPromotionPlan(
    val runId: String,
    val generatedAt: Instant,
    val inputGraphs: ReasoningInputGraphUris,
    val outputGraphs: ReasoningOutputGraphUris,
) {
    init {
        ControlledIdentifier.requireRelease(runId, "runId")
    }
}

data class ReasoningInputGraphUris(
    val canonicalGraphUri: String,
    val provenanceGraphUri: String,
) {
    init {
        ManagedGraphUri.requireKind(canonicalGraphUri, ManagedGraphKind.CANONICAL, "canonicalGraphUri")
        ManagedGraphUri.requireKind(provenanceGraphUri, ManagedGraphKind.PROVENANCE, "provenanceGraphUri")
    }

    companion object {
        fun forRelease(releaseId: String): ReasoningInputGraphUris {
            return ReasoningInputGraphUris(
                canonicalGraphUri = ManagedGraphUri.of(ManagedGraphKind.CANONICAL, releaseId, "releaseId").value,
                provenanceGraphUri = ManagedGraphUri.of(ManagedGraphKind.PROVENANCE, releaseId, "releaseId").value,
            )
        }
    }
}

data class ReasoningOutputGraphUris(
    val auditGraphUri: String,
    val reasoningGraphUri: String,
) {
    init {
        ManagedGraphUri.requireKind(auditGraphUri, ManagedGraphKind.REASONING_AUDIT, "auditGraphUri")
        ManagedGraphUri.requireKind(reasoningGraphUri, ManagedGraphKind.REASONING, "reasoningGraphUri")
    }

    fun models(output: ReasoningOutput): LinkedHashMap<String, Model> {
        return linkedMapOf(
            auditGraphUri to output.auditModel,
            reasoningGraphUri to output.reasoningModel,
        )
    }

    companion object {
        fun forRun(runId: String): ReasoningOutputGraphUris {
            return ReasoningOutputGraphUris(
                auditGraphUri = ManagedGraphUri.of(ManagedGraphKind.REASONING_AUDIT, runId, "runId").value,
                reasoningGraphUri = ManagedGraphUri.of(ManagedGraphKind.REASONING, runId, "runId").value,
            )
        }
    }
}

data class ReasoningPromotionResult(
    val promoted: Boolean,
    val validation: ReasoningValidationReport,
    val findingCount: Int = 0,
    val writtenGraphUris: List<String> = emptyList(),
    val rollbackAttempted: Boolean = false,
    val rollbackSucceeded: Boolean = false,
    val releaseManifest: ReasoningReleaseManifest? = null,
    val errors: List<String> = emptyList(),
)

data class ReasoningReleaseManifest(
    val runId: String,
    val canonicalGraphUri: String,
    val provenanceGraphUri: String,
    val auditGraphUri: String,
    val reasoningGraphUri: String,
    val findingCount: Int,
) {
    companion object {
        fun from(plan: ReasoningPromotionPlan, findingCount: Int): ReasoningReleaseManifest {
            return ReasoningReleaseManifest(
                runId = plan.runId,
                canonicalGraphUri = plan.inputGraphs.canonicalGraphUri,
                provenanceGraphUri = plan.inputGraphs.provenanceGraphUri,
                auditGraphUri = plan.outputGraphs.auditGraphUri,
                reasoningGraphUri = plan.outputGraphs.reasoningGraphUri,
                findingCount = findingCount,
            )
        }
    }
}
