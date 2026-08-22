package com.dcai.semanticservice.dynamic

import com.dcai.semanticservice.actions.OntologyActionGraphUris
import com.dcai.semanticservice.graph.ControlledIdentifier
import com.dcai.semanticservice.graph.ManagedGraphWriteCoordinator
import com.dcai.semanticservice.graph.NamedGraphStore
import com.dcai.semanticservice.ontology.Dcai
import com.dcai.semanticservice.ontology.Prov
import com.dcai.semanticservice.ingestion.SourceExtractBatch
import com.dcai.semanticservice.promotion.GraphPromotionResult
import com.dcai.semanticservice.promotion.ProductionGraphPromotionPlan
import com.dcai.semanticservice.promotion.ProductionGraphUris
import com.dcai.semanticservice.promotion.SourceGraphPromoter
import com.dcai.semanticservice.reasoning.ReasoningInputGraphUris
import com.dcai.semanticservice.reasoning.ReasoningOutputGraphUris
import com.dcai.semanticservice.reasoning.ReasoningPromotionPlan
import com.dcai.semanticservice.reasoning.ReasoningPromotionResult
import com.dcai.semanticservice.reasoning.ReasoningRefresher
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.RDF

interface DynamicPlaybackRunner {
    fun run(plan: DynamicPlaybackPlan): DynamicPlaybackResult
}

class DynamicPlaybackService(
    private val sourcePromoter: SourceGraphPromoter,
    private val reasoningRefresher: ReasoningRefresher,
    private val mapper: DynamicPlaybackRdfMapper,
    private val validationGate: DynamicPlaybackValidationGate,
    private val graphStore: NamedGraphStore,
) : DynamicPlaybackRunner {
    private val graphWrites = ManagedGraphWriteCoordinator(graphStore)

    override fun run(plan: DynamicPlaybackPlan): DynamicPlaybackResult {
        val stepResults = mutableListOf<DynamicPlaybackStepResult>()
        val eventRecords = mutableListOf<DynamicPlaybackEventRecord>()

        plan.scenario.steps.forEach { step ->
            val sourceResult = sourcePromoter.promote(
                ProductionGraphPromotionPlan(
                    batch = step.sourceBatch,
                    graphs = ProductionGraphUris.forRelease(step.sourceReleaseId),
                ),
            )
            if (!sourceResult.promoted) {
                return DynamicPlaybackResult(
                    played = false,
                    scenarioId = plan.scenario.scenarioId,
                    playbackBatchId = plan.scenario.playbackBatchId,
                    actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                    stepResults = stepResults + DynamicPlaybackStepResult(step.step, sourceResult, null),
                    errors = listOf("Dynamic playback source promotion failed at step ${step.step}") + sourceResult.errors,
                )
            }

            val reasoningResult = reasoningRefresher.run(
                ReasoningPromotionPlan(
                    runId = step.reasoningRunId,
                    generatedAt = step.event.occurredAt,
                    inputGraphs = ReasoningInputGraphUris.forRelease(step.sourceReleaseId),
                    outputGraphs = ReasoningOutputGraphUris.forRun(step.reasoningRunId),
                ),
            )
            stepResults += DynamicPlaybackStepResult(step.step, sourceResult, reasoningResult)
            if (!reasoningResult.promoted) {
                return DynamicPlaybackResult(
                    played = false,
                    scenarioId = plan.scenario.scenarioId,
                    playbackBatchId = plan.scenario.playbackBatchId,
                    actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                    stepResults = stepResults.toList(),
                    errors = listOf("Dynamic playback reasoning refresh failed at step ${step.step}") + reasoningResult.errors,
                )
            }

            eventRecords += step.event.copy(
                canonicalGraphUri = ProductionGraphUris.forRelease(step.sourceReleaseId).canonicalGraphUri,
                provenanceGraphUri = ProductionGraphUris.forRelease(step.sourceReleaseId).provenanceGraphUri,
                reasoningGraphUri = ReasoningOutputGraphUris.forRun(step.reasoningRunId).reasoningGraphUri,
            )
        }

        val snapshot = runCatching { graphStore.readNamedGraph(plan.graphs.actionAuditGraphUri) }
            .getOrElse { error ->
                return DynamicPlaybackResult(
                    played = false,
                    scenarioId = plan.scenario.scenarioId,
                    playbackBatchId = plan.scenario.playbackBatchId,
                    actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                    stepResults = stepResults.toList(),
                    errors = listOf("Dynamic playback graph snapshot failed: ${error.message}"),
                )
            }
        val candidate = ModelFactory.createDefaultModel()
            .add(snapshot.model)
            .add(mapper.map(plan.scenario.copy(steps = plan.scenario.steps.zip(eventRecords).map { (step, event) -> step.copy(event = event) })))
        val validation = validationGate.validate(candidate)
        if (!validation.conforms) {
            return DynamicPlaybackResult(
                played = false,
                scenarioId = plan.scenario.scenarioId,
                playbackBatchId = plan.scenario.playbackBatchId,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                stepResults = stepResults.toList(),
                validation = validation,
                errors = validation.errors,
            )
        }

        val write = graphWrites.replaceAll(
            graphModels = mapOf(plan.graphs.actionAuditGraphUri to candidate),
            snapshots = mapOf(plan.graphs.actionAuditGraphUri to snapshot),
            writeFailurePrefix = "Dynamic playback graph write failed",
            rollbackFailurePrefix = "Dynamic playback rollback failed",
        )
        return if (write.succeeded) {
            DynamicPlaybackResult(
                played = true,
                scenarioId = plan.scenario.scenarioId,
                playbackBatchId = plan.scenario.playbackBatchId,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                stepResults = stepResults.toList(),
                validation = validation,
                writtenGraphUris = write.writtenGraphUris,
            )
        } else {
            DynamicPlaybackResult(
                played = false,
                scenarioId = plan.scenario.scenarioId,
                playbackBatchId = plan.scenario.playbackBatchId,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                stepResults = stepResults.toList(),
                validation = validation,
                rollbackAttempted = write.rollbackAttempted,
                rollbackSucceeded = write.rollbackSucceeded,
                errors = write.errors,
            )
        }
    }
}

