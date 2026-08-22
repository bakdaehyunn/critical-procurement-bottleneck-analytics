package com.dcai.semanticservice.query

data class QueryPagingPolicy(
    val identityBindings: List<String>,
    val sortBindings: List<QueryPagingSort>,
) {
    init {
        require(identityBindings.isNotEmpty()) { "Paged queries require at least one stable identity binding." }
        require(identityBindings.all(BINDING_NAME::matches)) { "Paged query identity bindings must be SPARQL variable names." }
        require(sortBindings.all { BINDING_NAME.matches(it.binding) }) { "Paged query sort bindings must be SPARQL variable names." }
    }

    companion object {
        private val BINDING_NAME = Regex("[A-Za-z][A-Za-z0-9_]*")
    }
}

data class QueryPagingSort(
    val binding: String,
    val direction: QuerySortDirection,
)

enum class QuerySortDirection {
    ASCENDING,
    DESCENDING,
}

object QueryPagingPolicies {
    private val policies = mapOf(
        "semanticTrustFindingList" to QueryPagingPolicy(
            identityBindings = listOf("trustFindingId"),
            sortBindings = listOf(QueryPagingSort("createdAt", QuerySortDirection.DESCENDING)),
        ),
        "semanticActionReviewQueueByIncident" to QueryPagingPolicy(
            identityBindings = listOf("execution"),
            sortBindings = listOf(QueryPagingSort("stateGeneratedAt", QuerySortDirection.DESCENDING)),
        ),
        "semanticAiProposalReviewQueue" to QueryPagingPolicy(
            identityBindings = listOf("proposal"),
            sortBindings = listOf(QueryPagingSort("generatedAt", QuerySortDirection.DESCENDING)),
        ),
        "semanticPromotionReviewQueue" to QueryPagingPolicy(
            identityBindings = listOf("queueKind", "reviewActionId", "targetUri", "releaseId"),
            sortBindings = listOf(
                QueryPagingSort("prioritySortOrder", QuerySortDirection.ASCENDING),
                QueryPagingSort("targetLabel", QuerySortDirection.ASCENDING),
            ),
        ),
        "semanticReasoningReviewQueue" to QueryPagingPolicy(
            identityBindings = listOf("queueKind", "reviewActionId", "targetUri", "releaseId"),
            sortBindings = listOf(
                QueryPagingSort("prioritySortOrder", QuerySortDirection.ASCENDING),
                QueryPagingSort("targetLabel", QuerySortDirection.ASCENDING),
            ),
        ),
    )

    fun requireFor(queryId: String): QueryPagingPolicy {
        return find(queryId)
            ?: throw IllegalArgumentException("Query $queryId does not declare a stable paging policy.")
    }

    fun find(queryId: String): QueryPagingPolicy? = policies[queryId]

    val queryIds: Set<String> = policies.keys
}
