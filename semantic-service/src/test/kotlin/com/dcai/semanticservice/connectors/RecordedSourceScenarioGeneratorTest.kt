package com.dcai.semanticservice.connectors

import com.dcai.semanticservice.ingestion.SourceExtractRdfMapper
import com.dcai.semanticservice.lifecycle.GraphLifecycleInspectionPlan
import com.dcai.semanticservice.lifecycle.GraphLifecycleInspector
import com.dcai.semanticservice.ontology.Dcai
import com.dcai.semanticservice.ontology.Prov
import com.dcai.semanticservice.promotion.GraphPromotionService
import com.dcai.semanticservice.promotion.ProductionGraphPromotionPlan
import com.dcai.semanticservice.promotion.ProductionGraphUris
import com.dcai.semanticservice.promotion.ProductionGraphValidationGate
import com.dcai.semanticservice.reasoning.ReasoningInput
import com.dcai.semanticservice.reasoning.ReasoningInputGraphUris
import com.dcai.semanticservice.reasoning.ReasoningModelBuilder
import com.dcai.semanticservice.reasoning.ReasoningOutputGraphUris
import com.dcai.semanticservice.reasoning.ReasoningPromotionPlan
import com.dcai.semanticservice.reasoning.ReasoningPromotionService
import com.dcai.semanticservice.reasoning.ReasoningValidationGate
import com.dcai.semanticservice.runtime.SemanticServiceComposition
import com.dcai.semanticservice.testfixtures.InMemoryNamedGraphStore
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.apache.jena.query.DatasetFactory
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.RDF

class RecordedSourceScenarioGeneratorTest {
    private val repoRoot = SemanticServiceComposition.locateRepoRoot()
    private val generator = RecordedSourceScenarioGenerator()
    private val loader = RecordedSourceConnectorSimulationLoader()

    @Test
    fun generatesDeterministicDemoExportsCompatibleWithConnectorLoader() {
        val firstDirectory = Files.createTempDirectory("generated-demo-first")
        val secondDirectory = Files.createTempDirectory("generated-demo-second")

        val first = generator.generate(request(RecordedSourceScenarioProfile.DEMO, 20260610, firstDirectory))
        val second = generator.generate(request(RecordedSourceScenarioProfile.DEMO, 20260610, secondDirectory))
        val firstLoaded = loader.load(firstDirectory)
        val secondLoaded = loader.load(secondDirectory)

        assertEquals(first.copy(outputDirectory = second.outputDirectory), second)
        assertEquals(renderedFiles(firstDirectory), renderedFiles(secondDirectory))
        assertEquals("generated-demo-seed-20260610", firstLoaded.batch.batchId)
        assertEquals(74, firstLoaded.report.totalRows)
        assertEquals("recorded-source-system-contract-v1", firstLoaded.report.connectorContractId)
        assertEquals("2026-06-mvp", firstLoaded.report.connectorContractVersion)
        assertEquals("demo", firstLoaded.report.scenarioProfile)
        assertEquals(20260610, firstLoaded.report.scenarioSeed)
        assertTrue(first.csvFiles.contains("scenario_inventory.csv"))
        assertTrue(firstDirectory.resolve("scenario_inventory.csv").readText().contains("recovery-blocker"))
        assertEquals(72, firstLoaded.report.acceptedRows)
        assertEquals(2, firstLoaded.report.rejectedRowCount)
        assertEquals(
            firstLoaded.batch.allSourceRecords.map { it.recordId to it.payloadHash },
            secondLoaded.batch.allSourceRecords.map { it.recordId to it.payloadHash },
        )
        assertTrue(firstLoaded.report.rejectedRows.any { it.reason == "missing required field assetId" })
        assertTrue(firstLoaded.report.rejectedRows.any { it.reason.startsWith("duplicate eventId=") })
    }

    @Test
    fun generatesMvpAndStressProfilesWithExpectedScale() {
        val mvpDirectory = Files.createTempDirectory("generated-mvp")
        val stressDirectory = Files.createTempDirectory("generated-stress")

        val mvp = generator.generate(request(RecordedSourceScenarioProfile.MVP, 42, mvpDirectory))
        val stress = generator.generate(request(RecordedSourceScenarioProfile.STRESS, 42, stressDirectory))
        val stressLoaded = loader.load(stressDirectory)

        assertEquals(48, mvp.scenarioCount)
        assertEquals(872, mvp.totalRows)
        assertEquals(600, stress.scenarioCount)
        assertEquals(10_828, stress.totalRows)
        assertTrue(stress.totalRows >= 10_000)
        assertEquals(10_204, stressLoaded.report.acceptedRows)
        assertEquals(24, stressLoaded.report.rejectedRowCount)
    }

