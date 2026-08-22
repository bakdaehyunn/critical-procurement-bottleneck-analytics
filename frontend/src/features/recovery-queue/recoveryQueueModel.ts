export type Overview = {
  total_requests: number
  open_requests: number
  delayed_requests: number | null
  critical_asset_delayed: number | null
  avg_downtime_hours: number | null
  top_bottleneck_stage: string | null
  spare_waiting_delay_hours: number | null
  repeat_failure_asset_count: number | null
  engineer_assignment_delay_hours: number | null
  capacity_risk_kw: number | null
  affected_gpu_count: number | null
  redundancy_lost_incidents: number
  vendor_eta_missed_count: number
  latest_pipeline_run_status: string | null
  data_quality_status: string
}

export type FollowUpItem = {
  priority_rank: number
  incident_id: string
  request_number: string
  request_title: string
  asset_id: string
  asset_name: string
  zone_id: string
  zone_name: string
  current_stage: string
  current_status: string
  hours_in_current_stage: number | null
  needed_by_at: string
  priority_level: string
  business_impact: string
  asset_criticality_score: number | null
  downtime_score: number | null
  stage_delay_score: number | null
  infrastructure_zone_impact_score: number | null
  needed_by_urgency_score: number | null
  repeat_failure_score: number | null
  spare_risk_score: number | null
  capacity_risk_score: number | null
  redundancy_risk_score: number | null
  thermal_risk_score: number | null
  vendor_eta_risk_score: number | null
  mitigation_credit_score: number | null
  total_priority_score: number | null
  recommended_action: string
  reason_summary: string
  redundancy_state: string | null
  affected_gpu_count: number | null
  estimated_capacity_risk_kw: number | null
  mitigation_status: string | null
  vendor_status: string | null
  impact_confidence_status: string
  impact_trust_issue_count: number
  restore_readiness_status: string
  restore_readiness_summary: string | null
  dependency_roles: string[]
  dependency_path_ids: string[]
}

export type RecoveryQueueSnapshot = {
  overview: Overview
  followUps: FollowUpItem[]
}

export type FilterOption = {
  id: string
  name: string
}

export type FilterMetadata = {
  infrastructure_zones: FilterOption[]
  assets: FilterOption[]
  asset_types: string[]
  facilities_teams: string[]
  spare_categories: string[]
  priority_levels: string[]
  request_types: string[]
  failure_modes: string[]
  stages: string[]
}
