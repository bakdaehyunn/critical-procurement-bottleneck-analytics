import { semanticQueryPath, type SemanticQueryId, type SemanticQueryParameters } from './semanticQueryCatalog'
import { SEMANTIC_API_BASE_URL } from './semanticRuntimeConfig'

export type SemanticEnvelope<T> = {
  queryId: string
  resultType: string
  recordCount: number
  records: T[]
  provenance: {
    queryId: string
    graphScope: string
    contractVersion: string
  }
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
