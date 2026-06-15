package com.dcai.semanticservice.actions

import com.dcai.semanticservice.ingestion.Dcai
import com.dcai.semanticservice.ingestion.Prov
import java.nio.file.Path
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

class OntologyActionValidationGate(
    private val repoRoot: Path,
) {
    fun validate(model: Model): OntologyActionValidationReport {
        val errors = validateShacl(model) + validateActionProvenance(model)
        return OntologyActionValidationReport(
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
            listOf("Action audit SHACL validation failed: $details")
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

    private fun validateActionProvenance(model: Model): List<String> {
        val executions = model.listSubjectsWithProperty(RDF.type, Dcai.OntologyActionExecution).toList()
        if (executions.isEmpty()) {
            return listOf("Action provenance gate failed: no dcai:OntologyActionExecution")
        }

        val incompleteExecutions = executions.filterNot { execution ->
            model.contains(execution, Dcai.hasActionType) &&
                model.contains(execution, Dcai.hasActorId) &&
                model.contains(execution, Dcai.hasIdempotencyKey) &&
                model.contains(execution, Dcai.hasActionReason) &&
                model.contains(execution, Dcai.hasTargetObject) &&
                model.contains(execution, Prov.used) &&
                model.contains(execution, Prov.generated) &&
                model.contains(execution, Prov.generatedAtTime)
        }
        if (incompleteExecutions.isNotEmpty()) {
            return listOf("Action provenance gate failed: ${incompleteExecutions.size} OntologyActionExecution resources are incomplete")
        }

        val reports = model.listSubjectsWithProperty(RDF.type, Dcai.ActionValidationReport).toList()
        if (reports.isEmpty()) {
            return listOf("Action provenance gate failed: no dcai:ActionValidationReport")
        }

        val reportsWithoutExecution = reports.filterNot { report ->
            model.contains(report, Prov.wasGeneratedBy)
        }
        if (reportsWithoutExecution.isNotEmpty()) {
            return listOf("Action provenance gate failed: ${reportsWithoutExecution.size} validation reports have no generating execution")
        }

        val notifications = model.listSubjectsWithProperty(RDF.type, Dcai.OntologyActionNotification).toList()
        if (notifications.isEmpty()) {
            return listOf("Action provenance gate failed: no dcai:OntologyActionNotification")
        }

        val incompleteNotifications = notifications.filterNot { notification ->
            model.contains(notification, Dcai.hasActionType) &&
                model.contains(notification, Dcai.hasNotificationStatus) &&
                model.contains(notification, Dcai.hasTargetObject) &&
                model.contains(notification, Prov.wasGeneratedBy) &&
                model.contains(notification, Prov.generatedAtTime)
        }
        if (incompleteNotifications.isNotEmpty()) {
            return listOf("Action provenance gate failed: ${incompleteNotifications.size} OntologyActionNotification resources are incomplete")
        }

        val transitions = model.listSubjectsWithProperty(RDF.type, Dcai.OntologyActionStateTransition).toList()
        if (transitions.isEmpty()) {
            return listOf("Action provenance gate failed: no dcai:OntologyActionStateTransition")
        }

        val incompleteTransitions = transitions.filterNot { transition ->
            model.contains(transition, Dcai.hasIdentifier) &&
                model.contains(transition, Dcai.hasIdempotencyKey) &&
                model.contains(transition, Dcai.hasToActionState) &&
                model.contains(transition, Dcai.hasTransitionReason) &&
                model.contains(transition, Dcai.hasActorId) &&
                model.contains(transition, Dcai.hasTargetObject) &&
                model.contains(transition, Prov.used) &&
                model.contains(transition, Prov.generatedAtTime)
        }
        if (incompleteTransitions.isNotEmpty()) {
            return listOf("Action provenance gate failed: ${incompleteTransitions.size} OntologyActionStateTransition resources are incomplete")
        }

        val dispatches = model.listSubjectsWithProperty(RDF.type, Dcai.OntologyActionDispatch).toList()
        val incompleteDispatches = dispatches.filterNot { dispatch ->
            model.contains(dispatch, Dcai.hasIdentifier) &&
                model.contains(dispatch, Dcai.hasIdempotencyKey) &&
                model.contains(dispatch, Dcai.hasDispatchChannel) &&
                model.contains(dispatch, Dcai.hasDispatchStatus) &&
                model.contains(dispatch, Dcai.hasDispatchLifecycleState) &&
                model.contains(dispatch, Dcai.hasDispatchSummary) &&
                model.contains(dispatch, Dcai.hasActorId) &&
                model.contains(dispatch, Dcai.hasTargetObject) &&
                model.contains(dispatch, Prov.used) &&
                model.contains(dispatch, Prov.wasGeneratedBy) &&
                model.contains(dispatch, Prov.generatedAtTime)
        }
        if (incompleteDispatches.isNotEmpty()) {
            return listOf("Action provenance gate failed: ${incompleteDispatches.size} OntologyActionDispatch resources are incomplete")
        }

        return emptyList()
    }
}

class OntologyActionPreconditionValidator {
    fun validate(
        request: OntologyActionRequest,
        canonicalModel: Model,
        provenanceModel: Model,
        reasoningModel: Model?,
    ): List<String> {
        val errors = mutableListOf<String>()
        errors += validateCommon(request, canonicalModel, provenanceModel)
        errors += when (request.actionType) {
            OntologyActionType.ACKNOWLEDGE_RESTORE_BLOCKER -> validateAcknowledge(request, reasoningModel)
            OntologyActionType.ASSIGN_EVIDENCE_REVIEW -> validateEvidenceAssignment(request, reasoningModel)
            OntologyActionType.RECORD_VALIDATION_REVIEW -> validateValidationReview(request, canonicalModel)
        }
        return errors
    }

    private fun validateCommon(
        request: OntologyActionRequest,
        canonicalModel: Model,
        provenanceModel: Model,
    ): List<String> {
        val errors = mutableListOf<String>()
        if (!CONTROLLED_TOKEN.matches(request.idempotencyKey)) {
            errors += "idempotencyKey must contain only letters, numbers, dot, underscore, colon, or hyphen"
        }
        if (!CONTROLLED_TOKEN.matches(request.actorId)) {
            errors += "actorId must use the controlled local identifier vocabulary"
        }
        if (!canonicalModel.containsTypedResource(request.incidentUri, Dcai.InfrastructureIncident)) {
            errors += "Incident target is missing from canonical graph: ${request.incidentUri}"
        }
        if (!provenanceModel.containsTypedResource(request.sourceRecordUri, Dcai.SourceRecord)) {
            errors += "Source record provenance is missing: ${request.sourceRecordUri}"
        }
        return errors
    }

    private fun validateAcknowledge(
        request: OntologyActionRequest,
        reasoningModel: Model?,
    ): List<String> {
        val errors = mutableListOf<String>()
        if (reasoningModel == null) {
            return listOf("Reasoning graph is required for AcknowledgeRestoreBlocker")
        }
        val restoreReadinessFindingUri = request.restoreReadinessFindingUri
        if (restoreReadinessFindingUri.isNullOrBlank()) {
            errors += "restoreReadinessFindingUri is required for AcknowledgeRestoreBlocker"
        } else if (!reasoningModel.containsTypedResource(restoreReadinessFindingUri, Dcai.RestoreReadinessFinding)) {
            errors += "Restore-readiness finding is missing from reasoning graph: $restoreReadinessFindingUri"
        }
        val recoveryBlockerUri = request.recoveryBlockerUri
        if (!recoveryBlockerUri.isNullOrBlank() && !reasoningModel.containsTypedResource(recoveryBlockerUri, Dcai.RecoveryBlocker)) {
            errors += "Recovery blocker is missing from reasoning graph: $recoveryBlockerUri"
        }
        return errors
    }

    private fun validateEvidenceAssignment(
        request: OntologyActionRequest,
        reasoningModel: Model?,
    ): List<String> {
        val errors = mutableListOf<String>()
        if (reasoningModel == null) {
            return listOf("Reasoning graph is required for AssignEvidenceReview")
        }
        val trustFindingUri = request.trustFindingUri
        if (trustFindingUri.isNullOrBlank()) {
            errors += "trustFindingUri is required for AssignEvidenceReview"
        } else if (!reasoningModel.containsTypedResource(trustFindingUri, Dcai.TrustFinding)) {
            errors += "Trust finding is missing from reasoning graph: $trustFindingUri"
        }
        val assignedTeam = request.assignedTeam
        if (assignedTeam.isNullOrBlank()) {
            errors += "assignedTeam is required for AssignEvidenceReview"
        } else if (!CONTROLLED_TOKEN.matches(assignedTeam)) {
            errors += "assignedTeam must use the controlled local team vocabulary"
        }
        return errors
    }

    private fun validateValidationReview(
        request: OntologyActionRequest,
        canonicalModel: Model,
    ): List<String> {
        val errors = mutableListOf<String>()
        val validationEvidenceUri = request.validationEvidenceUri
        if (validationEvidenceUri.isNullOrBlank()) {
            errors += "validationEvidenceUri is required for RecordValidationReview"
        } else if (!canonicalModel.containsTypedResource(validationEvidenceUri, Dcai.ValidationEvidence)) {
            errors += "Validation evidence is missing from canonical graph: $validationEvidenceUri"
        }
        val reviewedStatus = request.reviewedStatus
        if (reviewedStatus.isNullOrBlank()) {
            errors += "reviewedStatus is required for RecordValidationReview"
        } else if (reviewedStatus !in CONTROLLED_REVIEW_STATUS) {
            errors += "reviewedStatus must use the controlled validation review vocabulary"
        }
        if (request.reviewSummary.isNullOrBlank()) {
            errors += "reviewSummary is required for RecordValidationReview"
        }
        return errors
    }

    private fun Model.containsTypedResource(uri: String, type: Resource): Boolean {
        return contains(ResourceFactory.createResource(uri), RDF.type, type)
    }

    private companion object {
        private val CONTROLLED_TOKEN = Regex("[A-Za-z0-9._:-]+")
        private val CONTROLLED_REVIEW_STATUS = setOf(
            "PASSED",
            "FAILED",
            "BLOCKED",
            "CONFLICTING_VALIDATION",
            "NEEDS_REVIEW",
        )
    }
}