class DynamicPlaybackRdfMapper {
    fun map(scenario: DynamicPlaybackScenario): Model {
        val model = ModelFactory.createDefaultModel()
        val batch = playbackBatch(scenario.playbackBatchId)
        model.add(batch, RDF.type, Dcai.DynamicPlaybackBatch)
        model.add(batch, Dcai.hasIdentifier, scenario.playbackBatchId)
        model.add(batch, Dcai.hasScenarioId, scenario.scenarioId)
        model.add(batch, Dcai.hasPlaybackBatchId, scenario.playbackBatchId)
        model.add(batch, Prov.generatedAtTime, typedLiteral(scenario.generatedAt.toString(), XSDDatatype.XSDdateTime))

        scenario.steps.forEach { step ->
            val event = playbackEvent(step.event.eventId)
            val incident = ResourceFactory.createResource("urn:dcai:incident:${encode(step.event.incidentId)}")
            model.add(event, RDF.type, Dcai.DynamicPlaybackEvent)
            model.add(event, Dcai.hasIdentifier, step.event.eventId)
            model.add(event, Dcai.hasScenarioId, scenario.scenarioId)
            model.add(event, Dcai.hasPlaybackBatchId, scenario.playbackBatchId)
            model.add(event, Dcai.hasPlaybackStep, typedLiteral(step.step.toString(), XSDDatatype.XSDinteger))
            model.add(event, Dcai.hasEventKind, step.event.eventKind)
            model.add(event, Dcai.hasSourceFamily, step.event.sourceFamily)
            model.add(event, Dcai.hasBeforeState, step.event.beforeState)
            model.add(event, Dcai.hasAfterState, step.event.afterState)
            model.add(event, Dcai.hasBeforeReasoningState, step.event.beforeReasoningState)
            model.add(event, Dcai.hasAfterReasoningState, step.event.afterReasoningState)
            model.add(event, Dcai.hasBeforeTrustState, step.event.beforeTrustState)
            model.add(event, Dcai.hasAfterTrustState, step.event.afterTrustState)
            model.add(event, Dcai.hasBeforeBlastRadiusCount, typedLiteral(step.event.beforeBlastRadiusCount.toString(), XSDDatatype.XSDinteger))
            model.add(event, Dcai.hasAfterBlastRadiusCount, typedLiteral(step.event.afterBlastRadiusCount.toString(), XSDDatatype.XSDinteger))
            model.add(event, Dcai.hasActionLifecycleState, step.event.actionLifecycleState)
            model.add(event, Dcai.hasFindingSummary, step.event.summary)
            model.add(event, Dcai.hasTargetObject, incident)
            model.add(event, Prov.used, ResourceFactory.createResource(step.event.sourceRecordUri))
            model.add(event, Prov.used, ResourceFactory.createResource(step.event.canonicalGraphUri))
            model.add(event, Prov.used, ResourceFactory.createResource(step.event.provenanceGraphUri))
            model.add(event, Prov.used, ResourceFactory.createResource(step.event.reasoningGraphUri))
            model.add(event, Prov.wasGeneratedBy, batch)
            model.add(event, Prov.generatedAtTime, typedLiteral(step.event.occurredAt.toString(), XSDDatatype.XSDdateTime))
        }
        return model
    }

