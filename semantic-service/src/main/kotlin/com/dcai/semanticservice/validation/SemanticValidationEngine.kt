package com.dcai.semanticservice.validation

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.Shapes
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS

enum class SemanticValidationProfile(
    internal val shapeFiles: Set<String>,
) {
    FIXTURE(emptySet()),
    PRODUCTION(
        setOf(
            "canonical-integrity.ttl",
            "impact-evidence.ttl",
            "provenance-required.ttl",
            "reasoning-output-validation.ttl",
            "source-required-fields.ttl",
            "topology-integrity.ttl",
            "workflow-transitions.ttl",
        ),
    ),
    REASONING(setOf("reasoning-output-validation.ttl", "provenance-required.ttl")),
    ONTOLOGY_ACTION(setOf("action-audit.ttl")),
    DYNAMIC_PLAYBACK(setOf("dynamic-playback.ttl")),
    AI_GOVERNANCE(setOf("ai-proposal-audit.ttl", "ai-proposed-write.ttl")),
}

class SemanticValidationEngine internal constructor(
    private val repoRoot: Path,
) {
    private val shapesByProfile = ConcurrentHashMap<SemanticValidationProfile, Shapes>()
    private val superClasses: Map<Resource, List<Resource>> by lazy(::loadSuperClasses)

    fun validate(
        model: Model,
        profile: SemanticValidationProfile,
        failureLabel: String = "SHACL validation failed",
    ): List<String> {
        val report = ShaclValidator.get().validate(shapes(profile), withRdfsTypeClosure(model).graph)
        if (report.conforms()) return emptyList()
        val details = report.getEntries().joinToString(separator = "; ") { it.toString() }
        return listOf("$failureLabel: $details")
    }

    private fun shapes(profile: SemanticValidationProfile): Shapes = shapesByProfile.computeIfAbsent(profile) {
        val shapesModel = ModelFactory.createDefaultModel()
        repoRoot.resolve("shapes").toFile()
            .walkTopDown()
            .filter { file -> file.isFile && file.extension == "ttl" }
            .filter { file -> profile.shapeFiles.isEmpty() || file.name in profile.shapeFiles }
            .sortedBy { file -> file.path }
            .forEach { file -> RDFDataMgr.read(shapesModel, file.toURI().toString()) }
        Shapes.parse(shapesModel.graph)
    }

    private fun withRdfsTypeClosure(model: Model): Model {
        val validationModel = ModelFactory.createDefaultModel().add(model)
        validationModel.listStatements(null as Resource?, RDF.type, null as RDFNode?).toList()
            .filter { statement -> statement.`object`.isURIResource }
            .forEach { statement ->
                ancestorsOf(statement.`object`.asResource()).forEach { ancestor ->
                    validationModel.add(statement.subject, RDF.type, ancestor)
                }
            }
        return validationModel
    }

    private fun loadSuperClasses(): Map<Resource, List<Resource>> {
        val ontologyModel = ModelFactory.createDefaultModel()
        repoRoot.resolve("ontology/modules").toFile()
            .walkTopDown()
            .filter { file -> file.isFile && file.extension == "ttl" }
            .sortedBy { file -> file.path }
            .forEach { file -> RDFDataMgr.read(ontologyModel, file.toURI().toString()) }
        return ontologyModel
            .listStatements(null as Resource?, RDFS.subClassOf, null as RDFNode?)
            .toList()
            .filter { statement -> statement.subject.isURIResource && statement.`object`.isURIResource }
            .groupBy(
                keySelector = { statement -> statement.subject.asResource() },
                valueTransform = { statement -> statement.`object`.asResource() },
            )
    }

    private fun ancestorsOf(resource: Resource, seen: Set<Resource> = emptySet()): Set<Resource> {
        val direct = superClasses[resource].orEmpty().filterNot { it in seen }
        return direct.toSet() + direct.flatMap { ancestor -> ancestorsOf(ancestor, seen + ancestor) }
    }
}

object SemanticValidationEngines {
    private val engines = ConcurrentHashMap<Path, SemanticValidationEngine>()

    fun forRepoRoot(repoRoot: Path): SemanticValidationEngine = engines.computeIfAbsent(
        repoRoot.toAbsolutePath().normalize(),
        ::SemanticValidationEngine,
    )
}
