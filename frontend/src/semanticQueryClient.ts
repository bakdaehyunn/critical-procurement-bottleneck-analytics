import { semanticQueryPath, semanticQueryRequiredFields, type SemanticQueryId, type SemanticQueryParameters } from './semanticQueryCatalog'
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
  options: { signal?: AbortSignal } = {},
): Promise<T[]> {
  const response = await fetch(`${SEMANTIC_API_BASE_URL}${semanticQueryPath(queryId)}`, {
    method: 'POST',
    headers: Object.keys(parameters).length ? { 'Content-Type': 'application/json' } : undefined,
    body: Object.keys(parameters).length ? JSON.stringify({ parameters }) : undefined,
    signal: options.signal,
  })
  if (!response.ok) {
    const payload = await response.text()
    throw new Error(`Semantic query failed: ${queryId} ${response.status} ${response.statusText} ${payload}`)
  }
  const payload = validateSemanticEnvelope<T>(await response.json(), queryId)
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
  const payload = validateSemanticEnvelope<T>(await response.json(), queryId, true)
  return {
    records: payload.records,
    pageInfo: payload.pageInfo!,
    provenance: payload.provenance,
  }
}

function validateSemanticEnvelope<T>(payload: unknown, expectedQueryId: SemanticQueryId, requirePage = false): SemanticEnvelope<T> {
  if (!isObject(payload)) throw invalidEnvelope(expectedQueryId, 'response must be an object')
  if (payload.queryId !== expectedQueryId) throw invalidEnvelope(expectedQueryId, 'queryId does not match the request')
  if (typeof payload.resultType !== 'string' || !payload.resultType) throw invalidEnvelope(expectedQueryId, 'resultType is required')
  if (!isNonNegativeInteger(payload.recordCount)) throw invalidEnvelope(expectedQueryId, 'recordCount must be a non-negative integer')
  if (!Array.isArray(payload.records) || payload.records.some((record) => !isObject(record))) {
    throw invalidEnvelope(expectedQueryId, 'records must be an array of objects')
  }
  const missingField = payload.records.flatMap((record) => semanticQueryRequiredFields[expectedQueryId]
    .filter((field) => !(field in record))
    .map((field) => `${field}`))[0]
  if (missingField) throw invalidEnvelope(expectedQueryId, `record is missing required field ${missingField}`)
  if (payload.recordCount !== payload.records.length) {
    throw invalidEnvelope(expectedQueryId, 'recordCount must equal the number of serialized records')
  }
  if (!isObject(payload.provenance) || payload.provenance.queryId !== expectedQueryId ||
    typeof payload.provenance.graphScope !== 'string' || typeof payload.provenance.contractVersion !== 'string') {
    throw invalidEnvelope(expectedQueryId, 'provenance is missing or inconsistent')
  }
  if (requirePage || payload.pageInfo !== undefined) validatePageInfo(payload.pageInfo, expectedQueryId)
  return payload as SemanticEnvelope<T>
}

function validatePageInfo(value: unknown, queryId: SemanticQueryId): asserts value is SemanticPageInfo {
  if (!isObject(value) || !isPositiveInteger(value.page) || !isPositiveInteger(value.pageSize) ||
    !isPositiveInteger(value.pageCount) || !isNonNegativeInteger(value.totalRecords)) {
    throw invalidEnvelope(queryId, 'pageInfo must contain valid page, pageSize, pageCount, and totalRecords values')
  }
  const expectedPageCount = Math.max(1, Math.ceil(value.totalRecords / value.pageSize))
  if (value.pageCount !== expectedPageCount) throw invalidEnvelope(queryId, 'pageInfo.pageCount is inconsistent with totalRecords')
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isNonNegativeInteger(value: unknown): value is number {
  return typeof value === 'number' && Number.isInteger(value) && value >= 0
}

function isPositiveInteger(value: unknown): value is number {
  return isNonNegativeInteger(value) && value > 0
}

function invalidEnvelope(queryId: SemanticQueryId, detail: string): Error {
  return new Error(`Invalid semantic response for ${queryId}: ${detail}.`)
}
