package com.dcai.semanticservice.query

data class QueryContract(
    val definition: ApprovedQueryDefinition,
    val codec: QueryResultCodec,
    val owner: QueryOwner,
    val privateEndpointEnabled: Boolean,
    val pagingPolicy: QueryPagingPolicy?,
)

enum class QueryOwner {
    INTERNAL_INSPECTION,
    RECOVERY_QUEUE,
    RECOVERY_CASE,
    REVIEW_INBOX,
    PLATFORM_STATUS,
    LEGACY_READ_MODEL,
}

enum class QueryResultCodec {
    UNSUPPORTED,
    NAMED_GRAPH_INVENTORY,
    INCIDENT_SUMMARY,
    PROVENANCE_SOURCE_RECORDS,
    FOLLOW_UP_QUEUE,
    DASHBOARD_OVERVIEW,
    PLATFORM_STATUS,
    FILTER_METADATA,
    FOLLOW_UP_DETAIL,
    IMPACT_SUMMARY,
    TOPOLOGY_DEPENDENCIES,
    TRUST_FINDINGS,
    STAGE_BOTTLENECKS,
    ASSET_DELAY_SUMMARY,
    ZONE_DELAY_SUMMARY,
    SPARE_WAIT_SUMMARY,
    VALIDATION_SUMMARY,
    INCIDENT_EVIDENCE,
    INCIDENT_TIMELINE,
    DEPENDENCY_IMPACT,
    BLAST_RADIUS,
    ONTOLOGY_REVIEW_QUEUE,
    ACTION_AVAILABILITY,
    ACTION_AUDIT_HISTORY,
    ACTION_NOTIFICATION_QUEUE,
    ACTION_REVIEW_QUEUE,
    ACTION_TRANSITION_HISTORY,
    ACTION_DISPATCH_QUEUE,
    DYNAMIC_PLAYBACK,
    AI_PROPOSAL_REVIEW_QUEUE,
    AI_PROPOSAL_DETAIL,
}

