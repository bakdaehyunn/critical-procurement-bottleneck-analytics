package com.dcai.semanticservice.actions

import com.dcai.semanticservice.graph.NamedGraphSnapshot
import com.dcai.semanticservice.graph.NamedGraphStore
import com.dcai.semanticservice.ingestion.Dcai
import org.apache.jena.rdf.model.ModelFactory

interface OntologyActionSubmitter {
    fun submit(plan: OntologyActionAuditPlan): OntologyActionAuditResult
}

class OntologyActionAuditService(
    private val mapper: OntologyActionRdfMapper,
    private val preconditionValidator: OntologyActionPreconditionValidator,
    private val validationGate: OntologyActionValidationGate,
    private val graphStore: NamedGraphStore,
) : OntologyActionSubmitter {
    override fun submit(plan: OntologyActionAuditPlan): OntologyActionAuditResult {
        val snapshots = runCatching {
            ActionGraphSnapshots(
                canonical = graphStore.readNamedGraph(plan.graphs.canonicalGraphUri),
                provenance = graphStore.readNamedGraph(plan.graphs.provenanceGraphUri),
                reasoning = plan.graphs.reasoningGraphUri?.let(graphStore::readNamedGraph),
                actionAudit = graphStore.readNamedGraph(plan.graphs.actionAuditGraphUri),
            )
        }.getOrElse { error ->
            return OntologyActionAuditResult(
                audited = false,
                validation = OntologyActionValidationReport(conforms = false, errors = listOf("Action graph snapshot failed: ${error.message}")),
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                errors = listOf("Action graph snapshot failed: ${error.message}"),
            )
        }

        if (snapshots.actionAudit.model.listSubjectsWithProperty(Dcai.hasIdempotencyKey, plan.request.idempotencyKey).hasNext()) {
            return OntologyActionAuditResult(
                audited = true,
                validation = OntologyActionValidationReport(conforms = true, tripleCount = snapshots.actionAudit.model.size().toInt()),
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
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
            return OntologyActionAuditResult(
                audited = false,
                validation = OntologyActionValidationReport(
                    conforms = false,
                    tripleCount = 0,
                    errors = preconditionErrors,
                ),
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                errors = preconditionErrors,
            )
        }

        val candidate = mapper.map(plan.request)
        val combined = ModelFactory.createDefaultModel()
            .add(snapshots.actionAudit.model)
            .add(candidate)
        val validation = validationGate.validate(combined)
        if (!validation.conforms) {
            return OntologyActionAuditResult(
                audited = false,
                validation = validation,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                errors = validation.errors,
            )
        }

        return runCatching {
            graphStore.replaceNamedGraph(plan.graphs.actionAuditGraphUri, combined)
            OntologyActionAuditResult(
                audited = true,
                validation = validation,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                writtenGraphUris = listOf(plan.graphs.actionAuditGraphUri),
            )
        }.getOrElse { writeError ->
            val rollbackErrors = rollback(plan.graphs.actionAuditGraphUri, snapshots.actionAudit)
            OntologyActionAuditResult(
                audited = false,
                validation = validation,
                actionAuditGraphUri = plan.graphs.actionAuditGraphUri,
                rollbackAttempted = true,
                rollbackSucceeded = rollbackErrors.isEmpty(),
                errors = listOf("Action audit graph write failed: ${writeError.message}") + rollbackErrors,
            )
        }
    }

    private fun rollback(graphUri: String, snapshot: NamedGraphSnapshot): List<String> {
        return runCatching {
            if (snapshot.exists) {
                graphStore.replaceNamedGraph(graphUri, snapshot.copyModel())
            } else {
                graphStore.deleteNamedGraph(graphUri)
            }
        }.exceptionOrNull()?.let { error ->
            listOf("Action audit rollback failed for $graphUri: ${error.message}")
        } ?: emptyList()
    }

    private data class ActionGraphSnapshots(
        val canonical: NamedGraphSnapshot,
        val provenance: NamedGraphSnapshot,
        val reasoning: NamedGraphSnapshot?,
        val actionAudit: NamedGraphSnapshot,
    )
}
