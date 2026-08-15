import { semanticQueryPath, type SemanticQueryId, type SemanticQueryParameters } from './semanticQueryCatalog'
import { SEMANTIC_API_BASE_URL } from './semanticRuntimeConfig'

export type SemanticEnvelope<T> = {
  queryId: string
  resultType: string
  recordCount: number
  records: T[]
  pageInfo?: SemanticPageInfo
  provenance: {
    queryId: string
    graphScope: string
    contractVersion: string
  }
}

export type SemanticPageInfo = {
  page: number
  pageSize: number
  pageCount: number
  totalRecords: number
}

export type SemanticPage<T> = {
  records: T[]
  pageInfo: SemanticPageInfo
  provenance: SemanticEnvelope<T>['provenance']
}

export async function postSemanticQuery<T>(
  queryId: SemanticQueryId,
  parameters: SemanticQueryParameters = {},
): Promise<T[]> {
  const response = await fetch(`${SEMANTIC_API_BASE_URL}${semanticQueryPath(queryId)}`, {
    method: 'POST',
    headers: Object.keys(parameters).length ? { 'Content-Type': 'application/json' } : undefined,
    body: Object.keys(parameters).length ? JSON.stringify({ parameters }) : undefined,
  })
  if (!response.ok) {
    const payload = await response.text()
    throw new Error(`Semantic query failed: ${queryId} ${response.status} ${response.statusText} ${payload}`)
  }
  const payload = await response.json() as SemanticEnvelope<T>
  return payload.records
}

export async function postSemanticQueryPage<T>(
  queryId: SemanticQueryId,
  page: number,
  pageSize: number,
  parameters: SemanticQueryParameters = {},
): Promise<SemanticPage<T>> {
  const pagedParameters = {
    ...parameters,
    page: String(page),
    pageSize: String(pageSize),
  }
  const response = await fetch(`${SEMANTIC_API_BASE_URL}${semanticQueryPath(queryId)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ parameters: pagedParameters }),
  })
  if (!response.ok) {
    const payload = await response.text()
    throw new Error(`Semantic query failed: ${queryId} ${response.status} ${response.statusText} ${payload}`)
  }
  const payload = await response.json() as SemanticEnvelope<T>
  return {
    records: payload.records,
    pageInfo: payload.pageInfo ?? {
      page,
      pageSize,
      pageCount: Math.max(1, Math.ceil(payload.recordCount / pageSize)),
      totalRecords: payload.recordCount,
    },
    provenance: payload.provenance,
  }
}
