package com.dcai.semanticservice.dynamic

import com.dcai.semanticservice.ingestion.Dcai
import com.dcai.semanticservice.ingestion.Prov
import java.nio.file.Path
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.Shapes
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS

class DynamicPlaybackValidationGate(
    private val repoRoot: Path,
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
        val shapesModel = ModelFactory.createDefaultModel()
        repoRoot.resolve("shapes").toFile()
            .walkTopDown()
            .filter { it.isFile && it.extension == "ttl" }
            .sortedBy { it.path }
            .forEach { RDFDataMgr.read(shapesModel, it.toURI().toString()) }

        val shapes = Shapes.parse(shapesModel.graph)
        val report = ShaclValidator.get().validate(shapes, withRdfsTypeClosure(model).graph)
        return if (report.conforms()) {
            emptyList()
        } else {
            val details = report.getEntries().joinToString(separator = "; ") { it.toString() }
            listOf("Dynamic playback SHACL validation failed: $details")
        }
    }

    private fun withRdfsTypeClosure(model: Model): Model {
        val validationModel = ModelFactory.createDefaultModel().add(model)
        val ontologyModel = ModelFactory.createDefaultModel()
        repoRoot.resolve("ontology/modules").toFile()
            .walkTopDown()
            .filter { it.isFile && it.extension == "ttl" }
            .sortedBy { it.path }
            .forEach { RDFDataMgr.read(ontologyModel, it.toURI().toString()) }

        val superClasses = ontologyModel
            .listStatements(null as Resource?, RDFS.subClassOf, null as RDFNode?)
            .toList()
            .filter { statement -> statement.subject.isURIResource && statement.`object`.isURIResource }
            .groupBy(
                keySelector = { statement -> statement.subject.asResource() },
                valueTransform = { statement -> statement.`object`.asResource() },
            )

        validationModel.listStatements(null as Resource?, RDF.type, null as RDFNode?).toList()
            .filter { statement -> statement.`object`.isURIResource }
            .forEach { statement ->
                ancestorsOf(statement.`object`.asResource(), superClasses).forEach { ancestor ->
                    validationModel.add(statement.subject, RDF.type, ancestor)
                }
            }

        return validationModel
    }

    private fun ancestorsOf(
        resource: Resource,
        superClasses: Map<Resource, List<Resource>>,
        seen: Set<Resource> = emptySet(),
    ): Set<Resource> {
        val direct = superClasses[resource].orEmpty().filterNot { it in seen }
        return direct.toSet() + direct.flatMap { ancestor ->
            ancestorsOf(ancestor, superClasses, seen + ancestor)
        }
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
