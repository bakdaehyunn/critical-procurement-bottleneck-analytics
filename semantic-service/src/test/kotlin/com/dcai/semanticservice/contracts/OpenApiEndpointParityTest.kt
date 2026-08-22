package com.dcai.semanticservice.contracts

import com.dcai.semanticservice.runtime.SemanticServiceComposition
import kotlin.test.Test
import kotlin.test.assertTrue

class OpenApiEndpointParityTest {
    @Test
    fun implementedPrivateOpenApiPathsMatchRuntimeRoutes() {
        val errors = OpenApiEndpointContractValidator().validate(SemanticServiceComposition.locateRepoRoot())
        assertTrue(errors.isEmpty(), errors.joinToString(separator = "\n"))
    }
}
