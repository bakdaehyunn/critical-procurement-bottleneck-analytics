package com.dcai.semanticservice.governance

import com.dcai.semanticservice.graph.ManagedGraphWriteCoordinator
import com.dcai.semanticservice.graph.NamedGraphSnapshot
import com.dcai.semanticservice.graph.NamedGraphStore
import com.dcai.semanticservice.ingestion.Dcai
import com.dcai.semanticservice.ingestion.Prov
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.Shapes
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS

interface AiGovernanceProposalSubmitter {
    fun submit(plan: AiGovernanceProposalPlan): AiGovernanceProposalResult
}

class AiGovernanceProposalService(
    private val mapper: AiGovernanceProposalRdfMapper,
    private val preconditionValidator: AiGovernanceProposalPreconditionValidator,
    private val validationGate: AiGovernanceProposalValidationGate,
    private val graphStore: NamedGraphStore,
) : AiGovernanceProposalSubmitter {
    private val graphWrites = ManagedGraphWriteCoordinator(graphStore)

    override fun submit(plan: AiGovernanceProposalPlan): AiGovernanceProposalResult {
        val snapshots = runCatching {
            AiGovernanceGraphSnapshots(
                canonical = graphStore.readNamedGraph(plan.graphs.canonicalGraphUri),
                provenance = graphStore.readNamedGraph(plan.graphs.provenanceGraphUri),
                reasoning = plan.graphs.reasoningGraphUri?.let(graphStore::readNamedGraph),
                aiAudit = graphStore.readNamedGraph(plan.graphs.aiAuditGraphUri),
            )
        }.getOrElse { error ->
            return AiGovernanceProposalResult(
                proposed = false,
                validation = AiGovernanceValidationReport(conforms = false, errors = listOf("AI governance graph snapshot failed: ${error.message}")),
                aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
                errors = listOf("AI governance graph snapshot failed: ${error.message}"),
            )
        }

        if (snapshots.aiAudit.model.listSubjectsWithProperty(Dcai.hasIdempotencyKey, plan.request.idempotencyKey).hasNext()) {
            return AiGovernanceProposalResult(
                proposed = true,
                validation = AiGovernanceValidationReport(conforms = true, tripleCount = snapshots.aiAudit.model.size().toInt()),
                aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
                idempotentReplay = true,
            )
        }

        val preconditionErrors = preconditionValidator.validate(
            request = plan.request,
            canonicalModel = snapshots.canonical.model,
            provenanceModel = snapshots.provenance.model,
            reasoningModel = snapshots.reasoning?.model,
        )
        if (preconditionErrors.isNotEmpty()) {
            return AiGovernanceProposalResult(
                proposed = false,
                validation = AiGovernanceValidationReport(conforms = false, errors = preconditionErrors),
                aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
                errors = preconditionErrors,
            )
        }

        val candidate = ModelFactory.createDefaultModel()
            .add(snapshots.aiAudit.model)
            .add(mapper.map(plan.request))
        val validation = validationGate.validate(candidate)
        if (!validation.conforms) {
            return AiGovernanceProposalResult(
                proposed = false,
                validation = validation,
                aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
                errors = validation.errors,
            )
        }

        val write = graphWrites.replaceAll(
            graphModels = mapOf(plan.graphs.aiAuditGraphUri to candidate),
            snapshots = mapOf(plan.graphs.aiAuditGraphUri to snapshots.aiAudit),
            writeFailurePrefix = "AI governance proposal graph write failed",
            rollbackFailurePrefix = "AI governance rollback failed",
        )
        return if (write.succeeded) {
            AiGovernanceProposalResult(
                proposed = true,
                validation = validation,
                aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
                writtenGraphUris = write.writtenGraphUris,
            )
        } else {
            AiGovernanceProposalResult(
                proposed = false,
                validation = validation,
                aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
                rollbackAttempted = write.rollbackAttempted,
                rollbackSucceeded = write.rollbackSucceeded,
                errors = write.errors,
            )
        }
    }

    private data class AiGovernanceGraphSnapshots(
        val canonical: NamedGraphSnapshot,
        val provenance: NamedGraphSnapshot,
        val reasoning: NamedGraphSnapshot?,
        val aiAudit: NamedGraphSnapshot,
    )
}

