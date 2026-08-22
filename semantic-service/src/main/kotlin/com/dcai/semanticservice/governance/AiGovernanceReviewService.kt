package com.dcai.semanticservice.governance

import com.dcai.semanticservice.actions.OntologyActionAuditPlan
import com.dcai.semanticservice.actions.OntologyActionAuditResult
import com.dcai.semanticservice.actions.OntologyActionGraphUris
import com.dcai.semanticservice.actions.OntologyActionRequest
import com.dcai.semanticservice.actions.OntologyActionSubmitter
import com.dcai.semanticservice.actions.OntologyActionType
import com.dcai.semanticservice.graph.ManagedGraphWriteCoordinator
import com.dcai.semanticservice.graph.ManagedGraphKind
import com.dcai.semanticservice.graph.ManagedGraphUri
import com.dcai.semanticservice.graph.ControlledIdentifier
import com.dcai.semanticservice.graph.NamedGraphSnapshot
import com.dcai.semanticservice.graph.NamedGraphStore
import com.dcai.semanticservice.ontology.Dcai
import com.dcai.semanticservice.ontology.Prov
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.RDF

enum class AiGovernanceReviewDecision(val id: String) {
    APPROVE("APPROVE"),
    REJECT("REJECT"),
    ;

    val reviewStatus: String
        get() = when (this) {
            APPROVE -> "APPROVED"
            REJECT -> "REJECTED"
        }

    companion object {
        fun fromId(id: String): AiGovernanceReviewDecision? = entries.firstOrNull { it.id == id }
    }
}

data class AiGovernanceReviewRequest(
    val reviewId: String,
    val idempotencyKey: String,
    val actorId: String,
    val reviewedAt: Instant,
    val proposalUri: String,
    val decision: AiGovernanceReviewDecision,
    val reviewReason: String,
    val actionType: OntologyActionType? = null,
) {
    init {
        require(reviewId.isNotBlank()) { "reviewId must not be blank" }
        require(idempotencyKey.isNotBlank()) { "idempotencyKey must not be blank" }
        require(actorId.isNotBlank()) { "actorId must not be blank" }
        require(proposalUri.isNotBlank()) { "proposalUri must not be blank" }
        require(reviewReason.isNotBlank()) { "reviewReason must not be blank" }
    }
}

data class AiGovernanceReviewGraphUris(
    val canonicalGraphUri: String,
    val provenanceGraphUri: String,
    val reasoningGraphUri: String?,
    val aiAuditGraphUri: String,
    val actionAuditGraphUri: String,
) {
    init {
        ManagedGraphUri.requireKind(canonicalGraphUri, ManagedGraphKind.CANONICAL, "canonicalGraphUri")
        ManagedGraphUri.requireKind(provenanceGraphUri, ManagedGraphKind.PROVENANCE, "provenanceGraphUri")
        reasoningGraphUri?.let { ManagedGraphUri.requireKind(it, ManagedGraphKind.REASONING, "reasoningGraphUri") }
        ManagedGraphUri.requireKind(aiAuditGraphUri, ManagedGraphKind.AI_AUDIT, "aiAuditGraphUri")
        ManagedGraphUri.requireKind(actionAuditGraphUri, ManagedGraphKind.ACTION_AUDIT, "actionAuditGraphUri")
    }

    fun actionGraphs(): OntologyActionGraphUris {
        return OntologyActionGraphUris(
            canonicalGraphUri = canonicalGraphUri,
            provenanceGraphUri = provenanceGraphUri,
            reasoningGraphUri = reasoningGraphUri,
            actionAuditGraphUri = actionAuditGraphUri,
        )
    }

    companion object {
        fun forRelease(
            sourceReleaseId: String,
            reasoningRunId: String?,
            aiAuditReleaseId: String,
            actionAuditReleaseId: String,
        ): AiGovernanceReviewGraphUris {
            return AiGovernanceReviewGraphUris(
                canonicalGraphUri = ManagedGraphUri.of(ManagedGraphKind.CANONICAL, sourceReleaseId, "sourceReleaseId").value,
                provenanceGraphUri = ManagedGraphUri.of(ManagedGraphKind.PROVENANCE, sourceReleaseId, "sourceReleaseId").value,
                reasoningGraphUri = reasoningRunId?.let { ManagedGraphUri.of(ManagedGraphKind.REASONING, it, "reasoningRunId").value },
                aiAuditGraphUri = ManagedGraphUri.of(ManagedGraphKind.AI_AUDIT, aiAuditReleaseId, "aiAuditReleaseId").value,
                actionAuditGraphUri = ManagedGraphUri.of(ManagedGraphKind.ACTION_AUDIT, actionAuditReleaseId, "actionAuditReleaseId").value,
            )
        }
    }
}