class QueryContractRegistry private constructor(
    private val contracts: Map<String, QueryContract>,
) {
    val queryIds: Set<String> = contracts.keys
    val privateEndpointQueryIds: Set<String> = contracts.values
        .filter(QueryContract::privateEndpointEnabled)
        .mapTo(linkedSetOf()) { it.definition.id }

    fun require(queryId: String): QueryContract = contracts[queryId]
        ?: error("Unapproved query id: $queryId")

    companion object {
        fun fromManifest(
            manifest: ApprovedQueryManifest,
            requireCompleteManifest: Boolean = false,
        ): QueryContractRegistry {
            val unknownIds = manifest.entries.keys - DECLARATIONS.keys
            if (requireCompleteManifest) {
                require(unknownIds.isEmpty()) {
                    "Approved query manifest has no contract declarations for: ${unknownIds.sorted().joinToString()}"
                }
            }
            if (requireCompleteManifest) {
                val missingIds = DECLARATIONS.keys - manifest.entries.keys
                require(missingIds.isEmpty()) {
                    "Query contract registry declarations are missing from the approved manifest: ${missingIds.sorted().joinToString()}"
                }
            }
            return QueryContractRegistry(
                manifest.entries.mapValues { (id, definition) ->
                    val declaration = DECLARATIONS[id] ?: Declaration(
                        codec = QueryResultCodec.UNSUPPORTED,
                        owner = QueryOwner.INTERNAL_INSPECTION,
                    )
                    QueryContract(
                        definition = definition,
                        codec = declaration.codec,
                        owner = declaration.owner,
                        privateEndpointEnabled = declaration.privateEndpointEnabled,
                        pagingPolicy = QueryPagingPolicies.find(id),
                    )
                },
            )
        }

        private data class Declaration(
            val codec: QueryResultCodec,
            val owner: QueryOwner,
            val privateEndpointEnabled: Boolean = true,
        )

        private fun declaration(codec: QueryResultCodec, owner: QueryOwner) = Declaration(codec, owner)

        private val DECLARATIONS = linkedMapOf(
            "fixtureNamedGraphInventory" to declaration(QueryResultCodec.NAMED_GRAPH_INVENTORY, QueryOwner.INTERNAL_INSPECTION),
            "fixtureIncidentSummary" to declaration(QueryResultCodec.INCIDENT_SUMMARY, QueryOwner.INTERNAL_INSPECTION),
            "fixtureProvenanceSourceRecords" to declaration(QueryResultCodec.PROVENANCE_SOURCE_RECORDS, QueryOwner.INTERNAL_INSPECTION),
            "semanticFollowUpQueueList" to declaration(QueryResultCodec.FOLLOW_UP_QUEUE, QueryOwner.RECOVERY_QUEUE),
            "semanticDashboardOverview" to declaration(QueryResultCodec.DASHBOARD_OVERVIEW, QueryOwner.RECOVERY_QUEUE),
            "semanticPlatformStatus" to declaration(QueryResultCodec.PLATFORM_STATUS, QueryOwner.PLATFORM_STATUS),
            "semanticFilterMetadata" to declaration(QueryResultCodec.FILTER_METADATA, QueryOwner.RECOVERY_QUEUE),
            "semanticFollowUpDetail" to declaration(QueryResultCodec.FOLLOW_UP_DETAIL, QueryOwner.RECOVERY_CASE),
            "semanticImpactSummary" to declaration(QueryResultCodec.IMPACT_SUMMARY, QueryOwner.LEGACY_READ_MODEL),
            "semanticTopologyDependencies" to declaration(QueryResultCodec.TOPOLOGY_DEPENDENCIES, QueryOwner.RECOVERY_CASE),
            "semanticTrustFindingList" to declaration(QueryResultCodec.TRUST_FINDINGS, QueryOwner.PLATFORM_STATUS),
            "semanticStageBottlenecks" to declaration(QueryResultCodec.STAGE_BOTTLENECKS, QueryOwner.LEGACY_READ_MODEL),
            "semanticAssetDelaySummary" to declaration(QueryResultCodec.ASSET_DELAY_SUMMARY, QueryOwner.LEGACY_READ_MODEL),
            "semanticZoneDelaySummary" to declaration(QueryResultCodec.ZONE_DELAY_SUMMARY, QueryOwner.LEGACY_READ_MODEL),
            "semanticSpareWaitSummary" to declaration(QueryResultCodec.SPARE_WAIT_SUMMARY, QueryOwner.LEGACY_READ_MODEL),
            "semanticValidationSummary" to declaration(QueryResultCodec.VALIDATION_SUMMARY, QueryOwner.RECOVERY_CASE),
            "semanticIncidentEvidence" to declaration(QueryResultCodec.INCIDENT_EVIDENCE, QueryOwner.RECOVERY_CASE),
            "semanticIncidentTimeline" to declaration(QueryResultCodec.INCIDENT_TIMELINE, QueryOwner.RECOVERY_CASE),
            "semanticDependencyImpactByAsset" to declaration(QueryResultCodec.DEPENDENCY_IMPACT, QueryOwner.RECOVERY_CASE),
            "semanticBlastRadiusByAsset" to declaration(QueryResultCodec.BLAST_RADIUS, QueryOwner.RECOVERY_CASE),
            "semanticPromotionReviewQueue" to declaration(QueryResultCodec.ONTOLOGY_REVIEW_QUEUE, QueryOwner.REVIEW_INBOX),
            "semanticReasoningReviewQueue" to declaration(QueryResultCodec.ONTOLOGY_REVIEW_QUEUE, QueryOwner.REVIEW_INBOX),
            "semanticAvailableActionsByFinding" to declaration(QueryResultCodec.ACTION_AVAILABILITY, QueryOwner.RECOVERY_CASE),
            "semanticActionAuditHistoryByRelease" to declaration(QueryResultCodec.ACTION_AUDIT_HISTORY, QueryOwner.INTERNAL_INSPECTION),
            "semanticActionAuditHistoryByIncident" to declaration(QueryResultCodec.ACTION_AUDIT_HISTORY, QueryOwner.RECOVERY_CASE),
            "semanticActionAuditHistoryByTarget" to declaration(QueryResultCodec.ACTION_AUDIT_HISTORY, QueryOwner.INTERNAL_INSPECTION),
            "semanticActionNotificationQueueByIncident" to declaration(QueryResultCodec.ACTION_NOTIFICATION_QUEUE, QueryOwner.RECOVERY_CASE),
            "semanticActionReviewQueueByIncident" to declaration(QueryResultCodec.ACTION_REVIEW_QUEUE, QueryOwner.REVIEW_INBOX),
            "semanticActionTransitionHistoryByIncident" to declaration(QueryResultCodec.ACTION_TRANSITION_HISTORY, QueryOwner.RECOVERY_CASE),
            "semanticActionDispatchQueueByIncident" to declaration(QueryResultCodec.ACTION_DISPATCH_QUEUE, QueryOwner.RECOVERY_CASE),
            "semanticDynamicEventTimelineByIncident" to declaration(QueryResultCodec.DYNAMIC_PLAYBACK, QueryOwner.RECOVERY_CASE),
            "semanticDynamicStateChangesByIncident" to declaration(QueryResultCodec.DYNAMIC_PLAYBACK, QueryOwner.RECOVERY_CASE),
            "semanticDynamicReasoningChangesByIncident" to declaration(QueryResultCodec.DYNAMIC_PLAYBACK, QueryOwner.RECOVERY_CASE),
            "semanticDynamicActionLifecycleByIncident" to declaration(QueryResultCodec.DYNAMIC_PLAYBACK, QueryOwner.RECOVERY_CASE),
            "semanticAiProposalReviewQueue" to declaration(QueryResultCodec.AI_PROPOSAL_REVIEW_QUEUE, QueryOwner.REVIEW_INBOX),
            "semanticAiProposalDetailByIncident" to declaration(QueryResultCodec.AI_PROPOSAL_DETAIL, QueryOwner.RECOVERY_CASE),
        )

        val declaredQueryIds: Set<String> = DECLARATIONS.keys
    }
}
