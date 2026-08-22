package com.dcai.semanticservice.query

import com.dcai.semanticservice.runtime.SemanticServiceComposition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.apache.jena.query.QueryFactory

class QueryPagingPoliciesTest {
    private val repoRoot = SemanticServiceComposition.locateRepoRoot()

    @Test
    fun pageableContractsDeclareProjectedStableIdentityAndSortBindings() {
        val manifest = ApprovedQueryCatalog(repoRoot).load()
        assertEquals(
            setOf(
                "semanticTrustFindingList",
                "semanticActionReviewQueueByIncident",
                "semanticAiProposalReviewQueue",
                "semanticPromotionReviewQueue",
                "semanticReasoningReviewQueue",
            ),
            QueryPagingPolicies.queryIds,
        )

        QueryPagingPolicies.queryIds.forEach { queryId ->
            val definition = manifest.requireQuery(queryId)
            val query = QueryFactory.create(definition.sparql)
            val policy = QueryPagingPolicies.requireFor(queryId)
            val projected = query.resultVars.toSet()

            assertTrue(query.isSelectType, "$queryId must remain a SELECT query")
            assertFalse(query.hasLimit(), "$queryId must leave LIMIT to the service paging contract")
            assertFalse(query.hasOffset(), "$queryId must leave OFFSET to the service paging contract")
            assertTrue(
                projected.containsAll(policy.identityBindings + policy.sortBindings.map(QueryPagingSort::binding)),
                "$queryId must project its paging identity and ordering bindings",
            )
        }
    }
}