data class AiGovernanceReviewPlan(
    val request: AiGovernanceReviewRequest,
    val graphs: AiGovernanceReviewGraphUris,
)

data class AiGovernanceReviewResult(
    val reviewed: Boolean,
    val decision: AiGovernanceReviewDecision,
    val validation: AiGovernanceValidationReport,
    val aiAuditGraphUri: String,
    val actionAuditGraphUri: String? = null,
    val writtenGraphUris: List<String> = emptyList(),
    val idempotentReplay: Boolean = false,
    val actionRequestCreated: Boolean = false,
    val actionRequestId: String? = null,
    val actionId: String? = null,
    val actionResult: OntologyActionAuditResult? = null,
    val rollbackAttempted: Boolean = false,
    val rollbackSucceeded: Boolean = false,
    val errors: List<String> = emptyList(),
)

interface AiGovernanceReviewSubmitter {
    fun submit(plan: AiGovernanceReviewPlan): AiGovernanceReviewResult
}

class AiGovernanceReviewService(
    private val validationGate: AiGovernanceProposalValidationGate,
    private val graphStore: NamedGraphStore,
    private val actionSubmitter: OntologyActionSubmitter,
) : AiGovernanceReviewSubmitter {
    private val graphWrites = ManagedGraphWriteCoordinator(graphStore)

    override fun submit(plan: AiGovernanceReviewPlan): AiGovernanceReviewResult {
        val snapshots = runCatching {
            AiGovernanceReviewGraphSnapshots(
                canonical = graphStore.readNamedGraph(plan.graphs.canonicalGraphUri),
                provenance = graphStore.readNamedGraph(plan.graphs.provenanceGraphUri),
                reasoning = plan.graphs.reasoningGraphUri?.let(graphStore::readNamedGraph),
                aiAudit = graphStore.readNamedGraph(plan.graphs.aiAuditGraphUri),
            )
        }.getOrElse { error ->
            return failed(plan, "AI governance review graph snapshot failed: ${error.message}")
        }

        if (snapshots.aiAudit.model.listSubjectsWithProperty(Dcai.hasIdempotencyKey, plan.request.idempotencyKey).hasNext()) {
            return AiGovernanceReviewResult(
                reviewed = true,
                decision = plan.request.decision,
                validation = AiGovernanceValidationReport(conforms = true, tripleCount = snapshots.aiAudit.model.size().toInt()),
                aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                idempotentReplay = true,
            )
        }

        val proposal = ResourceFactory.createResource(plan.request.proposalUri)
        val proposalFacts = extractProposalFacts(proposal, snapshots)
            ?: return failed(plan, "AI proposal is missing from managed ai-audit graph: ${plan.request.proposalUri}")
        val preconditionErrors = validateReviewPreconditions(plan.request, proposalFacts)
        if (preconditionErrors.isNotEmpty()) {
            return AiGovernanceReviewResult(
                reviewed = false,
                decision = plan.request.decision,
                validation = AiGovernanceValidationReport(conforms = false, errors = preconditionErrors),
                aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                errors = preconditionErrors,
            )
        }

        val candidate = ModelFactory.createDefaultModel().add(snapshots.aiAudit.model)
        updateProposalReviewState(candidate, proposal, plan.request)
        candidate.add(mapReviewDecision(plan.request, proposal))
        val validation = validationGate.validate(candidate)
        if (!validation.conforms) {
            return AiGovernanceReviewResult(
                reviewed = false,
                decision = plan.request.decision,
                validation = validation,
                aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                errors = validation.errors,
            )
        }

        val write = graphWrites.replaceAll(
            graphModels = mapOf(plan.graphs.aiAuditGraphUri to candidate),
            snapshots = mapOf(plan.graphs.aiAuditGraphUri to snapshots.aiAudit),
            writeFailurePrefix = "AI governance review graph write failed",
            rollbackFailurePrefix = "AI governance review rollback failed",
        )
        return if (write.succeeded) {
            submitActionIfRequired(plan, proposalFacts, snapshots.aiAudit, validation)
        } else {
            AiGovernanceReviewResult(
                reviewed = false,
                decision = plan.request.decision,
                validation = validation,
                aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                rollbackAttempted = write.rollbackAttempted,
                rollbackSucceeded = write.rollbackSucceeded,
                errors = write.errors,
            )
        }
    }

    private fun submitActionIfRequired(
        plan: AiGovernanceReviewPlan,
        proposalFacts: AiProposalFacts,
        aiAuditSnapshot: NamedGraphSnapshot,
        validation: AiGovernanceValidationReport,
    ): AiGovernanceReviewResult {
        if (plan.request.decision != AiGovernanceReviewDecision.APPROVE || proposalFacts.proposalType != AiProposalType.ACTION_RECOMMENDATION.id) {
            return AiGovernanceReviewResult(
                reviewed = true,
                decision = plan.request.decision,
                validation = validation,
                aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                writtenGraphUris = listOf(plan.graphs.aiAuditGraphUri),
            )
        }

        val actionType = plan.request.actionType
            ?: inferActionType(proposalFacts)
            ?: return rollbackAfterActionFailure(
                plan = plan,
                snapshot = aiAuditSnapshot,
                validation = validation,
                message = "Approved ACTION_RECOMMENDATION proposal could not be mapped to a governed ontology action type.",
            )

        val actionRequest = actionRequest(plan.request, proposalFacts, actionType)
        val actionResult = actionSubmitter.submit(
            OntologyActionAuditPlan(
                request = actionRequest,
                graphs = plan.graphs.actionGraphs(),
            ),
        )
        if (!actionResult.audited) {
            return rollbackAfterActionFailure(
                plan = plan,
                snapshot = aiAuditSnapshot,
                validation = validation,
                message = actionResult.errors.joinToString(separator = "; ").ifBlank {
                    actionResult.validation.errors.joinToString(separator = "; ").ifBlank {
                        "Generated ontology action request failed validation."
                    }
                },
                actionResult = actionResult,
            )
        }
        return AiGovernanceReviewResult(
            reviewed = true,
            decision = plan.request.decision,
            validation = validation,
            aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
            actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
            writtenGraphUris = listOf(plan.graphs.aiAuditGraphUri) + actionResult.writtenGraphUris,
            actionRequestCreated = true,
            actionRequestId = actionRequest.requestId,
            actionId = actionType.id,
            actionResult = actionResult,
        )
    }

    private fun rollbackAfterActionFailure(
        plan: AiGovernanceReviewPlan,
        snapshot: NamedGraphSnapshot,
        validation: AiGovernanceValidationReport,
        message: String,
        actionResult: OntologyActionAuditResult? = null,
    ): AiGovernanceReviewResult {
        val rollbackErrors = graphWrites.rollback(
            graphUri = plan.graphs.aiAuditGraphUri,
            snapshot = snapshot,
            rollbackFailurePrefix = "AI governance review rollback failed",
        )
        return AiGovernanceReviewResult(
            reviewed = false,
            decision = plan.request.decision,
            validation = validation,
            aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
            actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
            actionResult = actionResult,
            rollbackAttempted = true,
            rollbackSucceeded = rollbackErrors.isEmpty(),
            errors = listOf("AI governance review action handoff failed: $message") + rollbackErrors,
        )
    }

    private fun validateReviewPreconditions(
        request: AiGovernanceReviewRequest,
        proposalFacts: AiProposalFacts,
    ): List<String> {
        val errors = mutableListOf<String>()
        if (!ControlledIdentifier.isLocal(request.reviewId)) errors += "reviewId must use the controlled local identifier vocabulary"
        if (!ControlledIdentifier.isLocal(request.idempotencyKey)) errors += "idempotencyKey must use the controlled local identifier vocabulary"
        if (!ControlledIdentifier.isLocal(request.actorId)) errors += "actorId must use the controlled local identifier vocabulary"
        if (proposalFacts.reviewStatus != "PENDING_HUMAN_REVIEW") {
            errors += "AI proposal has already been reviewed: ${proposalFacts.reviewStatus}"
        }
        if (request.decision == AiGovernanceReviewDecision.APPROVE &&
            proposalFacts.proposalType == AiProposalType.ACTION_RECOMMENDATION.id &&
            request.actionType != null &&
            request.actionType != OntologyActionType.ACKNOWLEDGE_RESTORE_BLOCKER
        ) {
            errors += "AI proposal review v1 supports approved action recommendations only for AcknowledgeRestoreBlocker"
        }
        return errors
    }

    private fun extractProposalFacts(
        proposal: Resource,
        snapshots: AiGovernanceReviewGraphSnapshots,
    ): AiProposalFacts? {
        if (!snapshots.aiAudit.model.contains(proposal, RDF.type, Dcai.AIProposal)) {
            return null
        }
        val usedResources = snapshots.aiAudit.model.listObjectsOfProperty(proposal, Prov.used).toList()
            .filter { it.isURIResource }
            .map { it.asResource() }
        val targetResources = snapshots.aiAudit.model.listObjectsOfProperty(proposal, Dcai.hasTargetObject).toList()
            .filter { it.isURIResource }
            .map { it.asResource() }
        val sourceRecord = usedResources.firstOrNull { used ->
            snapshots.provenance.model.contains(used, RDF.type, Dcai.SourceRecord)
        } ?: return null
        val incident = targetResources.firstOrNull { target ->
            snapshots.canonical.model.contains(target, RDF.type, Dcai.InfrastructureIncident)
        } ?: return null
        val supportingEvidence = usedResources.firstOrNull { used ->
            used != sourceRecord && (snapshots.reasoning?.model?.contains(used, RDF.type) == true || snapshots.canonical.model.contains(used, RDF.type))
        } ?: return null
        return AiProposalFacts(
            proposal = proposal,
            proposalId = snapshots.aiAudit.model.requiredString(proposal, Dcai.hasIdentifier),
            proposalType = snapshots.aiAudit.model.requiredString(proposal, Dcai.hasProposalType),
            reviewStatus = snapshots.aiAudit.model.requiredString(proposal, Dcai.hasAIGovernanceReviewStatus),
            summary = snapshots.aiAudit.model.requiredString(proposal, Dcai.hasProposalSummary),
            rationale = snapshots.aiAudit.model.requiredString(proposal, Dcai.hasProposalRationale),
            incidentUri = incident.uri,
            sourceRecordUri = sourceRecord.uri,
            supportingEvidenceUri = supportingEvidence.uri,
            supportingEvidenceIsRestoreReadiness = snapshots.reasoning?.model?.contains(supportingEvidence, RDF.type, Dcai.RestoreReadinessFinding) == true,
        )
    }

    private fun inferActionType(proposalFacts: AiProposalFacts): OntologyActionType? {
        return if (proposalFacts.supportingEvidenceIsRestoreReadiness) {
            OntologyActionType.ACKNOWLEDGE_RESTORE_BLOCKER
        } else {
            null
        }
    }

    private fun actionRequest(
        request: AiGovernanceReviewRequest,
        proposalFacts: AiProposalFacts,
        actionType: OntologyActionType,
    ): OntologyActionRequest {
        return OntologyActionRequest(
            requestId = "${request.reviewId}:action-request",
            actionType = actionType,
            idempotencyKey = "${request.idempotencyKey}:action-request",
            actorId = request.actorId,
            requestedAt = request.reviewedAt.plusMillis(1),
            incidentUri = proposalFacts.incidentUri,
            actionReason = "Human approved AI proposal ${proposalFacts.proposalId}: ${request.reviewReason}",
            sourceRecordUri = proposalFacts.sourceRecordUri,
            restoreReadinessFindingUri = if (actionType == OntologyActionType.ACKNOWLEDGE_RESTORE_BLOCKER) {
                proposalFacts.supportingEvidenceUri
            } else {
                null
            },
            reviewSummary = proposalFacts.summary,
            supportingEvidenceUri = proposalFacts.supportingEvidenceUri,
        )
    }

    private fun updateProposalReviewState(
        model: Model,
        proposal: Resource,
        request: AiGovernanceReviewRequest,
    ) {
        model.removeAll(proposal, Dcai.hasProposalStatus, null)
        model.removeAll(proposal, Dcai.hasAIGovernanceReviewStatus, null)
        model.removeAll(proposal, Dcai.hasAIGovernanceDisabledReason, null)
        model.add(proposal, Dcai.hasProposalStatus, request.decision.reviewStatus)
        model.add(proposal, Dcai.hasAIGovernanceReviewStatus, request.decision.reviewStatus)
        model.add(proposal, Dcai.hasAIGovernanceDisabledReason, "AI proposal review decision is recorded; further mutation requires a separate governed action lifecycle.")
    }

    private fun mapReviewDecision(
        request: AiGovernanceReviewRequest,
        proposal: Resource,
    ): Model {
        val model = ModelFactory.createDefaultModel()
        val decision = ResourceFactory.createResource("urn:dcai:ai-approval-decision:${encode(request.idempotencyKey)}")
        model.add(decision, RDF.type, Dcai.AIApprovalDecision)
        model.add(decision, Dcai.hasIdentifier, request.reviewId)
        model.add(decision, Dcai.hasIdempotencyKey, request.idempotencyKey)
        model.add(decision, Dcai.hasApprovalState, request.decision.reviewStatus)
        model.add(decision, Dcai.hasActorId, request.actorId)
        model.add(decision, Dcai.hasReviewSummary, request.reviewReason)
        model.add(decision, Dcai.hasTargetObject, proposal)
        model.add(decision, Prov.used, proposal)
        model.add(decision, Prov.wasDerivedFrom, proposal)
        model.add(decision, Prov.generatedAtTime, ResourceFactory.createTypedLiteral(request.reviewedAt.toString(), XSDDatatype.XSDdateTime))
        return model
    }

    private fun failed(
        plan: AiGovernanceReviewPlan,
        message: String,
    ): AiGovernanceReviewResult {
        return AiGovernanceReviewResult(
            reviewed = false,
            decision = plan.request.decision,
            validation = AiGovernanceValidationReport(conforms = false, errors = listOf(message)),
            aiAuditGraphUri = plan.graphs.aiAuditGraphUri,
            actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
            errors = listOf(message),
        )
    }

    private fun Model.requiredString(resource: Resource, property: org.apache.jena.rdf.model.Property): String {
        return listObjectsOfProperty(resource, property).toList().firstOrNull()?.asLiteral()?.string
            ?: error("AI proposal is missing required ${property.localName}: ${resource.uri}")
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private data class AiGovernanceReviewGraphSnapshots(
        val canonical: NamedGraphSnapshot,
        val provenance: NamedGraphSnapshot,
        val reasoning: NamedGraphSnapshot?,
        val aiAudit: NamedGraphSnapshot,
    )

    private data class AiProposalFacts(
        val proposal: Resource,
        val proposalId: String,
        val proposalType: String,
        val reviewStatus: String,
        val summary: String,
        val rationale: String,
        val incidentUri: String,
        val sourceRecordUri: String,
        val supportingEvidenceUri: String,
        val supportingEvidenceIsRestoreReadiness: Boolean,
    )

    private companion object {
    }
}
