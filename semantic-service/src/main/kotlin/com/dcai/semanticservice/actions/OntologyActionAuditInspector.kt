package com.dcai.semanticservice.actions

import com.dcai.semanticservice.graph.NamedGraphStore
import com.dcai.semanticservice.ingestion.Dcai
import com.dcai.semanticservice.ingestion.Prov
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.RDF

open class OntologyActionAuditInspector(
    private val graphStore: NamedGraphStore,
) {
    open fun inspect(plan: OntologyActionAuditInspectionPlan): OntologyActionAuditInspectionResult {
        return runCatching {
            val graphUri = OntologyActionGraphUris.ACTION_AUDIT_PREFIX + plan.actionAuditReleaseId
            val snapshot = graphStore.readNamedGraph(graphUri)
            OntologyActionAuditInspectionResult(
                actionAuditReleaseId = plan.actionAuditReleaseId,
                actionAuditGraphUri = graphUri,
                exists = snapshot.exists,
                tripleCount = snapshot.model.size().toInt(),
                executionCount = snapshot.model.countType(Dcai.OntologyActionExecution),
                requestCount = snapshot.model.countType(Dcai.OntologyActionRequest),
                validationReportCount = snapshot.model.countType(Dcai.ActionValidationReport),
                notificationCount = snapshot.model.countType(Dcai.OntologyActionNotification),
                actionTypeCounts = snapshot.model.actionTypeCounts(),
                idempotencyKeyCount = snapshot.model
                    .listObjectsOfProperty(Dcai.hasIdempotencyKey)
                    .toList()
                    .map { it.toString() }
                    .distinct()
                    .size,
                latestGeneratedAt = snapshot.model
                    .listObjectsOfProperty(Prov.generatedAtTime)
                    .toList()
                    .map { it.toString() }
                    .maxOrNull(),
            )
        }.getOrElse { error ->
            OntologyActionAuditInspectionResult(
                actionAuditReleaseId = plan.actionAuditReleaseId,
                actionAuditGraphUri = OntologyActionGraphUris.ACTION_AUDIT_PREFIX + plan.actionAuditReleaseId,
                errors = listOf("Action audit inspection failed: ${error.message}"),
            )
        }
    }

    private fun Model.countType(type: Resource): Int {
        return listSubjectsWithProperty(RDF.type, type).toList().distinct().size
    }

    private fun Model.actionTypeCounts(): Map<String, Int> {
        return listSubjectsWithProperty(RDF.type, Dcai.OntologyActionExecution)
            .toList()
            .mapNotNull { execution ->
                listObjectsOfProperty(execution, Dcai.hasActionType)
                    .toList()
                    .firstOrNull()
                    ?.asResource()
                    ?.let { actionType ->
                        listObjectsOfProperty(actionType, Dcai.hasIdentifier).toList().firstOrNull()?.toString()
                    }
            }
            .groupingBy { it }
            .eachCount()
    }
}

data class OntologyActionAuditInspectionPlan(
    val actionAuditReleaseId: String,
) {
    init {
        require(actionAuditReleaseId.matches(Regex("[A-Za-z0-9._-]+"))) {
            "actionAuditReleaseId must contain only letters, numbers, dot, underscore, or hyphen"
        }
    }
}

data class OntologyActionAuditInspectionResult(
    val actionAuditReleaseId: String,
    val actionAuditGraphUri: String,
    val exists: Boolean = false,
    val tripleCount: Int = 0,
    val executionCount: Int = 0,
    val requestCount: Int = 0,
    val validationReportCount: Int = 0,
    val notificationCount: Int = 0,
    val actionTypeCounts: Map<String, Int> = emptyMap(),
    val idempotencyKeyCount: Int = 0,
    val latestGeneratedAt: String? = null,
    val errors: List<String> = emptyList(),
) {
    val inspected: Boolean = errors.isEmpty()
}
