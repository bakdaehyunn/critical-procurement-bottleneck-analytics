package com.dcai.semanticservice.query

import com.dcai.semanticservice.runtime.SemanticServiceApplication
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ApprovedQueryCatalogTest {
    private val repoRoot = SemanticServiceApplication.locateRepoRoot()

    @Test
    fun loadsOnlyPhaseSixteenApprovedReadOnlyQueries() {
        val manifest = ApprovedQueryCatalog(repoRoot).load()

        assertEquals(
            setOf(
                "fixtureNamedGraphInventory",
                "fixtureIncidentSummary",
                "fixtureProvenanceSourceRecords",
                "semanticFollowUpQueueList",
                "semanticDashboardOverview",
                "semanticFilterMetadata",
                "semanticFollowUpDetail",
                "semanticImpactSummary",
                "semanticTopologyDependencies",
                "semanticTrustFindingList",
                "semanticStageBottlenecks",
                "semanticAssetDelaySummary",
                "semanticZoneDelaySummary",
                "semanticSpareWaitSummary",
                "semanticValidationSummary",
                "semanticIncidentEvidence",
                "semanticIncidentTimeline",
                "semanticDependencyImpactByAsset",
                "semanticBlastRadiusByAsset",
                "semanticPromotionReviewQueue",
                "semanticReasoningReviewQueue",
                "semanticAvailableActionsByFinding",
                "semanticActionAuditHistoryByRelease",
                "semanticActionAuditHistoryByIncident",
                "semanticActionAuditHistoryByTarget",
                "semanticActionNotificationQueueByIncident",
                "semanticActionReviewQueueByIncident",
                "semanticActionTransitionHistoryByIncident",
                "semanticActionDispatchQueueByIncident",
                "semanticDynamicEventTimelineByIncident",
                "semanticDynamicStateChangesByIncident",
                "semanticDynamicReasoningChangesByIncident",
                "semanticDynamicActionLifecycleByIncident",
                "semanticAiProposalReviewQueue",
                "semanticAiProposalDetailByIncident",
            ),
            manifest.entries.keys,
        )
        assertTrue(manifest.entries.values.all { it.mode == QueryMode.SELECT })
        assertEquals(
            "fixture or promoted canonical graph, reasoning graph",
            manifest.requireQuery("semanticFollowUpDetail").graphScope,
        )
        assertTrue(manifest.requireQuery("semanticFollowUpDetail").sparql.contains("urn:dcai:graph:canonical:"))
        assertTrue(manifest.requireQuery("semanticFollowUpDetail").sparql.contains("urn:dcai:graph:reasoning:"))
        assertEquals(
            "fixture canonical graph or reasoning graph",
            manifest.requireQuery("semanticTrustFindingList").graphScope,
        )
        assertTrue(manifest.requireQuery("semanticTrustFindingList").sparql.contains("urn:dcai:graph:reasoning:"))
        assertEquals(
            "fixture or promoted canonical graph, reasoning graph",
            manifest.requireQuery("semanticAvailableActionsByFinding").graphScope,
        )
        assertTrue(manifest.requireQuery("semanticAvailableActionsByFinding").sparql.contains("AcknowledgeRestoreBlocker"))
        assertEquals(
            "fixture or promoted source, canonical, and provenance graph lifecycle state",
            manifest.requireQuery("semanticPromotionReviewQueue").graphScope,
        )
        assertTrue(manifest.requireQuery("semanticPromotionReviewQueue").sparql.contains("ApprovePromotionBatch"))
        assertEquals(
            "promoted canonical graph, reasoning-audit graph, reasoning graph",
            manifest.requireQuery("semanticReasoningReviewQueue").graphScope,
        )
        assertTrue(manifest.requireQuery("semanticReasoningReviewQueue").sparql.contains("RequestReasoningRefresh"))
        assertEquals(
            "managed action-audit graph, promoted canonical graph",
            manifest.requireQuery("semanticActionAuditHistoryByIncident").graphScope,
        )
        assertTrue(manifest.requireQuery("semanticActionAuditHistoryByIncident").sparql.contains("urn:dcai:graph:action-audit:"))
        assertEquals(
            "managed action-audit notification state, promoted canonical graph",
            manifest.requireQuery("semanticActionNotificationQueueByIncident").graphScope,
        )
        assertTrue(manifest.requireQuery("semanticActionNotificationQueueByIncident").sparql.contains("OntologyActionNotification"))
        assertEquals(
            "managed action-audit lifecycle state, promoted canonical graph",
            manifest.requireQuery("semanticActionReviewQueueByIncident").graphScope,
        )
        assertTrue(manifest.requireQuery("semanticActionReviewQueueByIncident").sparql.contains("OntologyActionStateTransition"))
        assertEquals(
            "managed action-audit lifecycle state, promoted canonical graph",
            manifest.requireQuery("semanticActionTransitionHistoryByIncident").graphScope,
        )
        assertTrue(manifest.requireQuery("semanticActionTransitionHistoryByIncident").sparql.contains("hasFromActionState"))
        assertEquals(
            "managed action-audit dispatch simulation state, promoted canonical graph",
            manifest.requireQuery("semanticActionDispatchQueueByIncident").graphScope,
        )
        assertTrue(manifest.requireQuery("semanticActionDispatchQueueByIncident").sparql.contains("OntologyActionDispatch"))
        assertEquals(
            "managed action-audit dynamic playback state, promoted source/canonical/provenance/reasoning graph references",
            manifest.requireQuery("semanticDynamicEventTimelineByIncident").graphScope,
        )
        assertTrue(manifest.requireQuery("semanticDynamicEventTimelineByIncident").sparql.contains("DynamicPlaybackEvent"))
        assertTrue(manifest.requireQuery("semanticDynamicReasoningChangesByIncident").sparql.contains("hasAfterReasoningState"))
        assertTrue(manifest.requireQuery("semanticDynamicActionLifecycleByIncident").sparql.contains("hasActionLifecycleState"))
        assertEquals(
            "managed ai-audit graph, promoted canonical/provenance graph",
            manifest.requireQuery("semanticAiProposalReviewQueue").graphScope,
        )
        assertTrue(manifest.requireQuery("semanticAiProposalReviewQueue").sparql.contains("AIProposal"))
        assertTrue(manifest.requireQuery("semanticAiProposalReviewQueue").sparql.contains("SourceRecord"))
        assertTrue(manifest.requireQuery("semanticAiProposalDetailByIncident").sparql.contains("incidentIdParam"))
    }

    @Test
    fun rejectsUnapprovedPlaceholderQueryIds() {
        val manifest = ApprovedQueryCatalog(repoRoot).load()

        assertFailsWith<IllegalStateException> {
            manifest.requireQuery("dependencyExposureReasoning")
        }
    }

    @Test
    fun rejectsApprovedNonReadOnlyQueryModes() {
        val tempRepo = Files.createTempDirectory("phase16-query-catalog-test")
        tempRepo.resolve("queries/inspection").toFile().mkdirs()
        tempRepo.resolve("queries/inspection/bad.construct.rq").toFile().writeText(
            """
            CONSTRUCT { ?s ?p ?o }
            WHERE { ?s ?p ?o }
            """.trimIndent(),
        )
        tempRepo.resolve("queries/manifest.ttl").toFile().writeText(
            """
            @prefix dcai-query: <urn:dcai:query:> .
            @prefix dcterms: <http://purl.org/dc/terms/> .
            @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .

            dcai-query:badRuntimeConstruct
              rdf:type dcai-query:QueryEntry ;
              dcterms:title "Bad runtime construct" ;
              dcai-query:queryPath "queries/inspection/bad.construct.rq" ;
              dcai-query:queryMode "CONSTRUCT" ;
              dcai-query:graphScope "fixture graph" ;
              dcai-query:implementationStatus "phase16-approved" .
            """.trimIndent(),
        )

        assertFailsWith<IllegalArgumentException> {
            ApprovedQueryCatalog(tempRepo).load()
        }
    }
}