class AiGovernanceProposalRdfMapper {
    fun map(request: AiGovernanceProposalRequest): Model {
        val model = ModelFactory.createDefaultModel()
        val batch = batch(request.batchId)
        val proposal = proposal(request.idempotencyKey)
        val validationReport = validationReport(request.idempotencyKey)
        val sourceRecord = ResourceFactory.createResource(request.sourceRecordUri)
        val supportingEvidence = ResourceFactory.createResource(request.supportingEvidenceUri)
        val target = ResourceFactory.createResource(request.targetObjectUri)
        val incident = ResourceFactory.createResource(request.incidentUri)

        model.add(batch, RDF.type, Dcai.AIProposalBatch)
        model.add(batch, Dcai.hasIdentifier, request.batchId)
        model.add(batch, Prov.generatedAtTime, typedLiteral(request.generatedAt.toString(), XSDDatatype.XSDdateTime))

        model.add(proposal, RDF.type, Dcai.AIProposal)
        model.add(proposal, Dcai.hasIdentifier, request.proposalId)
        model.add(proposal, Dcai.hasIdempotencyKey, request.idempotencyKey)
        model.add(proposal, Dcai.hasProposalType, request.proposalType.id)
        model.add(proposal, Dcai.hasProposalStatus, "PENDING_REVIEW")
        model.add(proposal, Dcai.hasProposalSummary, request.summary)
        model.add(proposal, Dcai.hasProposalRationale, request.rationale)
        model.add(proposal, Dcai.hasConfidenceScore, typedLiteral(request.confidenceScore.toString(), XSDDatatype.XSDdecimal))
        model.add(proposal, Dcai.hasRiskLevel, request.riskLevel.id)
        model.add(proposal, Dcai.hasModelId, request.modelId)
        model.add(proposal, Dcai.hasPromptId, request.promptId)
        model.add(proposal, Dcai.hasPromptHash, request.promptHash)
        model.add(proposal, Dcai.hasActorId, request.actorId)
        model.add(proposal, Dcai.hasAIGovernanceReviewStatus, "PENDING_HUMAN_REVIEW")
        model.add(proposal, Dcai.hasAIGovernanceDisabledReason, "AI proposal is pending human review; approved action handoff remains confined to managed audit graphs.")
        model.add(proposal, Dcai.hasTargetObject, target)
        model.add(proposal, Dcai.hasTargetObject, incident)
        model.add(proposal, Prov.used, sourceRecord)
        model.add(proposal, Prov.used, supportingEvidence)
        model.add(proposal, Prov.wasGeneratedBy, batch)
        model.add(proposal, Prov.generatedAtTime, typedLiteral(request.generatedAt.toString(), XSDDatatype.XSDdateTime))
        model.add(proposal, Prov.generated, validationReport)

        model.add(validationReport, RDF.type, Dcai.AIProposalValidationReport)
        model.add(validationReport, Dcai.hasIdentifier, "${request.proposalId}:validation")
        model.add(validationReport, Dcai.hasAIValidationStatus, "CONFORMS")
        model.add(validationReport, Dcai.hasAIValidationSummary, "AI proposal passed local evidence, confidence, risk, and provenance policy.")
        model.add(validationReport, Prov.wasGeneratedBy, proposal)
        return model
    }

    private fun batch(batchId: String): Resource = ResourceFactory.createResource("urn:dcai:ai-proposal-batch:${encode(batchId)}")

    private fun proposal(idempotencyKey: String): Resource = ResourceFactory.createResource("urn:dcai:ai-proposal:${encode(idempotencyKey)}")

    private fun validationReport(idempotencyKey: String): Resource = ResourceFactory.createResource("urn:dcai:ai-proposal-validation-report:${encode(idempotencyKey)}")

    private fun typedLiteral(value: String, datatype: XSDDatatype) = ResourceFactory.createTypedLiteral(value, datatype)

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}

class AiGovernanceProposalPreconditionValidator {
    fun validate(
        request: AiGovernanceProposalRequest,
        canonicalModel: Model,
        provenanceModel: Model,
        reasoningModel: Model?,
    ): List<String> {
        val errors = mutableListOf<String>()
        if (!CONTROLLED_TOKEN.matches(request.batchId)) errors += "batchId must use the controlled local identifier vocabulary"
        if (!CONTROLLED_TOKEN.matches(request.idempotencyKey)) errors += "idempotencyKey must use the controlled local identifier vocabulary"
        if (!CONTROLLED_TOKEN.matches(request.actorId)) errors += "actorId must use the controlled local identifier vocabulary"
        if (!CONTROLLED_TOKEN.matches(request.modelId)) errors += "modelId must use the controlled local identifier vocabulary"
        if (!CONTROLLED_TOKEN.matches(request.promptId)) errors += "promptId must use the controlled local identifier vocabulary"
        if (!PROMPT_HASH.matches(request.promptHash)) errors += "promptHash must use the controlled local prompt hash vocabulary"
        if (request.confidenceScore < 0.5 || request.confidenceScore > 1.0) {
            errors += "confidenceScore must be between 0.5 and 1.0 for local AI governance proposals"
        }
        if (!canonicalModel.contains(ResourceFactory.createResource(request.incidentUri), RDF.type, Dcai.InfrastructureIncident)) {
            errors += "Incident target is missing from canonical graph: ${request.incidentUri}"
        }
        if (!provenanceModel.contains(ResourceFactory.createResource(request.sourceRecordUri), RDF.type, Dcai.SourceRecord)) {
            errors += "Source record provenance is missing: ${request.sourceRecordUri}"
        }
        val evidence = ResourceFactory.createResource(request.supportingEvidenceUri)
        val evidenceExists = canonicalModel.contains(evidence, RDF.type) || (reasoningModel?.contains(evidence, RDF.type) == true)
        if (!evidenceExists) {
            errors += "Supporting evidence is missing from canonical or reasoning graph: ${request.supportingEvidenceUri}"
        }
        val target = ResourceFactory.createResource(request.targetObjectUri)
        val targetExists = canonicalModel.contains(target, RDF.type) || (reasoningModel?.contains(target, RDF.type) == true)
        if (!targetExists) {
            errors += "AI proposal target object is missing from canonical or reasoning graph: ${request.targetObjectUri}"
        }
        return errors
    }