    @Test
    fun promotesReasonsAndInspectsGeneratedDemoBatch() {
        val directory = Files.createTempDirectory("generated-demo-lifecycle")
        val generation = generator.generate(request(RecordedSourceScenarioProfile.DEMO, 7, directory))
        val simulation = loader.load(directory)
        val productionGraphs = ProductionGraphUris.forRelease(generation.batchId)
        val store = InMemoryNamedGraphStore()

        val promotion = GraphPromotionService(
            mapper = SourceExtractRdfMapper(),
            validationGate = ProductionGraphValidationGate(repoRoot),
            graphStore = store,
        ).promote(
            ProductionGraphPromotionPlan(
                batch = simulation.batch,
                graphs = productionGraphs,
            ),
        )

        assertTrue(promotion.promoted, promotion.errors.joinToString(separator = "\n"))

        val reasoningRunId = "${generation.batchId}-reasoning"
        val reasoningGraphs = ReasoningOutputGraphUris.forRun(reasoningRunId)
        val reasoning = ReasoningPromotionService(
            builder = ReasoningModelBuilder(),
            validationGate = ReasoningValidationGate(repoRoot),
            graphStore = store,
        ).run(
            ReasoningPromotionPlan(
                runId = reasoningRunId,
                generatedAt = Instant.parse("2026-06-10T01:00:00Z"),
                inputGraphs = ReasoningInputGraphUris.forRelease(generation.batchId),
                outputGraphs = reasoningGraphs,
            ),
        )

        assertTrue(reasoning.promoted, reasoning.errors.joinToString(separator = "\n"))
        assertEquals(27, reasoning.findingCount)

        val lifecycle = GraphLifecycleInspector(store).inspect(
            GraphLifecycleInspectionPlan(
                releaseId = generation.batchId,
                reasoningRunId = reasoningRunId,
            ),
        )

        assertTrue(lifecycle.inspected, lifecycle.errors.joinToString(separator = "\n"))
        assertEquals("promoted", lifecycle.lifecycleStatus)
        assertEquals("refreshed", lifecycle.reasoningStatus)
        assertEquals(4, lifecycle.canonicalGraph?.incidentCount)
        assertEquals(16, lifecycle.canonicalGraph?.assetCount)
        assertEquals(12, lifecycle.canonicalGraph?.dependencyEdgeCount)
        assertEquals(80, lifecycle.provenanceGraph?.sourceRecordCount)
        assertEquals(27, lifecycle.reasoningGraph?.findingCount)
        assertEquals(4, lifecycle.reasoningGraph?.recoveryBlockerCount)
        assertEquals(4, lifecycle.reasoningGraph?.restoreReadinessFindingCount)
        assertEquals(7, lifecycle.reasoningGraph?.trustFindingCount)
    }

