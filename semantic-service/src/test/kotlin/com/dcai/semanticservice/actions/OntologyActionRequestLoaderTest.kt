package com.dcai.semanticservice.actions

import com.dcai.semanticservice.runtime.SemanticServiceComposition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OntologyActionRequestLoaderTest {
    private val repoRoot = SemanticServiceComposition.locateRepoRoot()

    @Test
    fun parsesControlledLocalActionRequestFixture() {
        val request = OntologyActionRequestLoader().load(
            repoRoot.resolve("fixtures/action-requests/acknowledge-restore-blocker.properties"),
        )

        assertEquals(OntologyActionType.ACKNOWLEDGE_RESTORE_BLOCKER, request.actionType)
        assertEquals("ACT-REQ-ACK-LOCAL-001", request.requestId)
        assertEquals("operator-local-reviewer", request.actorId)
        assertEquals("urn:dcai:incident:INC-001", request.incidentUri)
    }

    @Test
    fun actionRequestFileMustResolveUnderControlledDirectory() {
        assertEquals(
            repoRoot.resolve("fixtures/action-requests/acknowledge-restore-blocker.properties"),
            SemanticServiceComposition.resolveControlledActionRequestPath(
                repoRoot = repoRoot,
                actionRequestPathArgument = "fixtures/action-requests/acknowledge-restore-blocker.properties",
            ),
        )
        assertFailsWith<IllegalArgumentException> {
            SemanticServiceComposition.resolveControlledActionRequestPath(
                repoRoot = repoRoot,
                actionRequestPathArgument = "../uncontrolled-action.properties",
            )
        }
    }
}