    private companion object {
        private val CONTROLLED_TOKEN = Regex("[A-Za-z0-9._:-]+")
        private val PROMPT_HASH = Regex("[A-Za-z0-9._:-]+")
    }
}

class AiGovernanceProposalValidationGate(
    private val repoRoot: Path,
) {
    fun validate(model: Model): AiGovernanceValidationReport {
        val errors = validateShacl(model) + validateProposalProvenance(model) + validateApprovalDecisionProvenance(model)
        return AiGovernanceValidationReport(
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
            listOf("AI governance SHACL validation failed: $details")
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
        return direct.toSet() + direct.flatMap { ancestor -> ancestorsOf(ancestor, superClasses, seen + ancestor) }
    }

    private fun validateProposalProvenance(model: Model): List<String> {
        val proposals = model.listSubjectsWithProperty(RDF.type, Dcai.AIProposal).toList()
        if (proposals.isEmpty()) {
            return listOf("AI governance provenance gate failed: no dcai:AIProposal")
        }
        val incompleteProposals = proposals.filterNot { proposal ->
            model.contains(proposal, Dcai.hasIdentifier) &&
                model.contains(proposal, Dcai.hasIdempotencyKey) &&
                model.contains(proposal, Dcai.hasProposalType) &&
                model.contains(proposal, Dcai.hasProposalSummary) &&
                model.contains(proposal, Dcai.hasConfidenceScore) &&
                model.contains(proposal, Dcai.hasRiskLevel) &&
                model.contains(proposal, Dcai.hasModelId) &&
                model.contains(proposal, Dcai.hasPromptId) &&
                model.contains(proposal, Dcai.hasPromptHash) &&
                model.contains(proposal, Dcai.hasTargetObject) &&
                model.listObjectsOfProperty(proposal, Prov.used).toList().size >= 2 &&
                model.contains(proposal, Prov.wasGeneratedBy) &&
                model.contains(proposal, Prov.generatedAtTime) &&
                model.contains(proposal, Prov.generated)
        }
        if (incompleteProposals.isNotEmpty()) {
            return listOf("AI governance provenance gate failed: ${incompleteProposals.size} AIProposal resources are incomplete")
        }

        val reports = model.listSubjectsWithProperty(RDF.type, Dcai.AIProposalValidationReport).toList()
        if (reports.isEmpty()) {
            return listOf("AI governance provenance gate failed: no dcai:AIProposalValidationReport")
        }
        val incompleteReports = reports.filterNot { report ->
            model.contains(report, Dcai.hasAIValidationStatus) &&
                model.contains(report, Dcai.hasAIValidationSummary) &&
                model.contains(report, Prov.wasGeneratedBy)
        }
        if (incompleteReports.isNotEmpty()) {
            return listOf("AI governance provenance gate failed: ${incompleteReports.size} AIProposalValidationReport resources are incomplete")
        }
        return emptyList()
    }

    private fun validateApprovalDecisionProvenance(model: Model): List<String> {
        val decisions = model.listSubjectsWithProperty(RDF.type, Dcai.AIApprovalDecision).toList()
        val incompleteDecisions = decisions.filterNot { decision ->
            model.contains(decision, Dcai.hasIdentifier) &&
                model.contains(decision, Dcai.hasIdempotencyKey) &&
                model.contains(decision, Dcai.hasApprovalState) &&
                model.contains(decision, Dcai.hasActorId) &&
                model.contains(decision, Dcai.hasReviewSummary) &&
                model.contains(decision, Dcai.hasTargetObject) &&
                model.contains(decision, Prov.used) &&
                model.contains(decision, Prov.wasDerivedFrom) &&
                model.contains(decision, Prov.generatedAtTime)
        }
        if (incompleteDecisions.isNotEmpty()) {
            return listOf("AI governance provenance gate failed: ${incompleteDecisions.size} AIApprovalDecision resources are incomplete")
        }
        return emptyList()
    }
}
