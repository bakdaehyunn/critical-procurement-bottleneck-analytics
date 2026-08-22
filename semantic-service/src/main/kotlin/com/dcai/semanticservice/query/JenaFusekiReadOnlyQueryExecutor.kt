package com.dcai.semanticservice.query

import com.dcai.semanticservice.graph.FusekiReadOnlyConfig
import org.apache.jena.query.ParameterizedSparqlString
import org.apache.jena.query.Query
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.sparql.syntax.ElementGroup
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP
import org.apache.jena.sparql.util.FmtUtils

class JenaFusekiReadOnlyQueryExecutor(
    private val registry: QueryContractRegistry,
    private val config: FusekiReadOnlyConfig = FusekiReadOnlyConfig.fromEnvironment(),
) : ReadOnlyQueryExecutor {
    constructor(
        manifest: ApprovedQueryManifest,
        config: FusekiReadOnlyConfig = FusekiReadOnlyConfig.fromEnvironment(),
    ) : this(QueryContractRegistry.fromManifest(manifest), config)

    override fun execute(queryId: String): QueryExecutionReport {
        return execute(queryId, emptyMap(), null)
    }

    override fun execute(
        queryId: String,
        parameters: Map<String, String>,
    ): QueryExecutionReport {
        return execute(queryId, parameters, null)
    }

    override fun execute(
        queryId: String,
        parameters: Map<String, String>,
        pageRequest: QueryPageRequest?,
    ): QueryExecutionReport {
        val contract = registry.require(queryId)
        val definition = contract.definition
        return when (definition.mode) {
            QueryMode.SELECT -> executeSelect(contract, parameters, pageRequest)
            QueryMode.ASK -> {
                require(pageRequest == null) { "ASK query ${definition.id} does not support paging." }
                executeAsk(definition, parameters)
            }
            QueryMode.CONSTRUCT,
            QueryMode.UPDATE,
            -> error("Query ${definition.id} is not approved for runtime read-only execution")
        }
    }

    private fun executeSelect(
        contract: QueryContract,
        parameters: Map<String, String>,
        pageRequest: QueryPageRequest?,
    ): QueryExecutionReport {
        val definition = contract.definition
        val query = definition.parameterizedQuery(parameters)
        if (pageRequest == null) {
            val rows = executeRows(query.toString())
            return QueryExecutionReport(
                queryId = definition.id,
                mode = definition.mode,
                rowCount = rows.size,
                rows = rows,
            )
        }

        require(!query.hasLimit() && !query.hasOffset()) {
            "Paged query ${definition.id} must not declare an outer LIMIT or OFFSET."
        }
        val policy = contract.pagingPolicy
            ?: throw IllegalArgumentException("Query ${definition.id} does not declare a stable paging policy.")
        val projectedBindings = query.resultVars.toSet()
        val requiredBindings = policy.identityBindings + policy.sortBindings.map(QueryPagingSort::binding)
        require(projectedBindings.containsAll(requiredBindings)) {
            "Paged query ${definition.id} must project identity and sort bindings: ${requiredBindings.joinToString()}."
        }

        val totalRecords = executeTotalCount(query, policy)
        val pageIdentities = executePageIdentities(query, policy, pageRequest)
        val rows = if (pageIdentities.isEmpty()) {
            emptyList()
        } else {
            executeRows(query.withIdentityValues(policy, pageIdentities).toString())
        }
        return QueryExecutionReport(
            queryId = definition.id,
            mode = definition.mode,
            rowCount = rows.size,
            rows = rows,
            page = QueryPageResult(
                page = pageRequest.page,
                pageSize = pageRequest.pageSize,
                totalRecords = totalRecords,
            ),
        )
    }

    private fun executeRows(sparql: String): List<Map<String, String>> {
        QueryExecutionHTTP
            .service(config.queryEndpointUrl)
            .query(sparql)
            .build()
            .use { execution ->
                val results = execution.execSelect()
                val variables = results.resultVars
                val rows = mutableListOf<Map<String, String>>()
                while (results.hasNext()) {
                    val solution = results.next()
                    rows += variables.associateWith { variable ->
                        solution.stringValue(variable)
                    }
                }
                return rows
            }
    }

    private fun executeTotalCount(
        query: Query,
        policy: QueryPagingPolicy,
    ): Int {
        val identityProjection = policy.identityBindings.joinToString(" ") { "?$it" }
        val countQuery = """
            ${query.prefixDeclarations()}
            SELECT (COUNT(*) AS ?totalRecords)
            WHERE {
              {
                SELECT DISTINCT $identityProjection
                WHERE { { ${query.withoutPrefixes()} } }
              }
            }
        """.trimIndent()
        QueryExecutionHTTP
            .service(config.queryEndpointUrl)
            .query(countQuery)
            .build()
            .use { execution ->
                val results = execution.execSelect()
                require(results.hasNext()) { "Paged query count did not return a totalRecords binding." }
                return results.next().getLiteral("totalRecords").int
            }
    }

    private fun executePageIdentities(
        query: Query,
        policy: QueryPagingPolicy,
        pageRequest: QueryPageRequest,
    ): List<List<RDFNode>> {
        val identityProjection = policy.identityBindings.joinToString(" ") { "?$it" }
        val sortProjection = policy.sortBindings.mapIndexed { index, sort ->
            val aggregate = if (sort.direction == QuerySortDirection.DESCENDING) "MAX" else "MIN"
            "($aggregate(?${sort.binding}) AS ?__pageSort$index)"
        }.joinToString(" ")
        val sortOrder = policy.sortBindings.mapIndexed { index, sort ->
            if (sort.direction == QuerySortDirection.DESCENDING) "DESC(?__pageSort$index)" else "?__pageSort$index"
        }
        val identityOrder = policy.identityBindings.map { "?$it" }
        val pageQuery = """
            ${query.prefixDeclarations()}
            SELECT $identityProjection $sortProjection
            WHERE { { ${query.withoutPrefixes()} } }
            GROUP BY $identityProjection
            ORDER BY ${(sortOrder + identityOrder).joinToString(" ")}
            LIMIT ${pageRequest.pageSize}
            OFFSET ${pageRequest.offset}
        """.trimIndent()

        QueryExecutionHTTP
            .service(config.queryEndpointUrl)
            .query(pageQuery)
            .build()
            .use { execution ->
                val results = execution.execSelect()
                val identities = mutableListOf<List<RDFNode>>()
                while (results.hasNext()) {
                    val solution = results.next()
                    identities += policy.identityBindings.map { binding ->
                        requireNotNull(solution.get(binding)) {
                            "Paged query identity binding $binding was not returned."
                        }
                    }
                }
                return identities
            }
    }

    private fun Query.withIdentityValues(
        policy: QueryPagingPolicy,
        identities: List<List<RDFNode>>,
    ): Query {
        val variables = policy.identityBindings.joinToString(" ") { "?$it" }
        val rows = identities.joinToString("\n") { identity ->
            identity.joinToString(prefix = "(", postfix = ")", separator = " ") { value ->
                FmtUtils.stringForNode(value.asNode())
            }
        }
        val valuesQuery = QueryFactory.create("SELECT * WHERE { VALUES ($variables) { $rows } }")
        val paged = cloneQuery()
        val pattern = ElementGroup().apply {
            addElement(paged.queryPattern)
            addElement(valuesQuery.queryPattern)
        }
        paged.queryPattern = pattern
        return paged
    }

    private fun Query.prefixDeclarations(): String {
        return prefixMapping.nsPrefixMap.toSortedMap().entries.joinToString("\n") { (prefix, namespace) ->
            "PREFIX $prefix: <$namespace>"
        }
    }

    private fun Query.withoutPrefixes(): String {
        return cloneQuery().apply { prefixMapping.clearNsPrefixMap() }.toString()
    }

    private fun executeAsk(
        definition: ApprovedQueryDefinition,
        parameters: Map<String, String>,
    ): QueryExecutionReport {
        QueryExecutionHTTP
            .service(config.queryEndpointUrl)
            .query(definition.parameterizedQuery(parameters).toString())
            .build()
            .use { execution ->
                val result = execution.execAsk()
                return QueryExecutionReport(
                    queryId = definition.id,
                    mode = definition.mode,
                    rowCount = 1,
                    askResult = result,
                )
            }
    }

    private fun ApprovedQueryDefinition.parameterizedQuery(parameters: Map<String, String>): Query {
        if (parameters.isEmpty()) return QueryFactory.create(sparql)
        val parameterized = ParameterizedSparqlString(sparql)
        parameters.forEach { (key, value) ->
            require(PARAMETER_NAME.matches(key)) { "Unsupported query parameter name: $key" }
            parameterized.setLiteral(key, value)
        }
        return QueryFactory.create(parameterized.toString())
    }

    private fun QuerySolution.stringValue(variable: String): String {
        return get(variable)?.displayString().orEmpty()
    }

    private fun RDFNode.displayString(): String {
        return when {
            isLiteral -> asLiteral().lexicalForm
            isURIResource -> asResource().uri
            else -> toString()
        }
    }

    private companion object {
        private val PARAMETER_NAME = Regex("[A-Za-z][A-Za-z0-9_]*")
    }
}
