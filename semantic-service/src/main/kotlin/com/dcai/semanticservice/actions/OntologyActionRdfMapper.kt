package com.dcai.semanticservice.actions

import com.dcai.semanticservice.ingestion.Dcai
import com.dcai.semanticservice.ingestion.Prov
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS

class OntologyActionRdfMapper {
    fun map(request: OntologyActionRequest): Model {
        val model = ModelFactory.createDefaultModel()
        val actionType = actionType(request.actionType)
        val actionRequest = actionRequest(request.idempotencyKey)
        val execution = actionExecution(request.idempotencyKey)
        val validationReport = validationReport(request.idempotencyKey)
        val targetObjects = targetObjects(request)

        model.add(actionType, RDF.type, Dcai.OntologyActionType)
        model.add(actionType, Dcai.hasIdentifier, request.actionType.id)
        model.add(actionType, RDFS.label, request.actionType.id)

        model.add(actionRequest, RDF.type, Dcai.OntologyActionRequest)
        model.add(actionRequest, Dcai.hasIdentifier, request.requestId)
        model.add(actionRequest, Dcai.hasIdempotencyKey, request.idempotencyKey)
        model.add(actionRequest, Dcai.hasActionType, actionType)
        model.add(actionRequest, Dcai.hasActorId, request.actorId)
        model.add(actionRequest, Dcai.hasActionReason, request.actionReason)
        model.add(actionRequest, Prov.generatedAtTime, literal(request.requestedAt.toString(), XSDDatatype.XSDdateTime))

        model.add(execution, RDF.type, Dcai.OntologyActionExecution)
        model.add(execution, Dcai.hasIdentifier, request.idempotencyKey)
        model.add(execution, Dcai.hasIdempotencyKey, request.idempotencyKey)
        model.add(execution, Dcai.hasActionType, actionType)
        model.add(execution, Dcai.hasActorId, request.actorId)
        model.add(execution, Dcai.hasActionReason, request.actionReason)
        model.add(execution, Dcai.hasActionStatus, "AUDITED")
        model.add(execution, Prov.used, actionRequest)
        model.add(execution, Prov.generated, validationReport)
        model.add(execution, Prov.generatedAtTime, literal(request.requestedAt.toString(), XSDDatatype.XSDdateTime))

        targetObjects.forEach { target ->
            model.add(actionRequest, Dcai.hasTargetObject, target)
            model.add(execution, Dcai.hasTargetObject, target)
            model.add(execution, Prov.used, target)
        }
        model.add(execution, Prov.wasDerivedFrom, ResourceFactory.createResource(request.sourceRecordUri))

        model.add(validationReport, RDF.type, Dcai.ActionValidationReport)
        model.add(validationReport, Dcai.hasActionValidationStatus, "CONFORMS")
        model.add(validationReport, Dcai.hasFindingSummary, "Ontology action request passed local precondition and provenance validation.")
        model.add(validationReport, Prov.wasGeneratedBy, execution)

        request.assignedTeam?.let { model.add(execution, Dcai.hasAssignedTeam, it) }
        request.assigneeId?.let { model.add(execution, Dcai.hasAssigneeId, it) }
        request.reviewedStatus?.let { model.add(execution, Dcai.hasReviewedStatus, it) }
        request.reviewSummary?.let { model.add(execution, Dcai.hasReviewSummary, it) }
        request.supportingEvidenceUri?.let { model.add(execution, Dcai.hasSupportingEvidence, ResourceFactory.createResource(it)) }

        return model
    }

    private fun targetObjects(request: OntologyActionRequest): List<Resource> {
        return listOfNotNull(
            request.incidentUri,
            request.sourceRecordUri,
            request.restoreReadinessFindingUri,
            request.recoveryBlockerUri,
            request.trustFindingUri,
            request.validationEvidenceUri,
            request.supportingEvidenceUri,
        ).distinct().map(ResourceFactory::createResource)
    }

    private fun actionType(type: OntologyActionType): Resource {
        return ResourceFactory.createResource("urn:dcai:ontology-action-type:${encode(type.id)}")
    }

    private fun actionRequest(idempotencyKey: String): Resource {
        return ResourceFactory.createResource("urn:dcai:ontology-action-request:${encode(idempotencyKey)}")
    }

    private fun actionExecution(idempotencyKey: String): Resource {
        return ResourceFactory.createResource("urn:dcai:ontology-action-execution:${encode(idempotencyKey)}")
    }

    private fun validationReport(idempotencyKey: String): Resource {
        return ResourceFactory.createResource("urn:dcai:action-validation-report:${encode(idempotencyKey)}")
    }

    private fun literal(value: String, datatype: XSDDatatype) = ResourceFactory.createTypedLiteral(value, datatype)

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
    }
}

