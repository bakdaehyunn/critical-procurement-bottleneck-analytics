package com.dcai.semanticservice.fixtures

import com.dcai.semanticservice.validation.SemanticValidationEngine
import com.dcai.semanticservice.validation.SemanticValidationEngines
import com.dcai.semanticservice.validation.SemanticValidationProfile
import java.nio.file.Path
import kotlin.io.path.exists
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.vocabulary.RDF

class FixtureValidationGate(
    private val repoRoot: Path,
    private val validationEngine: SemanticValidationEngine = SemanticValidationEngines.forRepoRoot(repoRoot),
) {
    fun validate(fixture: FixtureGraphTarget): ValidatedFixtureGraph {
        val model = ModelFactory.createDefaultModel()
        val errors = mutableListOf<String>()

        if (!fixture.path.exists()) {
            errors += "Missing fixture: ${repoRoot.relativize(fixture.path)}"
            return ValidatedFixtureGraph(
                target = fixture,
                model = model,
                validation = FixtureValidationReport(conforms = false, tripleCount = 0, errors = errors),
            )
        }

        runCatching {
            RDFDataMgr.read(model, fixture.path.toUri().toString())
        }.onFailure { error ->
            errors += "Unable to parse fixture ${repoRoot.relativize(fixture.path)}: ${error.message}"
        }

        if (errors.isEmpty()) {
            errors += validateShacl(model)
            errors += validateProvenance(model)
        }

        return ValidatedFixtureGraph(
            target = fixture,
            model = model,
            validation = FixtureValidationReport(
                conforms = errors.isEmpty(),
                tripleCount = model.size().toInt(),
                errors = errors,
            ),
        )
    }

    private fun validateShacl(model: Model): List<String> {
        return validationEngine.validate(model, SemanticValidationProfile.FIXTURE)
    }

    private fun validateProvenance(model: Model): List<String> {
        val sourceRecords = model.listSubjectsWithProperty(RDF.type, SOURCE_RECORD).toList()
        if (sourceRecords.isEmpty()) {
            return listOf("Provenance gate failed: fixture has no dcai:SourceRecord")
        }

        val hasCompleteSourceRecord = sourceRecords.any { sourceRecord ->
            model.contains(sourceRecord, HAS_SOURCE_RECORD_ID) &&
                model.contains(sourceRecord, HAS_SOURCE_SYSTEM) &&
                model.contains(sourceRecord, HAS_SOURCE_PAYLOAD_HASH) &&
                model.contains(sourceRecord, WAS_GENERATED_BY)
        }
        if (!hasCompleteSourceRecord) {
            return listOf("Provenance gate failed: no SourceRecord has id, source system, payload hash, and import activity")
        }

        val importActivities = model.listSubjectsWithProperty(RDF.type, IMPORT_ACTIVITY).toList()
        val hasTimestampedImportActivity = importActivities.any { activity ->
            model.contains(activity, GENERATED_AT_TIME)
        }
        if (!hasTimestampedImportActivity) {
            return listOf("Provenance gate failed: no ImportActivity has prov:generatedAtTime")
        }

        return emptyList()
    }

    companion object {
        private val SOURCE_RECORD = ResourceFactory.createResource("urn:dcai:ontology:SourceRecord")
        private val IMPORT_ACTIVITY = ResourceFactory.createResource("urn:dcai:ontology:ImportActivity")
        private val HAS_SOURCE_RECORD_ID = ResourceFactory.createProperty("urn:dcai:ontology:hasSourceRecordId")
        private val HAS_SOURCE_SYSTEM = ResourceFactory.createProperty("urn:dcai:ontology:hasSourceSystem")
        private val HAS_SOURCE_PAYLOAD_HASH = ResourceFactory.createProperty("urn:dcai:ontology:hasSourcePayloadHash")
        private val WAS_GENERATED_BY = ResourceFactory.createProperty("http://www.w3.org/ns/prov#wasGeneratedBy")
        private val GENERATED_AT_TIME = ResourceFactory.createProperty("http://www.w3.org/ns/prov#generatedAtTime")
    }
}

data class ValidatedFixtureGraph(
    val target: FixtureGraphTarget,
    val model: Model,
    val validation: FixtureValidationReport,
)

data class FixtureValidationReport(
    val conforms: Boolean,
    val tripleCount: Int,
    val errors: List<String> = emptyList(),
)