    private fun playbackBatch(playbackBatchId: String): Resource {
        return ResourceFactory.createResource("urn:dcai:dynamic-playback-batch:${encode(playbackBatchId)}")
    }

    private fun playbackEvent(eventId: String): Resource {
        return ResourceFactory.createResource("urn:dcai:dynamic-playback-event:${encode(eventId)}")
    }

    private fun typedLiteral(value: String, datatype: XSDDatatype) = ResourceFactory.createTypedLiteral(value, datatype)

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
    }
}

data class DynamicPlaybackPlan(
    val scenario: DynamicPlaybackScenario,
    val graphs: OntologyActionGraphUris,
)

data class DynamicPlaybackScenario(
    val scenarioId: String,
    val playbackBatchId: String,
    val generatedAt: Instant,
    val steps: List<DynamicPlaybackStep>,
) {
    init {
        ControlledIdentifier.requireRelease(scenarioId, "scenarioId")
        ControlledIdentifier.requireRelease(playbackBatchId, "playbackBatchId")
        require(steps.isNotEmpty()) { "dynamic playback scenario must contain at least one step" }
        require(steps.map { it.step } == steps.map { it.step }.sorted()) { "dynamic playback steps must be ordered" }
    }
}

data class DynamicPlaybackStep(
    val step: Int,
    val sourceReleaseId: String,
    val reasoningRunId: String,
    val sourceBatch: SourceExtractBatch,
    val event: DynamicPlaybackEventRecord,
) {
    init {
        require(step > 0) { "step must be positive" }
        ControlledIdentifier.requireRelease(sourceReleaseId, "sourceReleaseId")
        ControlledIdentifier.requireRelease(reasoningRunId, "reasoningRunId")
        require(sourceBatch.batchId == sourceReleaseId) { "sourceBatch.batchId must match sourceReleaseId" }
    }
}

data class DynamicPlaybackEventRecord(
    val eventId: String,
    val occurredAt: Instant,
    val incidentId: String,
    val eventKind: String,
    val sourceFamily: String,
    val sourceRecordUri: String,
    val beforeState: String,
    val afterState: String,
    val beforeReasoningState: String,
    val afterReasoningState: String,
    val beforeTrustState: String,
    val afterTrustState: String,
    val beforeBlastRadiusCount: Int,
    val afterBlastRadiusCount: Int,
    val actionLifecycleState: String,
    val summary: String,
    val canonicalGraphUri: String = "urn:dcai:graph:canonical:pending",
    val provenanceGraphUri: String = "urn:dcai:graph:provenance:pending",
    val reasoningGraphUri: String = "urn:dcai:graph:reasoning:pending",
) {
    init {
        ControlledIdentifier.requireRelease(eventId, "eventId")
        ControlledIdentifier.requireRelease(incidentId, "incidentId")
        require(eventKind.isNotBlank()) { "eventKind must not be blank" }
        require(sourceFamily.isNotBlank()) { "sourceFamily must not be blank" }
        require(sourceRecordUri.isNotBlank()) { "sourceRecordUri must not be blank" }
        require(beforeBlastRadiusCount >= 0) { "beforeBlastRadiusCount must be non-negative" }
        require(afterBlastRadiusCount >= 0) { "afterBlastRadiusCount must be non-negative" }
        require(summary.isNotBlank()) { "summary must not be blank" }
    }
}

data class DynamicPlaybackStepResult(
    val step: Int,
    val sourcePromotionResult: GraphPromotionResult,
    val reasoningPromotionResult: ReasoningPromotionResult?,
)

data class DynamicPlaybackValidationReport(
    val conforms: Boolean,
    val tripleCount: Int = 0,
    val errors: List<String> = emptyList(),
)

data class DynamicPlaybackResult(
    val played: Boolean,
    val scenarioId: String,
    val playbackBatchId: String,
    val actionAuditGraphUri: String,
    val stepResults: List<DynamicPlaybackStepResult> = emptyList(),
    val validation: DynamicPlaybackValidationReport? = null,
    val writtenGraphUris: List<String> = emptyList(),
    val rollbackAttempted: Boolean = false,
    val rollbackSucceeded: Boolean = false,
    val errors: List<String> = emptyList(),
)
