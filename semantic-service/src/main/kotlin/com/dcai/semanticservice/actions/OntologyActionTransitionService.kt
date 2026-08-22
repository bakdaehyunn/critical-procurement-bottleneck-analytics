package com.dcai.semanticservice.actions

import com.dcai.semanticservice.graph.ManagedGraphWriteCoordinator
import com.dcai.semanticservice.graph.ControlledIdentifier
import com.dcai.semanticservice.graph.NamedGraphStore
import com.dcai.semanticservice.ontology.Dcai
import com.dcai.semanticservice.ontology.Prov
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.RDF

interface OntologyActionTransitionSubmitter {
    fun submit(plan: OntologyActionTransitionPlan): OntologyActionTransitionResult
}

class OntologyActionTransitionService(
    private val validationGate: OntologyActionValidationGate,
    private val graphStore: NamedGraphStore,
) : OntologyActionTransitionSubmitter {
    private val graphWrites = ManagedGraphWriteCoordinator(graphStore)

    override fun submit(plan: OntologyActionTransitionPlan): OntologyActionTransitionResult {
        val snapshot = runCatching {
            graphStore.readNamedGraph(plan.graphs.actionAuditGraphUri)
        }.getOrElse { error ->
            return OntologyActionTransitionResult(
                transitioned = false,
                validation = OntologyActionValidationReport(conforms = false, errors = listOf("Action graph snapshot failed: ${error.message}")),
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                errors = listOf("Action graph snapshot failed: ${error.message}"),
            )
        }

        if (snapshot.model.listSubjectsWithProperty(Dcai.hasIdempotencyKey, plan.request.idempotencyKey).hasNext()) {
            return OntologyActionTransitionResult(
                transitioned = true,
                validation = OntologyActionValidationReport(conforms = true, tripleCount = snapshot.model.size().toInt()),
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                currentState = snapshot.model.currentState(plan.request.targetExecution()),
                idempotentReplay = true,
            )
        }

        val targetExecution = plan.request.targetExecution()
        val preconditionErrors = validatePreconditions(plan.request, snapshot.model, targetExecution)
        if (preconditionErrors.isNotEmpty()) {
            return OntologyActionTransitionResult(
                transitioned = false,
                validation = OntologyActionValidationReport(conforms = false, errors = preconditionErrors),
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                currentState = snapshot.model.currentState(targetExecution),
                errors = preconditionErrors,
            )
        }

        val currentState = requireNotNull(snapshot.model.currentState(targetExecution))
        val candidate = ModelFactory.createDefaultModel().add(snapshot.model)
        candidate.removeAll(targetExecution, Dcai.hasActionStatus, null)
        candidate.add(targetExecution, Dcai.hasActionStatus, plan.request.toState.id)
        candidate.notificationsFor(targetExecution).forEach { notification ->
            candidate.removeAll(notification, Dcai.hasNotificationStatus, null)
            candidate.add(notification, Dcai.hasNotificationStatus, plan.request.toState.id)
        }
        val transition = transition(plan.request.idempotencyKey)
        candidate.add(mapTransition(plan.request, currentState, targetExecution, transition))
        if (plan.request.toState == OntologyActionLifecycleState.APPROVED) {
            candidate.add(mapDispatches(plan.request, targetExecution, transition))
        }

        val validation = validationGate.validate(candidate)
        if (!validation.conforms) {
            return OntologyActionTransitionResult(
                transitioned = false,
                validation = validation,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                currentState = currentState,
                errors = validation.errors,
            )
        }

        val write = graphWrites.replaceAll(
            graphModels = mapOf(plan.graphs.actionAuditGraphUri to candidate),
            snapshots = mapOf(plan.graphs.actionAuditGraphUri to snapshot),
            writeFailurePrefix = "Action transition graph write failed",
            rollbackFailurePrefix = "Action transition rollback failed",
        )
        return if (write.succeeded) {
            OntologyActionTransitionResult(
                transitioned = true,
                validation = validation,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                currentState = plan.request.toState,
                writtenGraphUris = write.writtenGraphUris,
            )
        } else {
            OntologyActionTransitionResult(
                transitioned = false,
                validation = validation,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                currentState = currentState,
                rollbackAttempted = write.rollbackAttempted,
                rollbackSucceeded = write.rollbackSucceeded,
                errors = write.errors,
            )
        }
    }

    private fun validatePreconditions(
        request: OntologyActionTransitionRequest,
        model: Model,
        targetExecution: Resource,
    ): List<String> {
        val errors = mutableListOf<String>()
        if (!ControlledIdentifier.isLocal(request.idempotencyKey)) {
            errors += "idempotencyKey must contain only letters, numbers, dot, underscore, colon, or hyphen"
        }
        if (!ControlledIdentifier.isLocal(request.actorId)) {
            errors += "actorId must use the controlled local identifier vocabulary"
        }
        if (!model.contains(targetExecution, RDF.type, Dcai.OntologyActionExecution)) {
            errors += "Target ontology action execution is missing from action-audit graph: ${request.targetExecutionUri}"
        }
        val currentState = model.currentState(targetExecution)
        if (currentState == null) {
            errors += "Target ontology action execution has no lifecycle state transition history"
        } else if (!ALLOWED_TRANSITIONS.getValue(currentState).contains(request.toState)) {
            errors += "Invalid ontology action lifecycle transition: ${currentState.id} -> ${request.toState.id}"
        }
        if (request.toState == OntologyActionLifecycleState.CLOSED && currentState !in setOf(OntologyActionLifecycleState.APPROVED, OntologyActionLifecycleState.REJECTED)) {
            errors += "CLOSED requires APPROVED or REJECTED current state"
        }
        return errors
    }

    private fun mapTransition(
        request: OntologyActionTransitionRequest,
        fromState: OntologyActionLifecycleState,
        targetExecution: Resource,
        transition: Resource,
    ): Model {
        val model = ModelFactory.createDefaultModel()
        model.add(transition, RDF.type, Dcai.OntologyActionStateTransition)
        model.add(transition, Dcai.hasIdentifier, request.transitionId)
        model.add(transition, Dcai.hasIdempotencyKey, request.idempotencyKey)
        model.add(transition, Dcai.hasFromActionState, fromState.id)
        model.add(transition, Dcai.hasToActionState, request.toState.id)
        model.add(transition, Dcai.hasTransitionReason, request.transitionReason)
        model.add(transition, Dcai.hasActorId, request.actorId)
        model.add(transition, Dcai.hasTargetObject, targetExecution)
        model.add(transition, Prov.used, targetExecution)
        model.add(transition, Prov.generatedAtTime, ResourceFactory.createTypedLiteral(request.requestedAt.toString(), XSDDatatype.XSDdateTime))
        return model
    }

    private fun mapDispatches(
        request: OntologyActionTransitionRequest,
        targetExecution: Resource,
        transition: Resource,
    ): Model {
        val model = ModelFactory.createDefaultModel()
        SIMULATED_DISPATCH_CHANNELS.forEach { channel ->
            val dispatch = ResourceFactory.createResource("urn:dcai:ontology-action-dispatch:${encode("${request.idempotencyKey}:$channel")}")
            model.add(dispatch, RDF.type, Dcai.OntologyActionDispatch)
            model.add(dispatch, Dcai.hasIdentifier, "${request.transitionId}:dispatch:$channel")
            model.add(dispatch, Dcai.hasIdempotencyKey, "${request.idempotencyKey}:dispatch:$channel")
            model.add(dispatch, Dcai.hasDispatchChannel, channel)
            model.add(dispatch, Dcai.hasDispatchStatus, "SIMULATED_QUEUED")
            model.add(dispatch, Dcai.hasDispatchLifecycleState, request.toState.id)
            model.add(dispatch, Dcai.hasDispatchSummary, dispatchSummary(channel, request))
            model.add(dispatch, Dcai.hasActorId, request.actorId)
            model.add(dispatch, Dcai.hasTargetObject, targetExecution)
            model.add(dispatch, Prov.used, targetExecution)
            model.add(dispatch, Prov.wasGeneratedBy, transition)
            model.add(dispatch, Prov.generatedAtTime, ResourceFactory.createTypedLiteral(request.requestedAt.toString(), XSDDatatype.XSDdateTime))
        }
        return model
    }

    private fun Model.currentState(targetExecution: Resource): OntologyActionLifecycleState? {
        return listSubjectsWithProperty(RDF.type, Dcai.OntologyActionStateTransition)
            .toList()
            .filter { transition -> contains(transition, Dcai.hasTargetObject, targetExecution) }
            .mapNotNull { transition ->
                val generatedAt = listObjectsOfProperty(transition, Prov.generatedAtTime).toList().firstOrNull()?.lexicalValue()
                val state = listObjectsOfProperty(transition, Dcai.hasToActionState).toList().firstOrNull()?.lexicalValue()
                if (generatedAt == null || state == null) {
                    null
                } else {
                    generatedAt to state
                }
            }
            .maxByOrNull { it.first }
            ?.second
            ?.let(OntologyActionLifecycleState::fromId)
    }

    private fun Model.notificationsFor(targetExecution: Resource): List<Resource> {
        return listSubjectsWithProperty(RDF.type, Dcai.OntologyActionNotification)
            .toList()
            .filter { notification -> contains(notification, Prov.wasGeneratedBy, targetExecution) }
    }

    private fun RDFNode.lexicalValue(): String {
        return if (isLiteral) {
            asLiteral().lexicalForm
        } else {
            toString()
        }
    }

    private fun OntologyActionTransitionRequest.targetExecution(): Resource {
        return ResourceFactory.createResource(targetExecutionUri)
    }

    private fun transition(idempotencyKey: String): Resource {
        return ResourceFactory.createResource("urn:dcai:ontology-action-transition:${encode(idempotencyKey)}")
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
    }

    private companion object {
        private val SIMULATED_DISPATCH_CHANNELS = listOf(
            "NOC_QUEUE",
            "WORK_ORDER_QUEUE",
            "VALIDATION_REVIEW_QUEUE",
        )
        private val ALLOWED_TRANSITIONS = mapOf(
            OntologyActionLifecycleState.REQUESTED to setOf(OntologyActionLifecycleState.VALIDATED),
            OntologyActionLifecycleState.VALIDATED to setOf(OntologyActionLifecycleState.QUEUED),
            OntologyActionLifecycleState.QUEUED to setOf(OntologyActionLifecycleState.IN_REVIEW, OntologyActionLifecycleState.REJECTED),
            OntologyActionLifecycleState.IN_REVIEW to setOf(OntologyActionLifecycleState.APPROVED, OntologyActionLifecycleState.REJECTED),
            OntologyActionLifecycleState.APPROVED to setOf(OntologyActionLifecycleState.CLOSED),
            OntologyActionLifecycleState.REJECTED to setOf(OntologyActionLifecycleState.CLOSED),
            OntologyActionLifecycleState.CLOSED to emptySet(),
        )

        private fun dispatchSummary(channel: String, request: OntologyActionTransitionRequest): String {
            return when (channel) {
                "NOC_QUEUE" -> "Simulated NOC queue dispatch created for approved local ontology action ${request.transitionId}; no external notification was sent."
                "WORK_ORDER_QUEUE" -> "Simulated work-order queue dispatch created for approved local ontology action ${request.transitionId}; no source-system writeback was attempted."
                "VALIDATION_REVIEW_QUEUE" -> "Simulated validation review queue dispatch created for approved local ontology action ${request.transitionId}; no external validation system was mutated."
                else -> "Simulated internal dispatch created for approved local ontology action ${request.transitionId}."
            }
        }
    }
}
