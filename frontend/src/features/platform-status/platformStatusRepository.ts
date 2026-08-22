import { semanticQueryCatalog } from '../../semanticQueryCatalog'
import { postSemanticQuery, postSemanticQueryPage } from '../../semanticQueryClient'
import type { PagedResult } from '../../shared/pagination'

export type PlatformStatus = {
  service_boundary: string
  platform_verdict: 'OPERATIONAL' | 'DEGRADED' | 'UNKNOWN'
  reason_code: string
  source_freshness_status: string
  latest_source_import_at: string | null
  source_system_count: number
  latest_canonical_release_id: string | null
  latest_promotion_at: string | null
  promotion_status: string
  latest_reasoning_run_id: string | null
  latest_analysis_at: string | null
  analysis_status: string
  pipeline_status: string
  reconciliation_status: string
  graph_validation_status: string
  source_record_count: number
  incident_count: number
  incident_with_provenance_count: number
  asset_count: number
  asset_with_provenance_count: number
}

export type DataQualityCheck = {
  check_result_id: string
  pipeline_run_id: string | null
  check_name: string
  graph_scope: string
  severity: string
  status: string
  failed_row_count: number | null
  sample_failed_keys: string[]
  message: string
  created_at: string | null
}

type SemanticPlatformStatusRecord = {
  serviceBoundary: string
  platformVerdict: PlatformStatus['platform_verdict']
  reasonCode: string
  sourceFreshnessStatus: string
  latestSourceImportAt?: string
  sourceSystemCount: number
  latestCanonicalReleaseId?: string
  latestPromotionAt?: string
  promotionStatus: string
  latestReasoningRunId?: string
  latestAnalysisAt?: string
  analysisStatus: string
  pipelineStatus: string
  reconciliationStatus: string
  graphValidationStatus: string
  sourceRecordCount: number
  incidentCount: number
  incidentWithProvenanceCount: number
  assetCount: number
  assetWithProvenanceCount: number
}

type SemanticTrustFindingRecord = {
  trustFindingUri: string
  trustFindingId?: string
  summary: string
  sourceFactUri: string
  activityUri?: string
  severity?: string
  status?: string
  createdAt?: string
}

export async function fetchPlatformStatus(): Promise<PlatformStatus> {
  const [record] = await postSemanticQuery<SemanticPlatformStatusRecord>(semanticQueryCatalog.platformStatus)
  if (!record) throw new Error('The semantic platform status read model returned no evidence.')
  return {
    service_boundary: record.serviceBoundary,
    platform_verdict: record.platformVerdict,
    reason_code: record.reasonCode,
    source_freshness_status: record.sourceFreshnessStatus,
    latest_source_import_at: record.latestSourceImportAt ?? null,
    source_system_count: record.sourceSystemCount,
    latest_canonical_release_id: record.latestCanonicalReleaseId ?? null,
    latest_promotion_at: record.latestPromotionAt ?? null,
    promotion_status: record.promotionStatus,
    latest_reasoning_run_id: record.latestReasoningRunId ?? null,
    latest_analysis_at: record.latestAnalysisAt ?? null,
    analysis_status: record.analysisStatus,
    pipeline_status: record.pipelineStatus,
    reconciliation_status: record.reconciliationStatus,
    graph_validation_status: record.graphValidationStatus,
    source_record_count: record.sourceRecordCount,
    incident_count: record.incidentCount,
    incident_with_provenance_count: record.incidentWithProvenanceCount,
    asset_count: record.assetCount,
    asset_with_provenance_count: record.assetWithProvenanceCount,
  }
}

export async function fetchQualityCheckPage(page: number, pageSize: number): Promise<PagedResult<DataQualityCheck>> {
  const result = await postSemanticQueryPage<SemanticTrustFindingRecord>(semanticQueryCatalog.trustFindingList, page, pageSize)
  return { records: result.records.map(mapTrustFinding), page_info: result.pageInfo }
}

export function mapTrustFinding(record: SemanticTrustFindingRecord): DataQualityCheck {
  return {
    check_result_id: record.trustFindingId ?? record.trustFindingUri,
    pipeline_run_id: record.activityUri ?? null,
    check_name: 'Semantic evidence issue',
    graph_scope: 'reasoning graph',
    severity: record.severity ?? 'UNKNOWN',
    status: record.status ?? 'UNKNOWN',
    failed_row_count: null,
    sample_failed_keys: [record.sourceFactUri],
    message: record.summary,
    created_at: record.createdAt ?? null,
  }
}
