package com.dcai.semanticservice.query

import com.dcai.semanticservice.runtime.SemanticServiceComposition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.io.path.readText

class QueryContractRegistryTest {
    private val manifest = ApprovedQueryCatalog(SemanticServiceComposition.locateRepoRoot()).load()
    private val registry = QueryContractRegistry.fromManifest(manifest, requireCompleteManifest = true)

    @Test
    fun approvedManifestAndRegistryAreCompleteAndEndpointApprovalIsDerived() {
        assertEquals(manifest.entries.keys, QueryContractRegistry.declaredQueryIds)
        assertEquals(manifest.entries.keys, registry.queryIds)
        assertEquals(manifest.entries.keys, registry.privateEndpointQueryIds)
    }

    @Test
    fun registryOwnsCodecFeatureAndPagingMetadata() {
        val trust = registry.require("semanticTrustFindingList")
        assertEquals(QueryResultCodec.TRUST_FINDINGS, trust.codec)
        assertEquals(QueryOwner.PLATFORM_STATUS, trust.owner)
        assertNotNull(trust.pagingPolicy)

        val caseTimeline = registry.require("semanticIncidentTimeline")
        assertEquals(QueryResultCodec.INCIDENT_TIMELINE, caseTimeline.codec)
        assertEquals(QueryOwner.RECOVERY_CASE, caseTimeline.owner)
        assertEquals(null, caseTimeline.pagingPolicy)

        assertEquals(QueryPagingPolicies.queryIds, registry.queryIds.filterTo(mutableSetOf()) {
            registry.require(it).pagingPolicy != null
        })
    }

    @Test
    fun frontendCatalogMatchesAllFeatureOwnedRuntimeContracts() {
        val frontendCatalog = SemanticServiceComposition.locateRepoRoot()
            .resolve("frontend/src/semanticQueryCatalog.ts")
            .readText()
        val frontendIds = Regex("'((?:semantic)[A-Za-z0-9]+)'")
            .findAll(frontendCatalog.substringBefore("export type SemanticQueryId"))
            .map { match -> match.groupValues[1] }
            .toSet()
        val featureOwnedIds = registry.queryIds.filterTo(mutableSetOf()) { queryId ->
            registry.require(queryId).owner !in setOf(QueryOwner.INTERNAL_INSPECTION, QueryOwner.LEGACY_READ_MODEL)
        }

        assertEquals(featureOwnedIds, frontendIds)
    }
}
