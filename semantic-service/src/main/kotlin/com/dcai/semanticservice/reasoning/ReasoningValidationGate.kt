package com.dcai.semanticservice.reasoning

import com.dcai.semanticservice.validation.SemanticValidationEngine
import com.dcai.semanticservice.validation.SemanticValidationEngines
import com.dcai.semanticservice.validation.SemanticValidationProfile
import java.nio.file.Path
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.RDF

class ReasoningValidationGate(
    private val repoRoot: Path,
    private val validationEngine: SemanticValidationEngine = SemanticValidationEngines.forRepoRoot(repoRoot),
) {
    fun validate(model: Model): ReasoningValidationReport {
        val errors = validateShacl(model) + validateReasoningProvenance(model)
        return ReasoningValidationReport(
            conforms = errors.isEmpty(),
            tripleCount = model.size().toInt(),
            errors = errors,
        )
    }

    private fun validateShacl(model: Model): List<String> {
        return validationEngine.validate(model, SemanticValidationProfile.REASONING)
    }

    private fun validateReasoningProvenance(model: Model): List<String> {
        val activities = model.listSubjectsWithProperty(RDF.type, REASONING_ACTIVITY).toList()
        if (activities.isEmpty()) {
            return listOf("Reasoning provenance gate failed: no dcai:ReasoningActivity")
        }

        val incompleteActivities = activities.filterNot { activity ->
            model.contains(activity, USED) &&
                model.contains(activity, GENERATED) &&
                model.contains(activity, GENERATED_AT_TIME)
        }
        if (incompleteActivities.isNotEmpty()) {
            return listOf("Reasoning provenance gate failed: ${incompleteActivities.size} ReasoningActivity resources are incomplete")
        }

        val findings = model.listSubjectsWithProperty(RDF.type, DEPENDENCY_IMPACT_FINDING).toList() +
            model.listSubjectsWithProperty(RDF.type, BLAST_RADIUS_FINDING).toList() +
            model.listSubjectsWithProperty(RDF.type, RECOVERY_BLOCKER).toList() +
            model.listSubjectsWithProperty(RDF.type, RESTORE_READINESS_FINDING).toList() +
            model.listSubjectsWithProperty(RDF.type, TRUST_FINDING).toList()
        if (findings.isEmpty()) {
            return listOf("Reasoning provenance gate failed: reasoning output has no approved finding")
        }

        val findingsWithoutActivity = findings.filterNot { finding ->
            model.contains(finding, WAS_GENERATED_BY)
        }
        if (findingsWithoutActivity.isNotEmpty()) {
            return listOf("Reasoning provenance gate failed: ${findingsWithoutActivity.size} findings have no generating activity")
        }

        return emptyList()
    }

    private companion object {
        private val REASONING_ACTIVITY = ResourceFactory.createResource("urn:dcai:ontology:ReasoningActivity")
        private val DEPENDENCY_IMPACT_FINDING = ResourceFactory.createResource("urn:dcai:ontology:DependencyImpactFinding")
        private val BLAST_RADIUS_FINDING = ResourceFactory.createResource("urn:dcai:ontology:BlastRadiusFinding")
        private val RECOVERY_BLOCKER = ResourceFactory.createResource("urn:dcai:ontology:RecoveryBlocker")
        private val RESTORE_READINESS_FINDING = ResourceFactory.createResource("urn:dcai:ontology:RestoreReadinessFinding")
        private val TRUST_FINDING = ResourceFactory.createResource("urn:dcai:ontology:TrustFinding")
        private val USED = ResourceFactory.createProperty("http://www.w3.org/ns/prov#used")
        private val GENERATED = ResourceFactory.createProperty("http://www.w3.org/ns/prov#generated")
        private val GENERATED_AT_TIME = ResourceFactory.createProperty("http://www.w3.org/ns/prov#generatedAtTime")
        private val WAS_GENERATED_BY = ResourceFactory.createProperty("http://www.w3.org/ns/prov#wasGeneratedBy")
    }
}

data class ReasoningValidationReport(
    val conforms: Boolean,
    val tripleCount: Int,
    val errors: List<String> = emptyList(),
)
