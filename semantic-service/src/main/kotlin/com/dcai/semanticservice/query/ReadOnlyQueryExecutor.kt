package com.dcai.semanticservice.query

interface ReadOnlyQueryExecutor {
    fun execute(queryId: String): QueryExecutionReport

    fun execute(
        queryId: String,
        parameters: Map<String, String>,
    ): QueryExecutionReport = execute(queryId)

    fun execute(
        queryId: String,
        parameters: Map<String, String>,
        pageRequest: QueryPageRequest?,
    ): QueryExecutionReport {
        require(pageRequest == null) { "Query executor does not support bounded paging." }
        return execute(queryId, parameters)
    }
}

data class QueryPageRequest(
    val page: Int,
    val pageSize: Int,
) {
    init {
        require(page >= 1) { "Semantic query page must be at least 1." }
        require(pageSize in 1..100) { "Semantic query pageSize must be between 1 and 100." }
    }

    val offset: Long = (page.toLong() - 1L) * pageSize.toLong()
}

data class QueryPageResult(
    val page: Int,
    val pageSize: Int,
    val totalRecords: Int,
) {
    val pageCount: Int = maxOf(1, (totalRecords + pageSize - 1) / pageSize)
}

data class QueryExecutionReport(
    val queryId: String,
    val mode: QueryMode,
    val rowCount: Int = 0,
    val askResult: Boolean? = null,
    val rows: List<Map<String, String>> = emptyList(),
    val page: QueryPageResult? = null,
) {
    val succeeded: Boolean = true
}