    @Test
    fun publicPortfolioScenariosExistAndReachTheirDocumentedReasoningFindings() {
        val directory = Files.createTempDirectory("generated-public-portfolio")
        val generation = generator.generate(request(RecordedSourceScenarioProfile.MVP, 20260610, directory))
        val inventory = directory.resolve("scenario_inventory.csv").readText()

        assertTrue(inventory.contains("SCN-20260611,INC-GEN-SCN-20260611,recovery-blocker,"))
        assertTrue(inventory.contains("SCN-20260613,INC-GEN-SCN-20260613,cooling-instability,"))
        assertTrue(inventory.contains("SCN-20260616,INC-GEN-SCN-20260616,conflicting-validation,"))

        val simulation = loader.load(directory)
        val graphs = ProductionGraphUris.forRelease(generation.batchId)
        val store = InMemoryNamedGraphStore()
        val promotion = GraphPromotionService(
            mapper = SourceExtractRdfMapper(),
            validationGate = ProductionGraphValidationGate(repoRoot),
            graphStore = store,
        ).promote(ProductionGraphPromotionPlan(batch = simulation.batch, graphs = graphs))

        assertTrue(promotion.promoted, promotion.errors.joinToString(separator = "\n"))

        val reasoning = ReasoningModelBuilder().build(
            ReasoningInput(
                runId = "public-portfolio-scenarios-v1",
                generatedAt = Instant.parse("2026-06-10T03:00:00Z"),
                canonicalModel = store.readNamedGraph(graphs.canonicalGraphUri).model,
                provenanceModel = store.readNamedGraph(graphs.provenanceGraphUri).model,
            ),
        )

        assertFindingDerivedFrom(
            reasoning.auditModel,
            Dcai.RecoveryBlocker,
            ResourceFactory.createResource("urn:dcai:incident:INC-GEN-SCN-20260611"),
        )
        assertFindingDerivedFrom(
            reasoning.auditModel,
            Dcai.DependencyImpactFinding,
            ResourceFactory.createResource("urn:dcai:incident:INC-GEN-SCN-20260613"),
        )
        assertFindingDerivedFrom(
            reasoning.auditModel,
            Dcai.TrustFinding,
            ResourceFactory.createResource("urn:dcai:evidence:VAL-GEN-SCN-20260616-SECONDARY"),
        )
        assertTrue(ReasoningValidationGate(repoRoot).validate(reasoning.auditModel).conforms)

        val dataset = DatasetFactory.createTxnMem()
        dataset.addNamedModel(graphs.canonicalGraphUri, store.readNamedGraph(graphs.canonicalGraphUri).model)
        dataset.addNamedModel(
            ReasoningOutputGraphUris.forRun("public-portfolio-scenarios-v1").reasoningGraphUri,
            reasoning.reasoningModel,
        )
        val actionQuery = QueryFactory.read(
            repoRoot.resolve("queries/read-model/semantic_available_actions_by_finding.select.rq").toString(),
        )
        val statuses = mutableMapOf<Pair<String, String>, MutableSet<String>>()
        QueryExecutionFactory.create(actionQuery, dataset).use { execution ->
            val results = execution.execSelect()
            while (results.hasNext()) {
                val row = results.next()
                val key = row.getLiteral("incidentId").string to row.getLiteral("actionId").string
                statuses.getOrPut(key) { linkedSetOf() } += row.getLiteral("actionStatus").string
            }
        }

        assertEquals(
            setOf("AVAILABLE_FOR_LOCAL_AUDIT"),
            statuses.getValue("INC-GEN-SCN-20260611" to "AcknowledgeRestoreBlocker"),
        )
        assertEquals(
            setOf("AVAILABLE_FOR_LOCAL_AUDIT"),
            statuses.getValue("INC-GEN-SCN-20260613" to "AcknowledgeRestoreBlocker"),
        )
        assertEquals(
            setOf("AVAILABLE_FOR_LOCAL_AUDIT"),
            statuses.getValue("INC-GEN-SCN-20260616" to "AssignEvidenceReview"),
        )
        assertEquals(
            setOf("AVAILABLE_FOR_LOCAL_AUDIT"),
            statuses.getValue("INC-GEN-SCN-20260616" to "RecordValidationReview"),
        )
        assertEquals(
            setOf("DISABLED"),
            statuses.getValue("INC-GEN-SCN-20260616" to "RequestReasoningRefresh"),
        )

        val dependencyQuery = QueryFactory.read(
            repoRoot.resolve("queries/read-model/semantic_dependency_impact_by_asset.select.rq").toString(),
        )
        var foundCoolingResponse = false
        QueryExecutionFactory.create(dependencyQuery, dataset).use { execution ->
            val results = execution.execSelect()
            while (results.hasNext()) {
                val row = results.next()
                if (
                    row.getLiteral("assetId").string == "ASSET-GEN-GPU-SCN-20260613" &&
                    row.getLiteral("dependencyId")?.string == "DEP-GEN-SCN-20260613-COOLING"
                ) {
                    assertEquals("ASSET-GEN-CHW-SCN-20260613", row.getLiteral("dependencyAssetId").string)
                    assertEquals("cooling-loop", row.getLiteral("dependencyRole").string)
                    assertEquals("row", row.getLiteral("impactScope").string)
                    foundCoolingResponse = true
                }
            }
        }
        assertTrue(foundCoolingResponse, "Expected the public cooling dependency query response")
    }

    private fun request(
        profile: RecordedSourceScenarioProfile,
        seed: Int,
        outputDirectory: Path,
    ): RecordedSourceScenarioGenerationRequest {
        return RecordedSourceScenarioGenerationRequest(
            profile = profile,
            seed = seed,
            outputDirectory = outputDirectory,
        )
    }

    private fun renderedFiles(directory: Path): Map<String, String> {
        return Files.list(directory).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .sorted { left, right -> left.fileName.toString().compareTo(right.fileName.toString()) }
                .toList()
                .associate { it.fileName.toString() to it.readText() }
        }
    }

    private fun assertFindingDerivedFrom(
        model: Model,
        findingType: Resource,
        source: Resource,
    ) {
        assertTrue(
            model.listSubjectsWithProperty(RDF.type, findingType).toList().any { finding ->
                model.contains(finding, Prov.wasDerivedFrom, source)
            },
            "Expected $findingType finding derived from $source",
        )
    }
}
