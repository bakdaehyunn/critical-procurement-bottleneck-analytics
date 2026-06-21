import {
  SEMANTIC_ACTION_AUDIT_RELEASE_ID,
  SEMANTIC_AI_AUDIT_RELEASE_ID,
  SEMANTIC_API_BASE_URL,
  SEMANTIC_REASONING_RUN_ID,
  SEMANTIC_SOURCE_RELEASE_ID,
} from './semanticRuntimeConfig'

export type AiProposalReviewDecision = 'APPROVE' | 'REJECT'

export type AiProposalReviewSubmission = {
  proposal_uri: string
  proposal_id: string
  decision: AiProposalReviewDecision
  actor_id: string
  review_reason: string
  action_id?: string
}

export type AiProposalReviewResult = {
  resultType: 'ai-proposal-review'
  reviewed: boolean
  decision: AiProposalReviewDecision
  reviewStatus: string
  reviewId: string
  idempotencyKey: string
  proposalUri: string
  aiAuditGraphUri: string
  actionAuditGraphUri?: string
  writtenGraphUris: string[]
  idempotentReplay: boolean
  actionRequestCreated: boolean
  actionRequestId?: string
  actionId?: string
  canonicalGraphMutation: boolean
  reasoningGraphMutation: boolean
  provenanceGraphMutation: boolean
  sourceGraphMutation: boolean
  operationsGraphMutation: boolean
  externalSystemMutation: boolean
}

export async function submitAiProposalReview(
  submission: AiProposalReviewSubmission,
): Promise<AiProposalReviewResult> {
  const reviewedAt = new Date().toISOString()
  const timestamp = reviewedAt.replace(/[-:.TZ]/g, '')
  const reviewId = `AI-REV-${submission.decision}-${submission.proposal_id}-${timestamp}`
  const idempotencyKey = `${SEMANTIC_AI_AUDIT_RELEASE_ID}:review:${submission.decision}:${submission.proposal_id}:${timestamp}`
  const response = await fetch(`${SEMANTIC_API_BASE_URL}/semantic/internal/ai-proposal-review`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      reviewId,
      idempotencyKey,
      actorId: submission.actor_id,
      reviewedAt,
      proposalUri: submission.proposal_uri,
      decision: submission.decision,
      reviewReason: submission.review_reason,
      actionId: submission.action_id,
      sourceReleaseId: SEMANTIC_SOURCE_RELEASE_ID,
      reasoningRunId: SEMANTIC_REASONING_RUN_ID,
      aiAuditReleaseId: SEMANTIC_AI_AUDIT_RELEASE_ID,
      actionAuditReleaseId: SEMANTIC_ACTION_AUDIT_RELEASE_ID,
    }),
  })
  if (!response.ok) {
    const payload = await response.text()
    throw new Error(`AI proposal review failed: ${response.status} ${response.statusText} ${payload}`)
  }
  return await response.json() as AiProposalReviewResult
}
