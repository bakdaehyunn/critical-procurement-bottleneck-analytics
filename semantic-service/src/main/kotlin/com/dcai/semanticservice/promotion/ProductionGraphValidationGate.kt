package com.dcai.semanticservice.promotion

import com.dcai.semanticservice.validation.SemanticValidationEngine
import com.dcai.semanticservice.validation.SemanticValidationEngines
import com.dcai.semanticservice.validation.SemanticValidationProfile
import java.nio.file.Path
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.RDF

class ProductionGraphValidationGate(
    private val repoRoot: Path,
    private val validationEngine: SemanticValidationEngine = SemanticValidationEngines.forRepoRoot(repoRoot),
) {
    fun validate(model: Model): ProductionGraphValidationReport {
        val errors = validateShacl(model) + validateProvenance(model)
        return ProductionGraphValidationReport(
            conforms = errors.isEmpty(),
            tripleCount = model.size().toInt(),
            errors = errors,
        )
    }

    private fun validateShacl(model: Model): List<String> {
        return validationEngine.validate(model, SemanticValidationProfile.PRODUCTION)
    }

    private fun validateProvenance(model: Model): List<String> {
        val sourceRecords = model.listSubjectsWithProperty(RDF.type, SOURCE_RECORD).toList()
        if (sourceRecords.isEmpty()) {
            return listOf("Provenance gate failed: candidate graph has no dcai:SourceRecord")
        }

        val incompleteSourceRecords = sourceRecords.filterNot { sourceRecord ->
            model.contains(sourceRecord, HAS_SOURCE_RECORD_ID) &&
                model.contains(sourceRecord, HAS_SOURCE_SYSTEM) &&
                model.contains(sourceRecord, HAS_SOURCE_PAYLOAD_HASH) &&
                model.contains(sourceRecord, WAS_GENERATED_BY)
        }
        if (incompleteSourceRecords.isNotEmpty()) {
            return listOf("Provenance gate failed: ${incompleteSourceRecords.size} SourceRecord resources are incomplete")
        }

        val promotionActivities = model.listSubjectsWithProperty(RDF.type, PROMOTION_ACTIVITY).toList()
        if (promotionActivities.isEmpty()) {
            return listOf("Provenance gate failed: candidate graph has no dcai:PromotionActivity")
        }

        val hasTimestampedPromotion = promotionActivities.any { activity ->
            model.contains(activity, GENERATED_AT_TIME)
        }
        if (!hasTimestampedPromotion) {
            return listOf("Provenance gate failed: no PromotionActivity has prov:generatedAtTime")
        }

        return emptyList()
    }

    private companion object {
        private val SOURCE_RECORD = ResourceFactory.createResource("urn:dcai:ontology:SourceRecord")
        private val PROMOTION_ACTIVITY = ResourceFactory.createResource("urn:dcai:ontology:PromotionActivity")
        private val HAS_SOURCE_RECORD_ID = ResourceFactory.createProperty("urn:dcai:ontology:hasSourceRecordId")
        private val HAS_SOURCE_SYSTEM = ResourceFactory.createProperty("urn:dcai:ontology:hasSourceSystem")
        private val HAS_SOURCE_PAYLOAD_HASH = ResourceFactory.createProperty("urn:dcai:ontology:hasSourcePayloadHash")
        private val WAS_GENERATED_BY = ResourceFactory.createProperty("http://www.w3.org/ns/prov#wasGeneratedBy")
        private val GENERATED_AT_TIME = ResourceFactory.createProperty("http://www.w3.org/ns/prov#generatedAtTime")
    }
}

data class ProductionGraphValidationReport(
    val conforms: Boolean,
    val tripleCount: Int,
    val errors: List<String> = emptyList(),
)
