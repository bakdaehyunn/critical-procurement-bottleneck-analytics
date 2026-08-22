package com.dcai.semanticservice.dynamic

import com.dcai.semanticservice.ontology.Dcai
import com.dcai.semanticservice.ontology.Prov
import com.dcai.semanticservice.validation.SemanticValidationEngine
import com.dcai.semanticservice.validation.SemanticValidationEngines
import com.dcai.semanticservice.validation.SemanticValidationProfile
import java.nio.file.Path
import org.apache.jena.rdf.model.Model
import org.apache.jena.vocabulary.RDF

class DynamicPlaybackValidationGate(
    private val repoRoot: Path,
    private val validationEngine: SemanticValidationEngine = SemanticValidationEngines.forRepoRoot(repoRoot),
) {
    fun validate(model: Model): DynamicPlaybackValidationReport {
        val errors = validateShacl(model) + validatePlaybackProvenance(model)
        return DynamicPlaybackValidationReport(
            conforms = errors.isEmpty(),
            tripleCount = model.size().toInt(),
            errors = errors,
        )
    }

    private fun validateShacl(model: Model): List<String> {
        return validationEngine.validate(
            model,
            SemanticValidationProfile.DYNAMIC_PLAYBACK,
            "Dynamic playback SHACL validation failed",
        )
    }

    private fun validatePlaybackProvenance(model: Model): List<String> {
        val batches = model.listSubjectsWithProperty(RDF.type, Dcai.DynamicPlaybackBatch).toList()
        if (batches.isEmpty()) {
            return listOf("Dynamic playback provenance gate failed: no dcai:DynamicPlaybackBatch")
        }
        val incompleteBatches = batches.filterNot { batch ->
            model.contains(batch, Dcai.hasScenarioId) &&
                model.contains(batch, Dcai.hasPlaybackBatchId) &&
                model.contains(batch, Prov.generatedAtTime)
        }
        if (incompleteBatches.isNotEmpty()) {
            return listOf("Dynamic playback provenance gate failed: ${incompleteBatches.size} playback batches are incomplete")
        }

        val events = model.listSubjectsWithProperty(RDF.type, Dcai.DynamicPlaybackEvent).toList()
        if (events.isEmpty()) {
            return listOf("Dynamic playback provenance gate failed: no dcai:DynamicPlaybackEvent")
        }
        val incompleteEvents = events.filterNot { event ->
            model.contains(event, Dcai.hasIdentifier) &&
                model.contains(event, Dcai.hasScenarioId) &&
                model.contains(event, Dcai.hasPlaybackBatchId) &&
                model.contains(event, Dcai.hasPlaybackStep) &&
                model.contains(event, Dcai.hasEventKind) &&
                model.contains(event, Dcai.hasSourceFamily) &&
                model.contains(event, Dcai.hasBeforeState) &&
                model.contains(event, Dcai.hasAfterState) &&
                model.contains(event, Dcai.hasBeforeReasoningState) &&
                model.contains(event, Dcai.hasAfterReasoningState) &&
                model.contains(event, Dcai.hasBeforeTrustState) &&
                model.contains(event, Dcai.hasAfterTrustState) &&
                model.contains(event, Dcai.hasBeforeBlastRadiusCount) &&
                model.contains(event, Dcai.hasAfterBlastRadiusCount) &&
                model.contains(event, Dcai.hasActionLifecycleState) &&
                model.contains(event, Dcai.hasFindingSummary) &&
                model.contains(event, Prov.used) &&
                model.contains(event, Prov.wasGeneratedBy) &&
                model.contains(event, Prov.generatedAtTime)
        }
        if (incompleteEvents.isNotEmpty()) {
            return listOf("Dynamic playback provenance gate failed: ${incompleteEvents.size} playback events are incomplete")
        }

        return emptyList()
    }
}
