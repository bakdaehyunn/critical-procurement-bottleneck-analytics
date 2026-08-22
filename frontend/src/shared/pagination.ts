import type { SemanticPageInfo } from '../semanticQueryClient'

export type PagedResult<T> = {
  records: T[]
  page_info: SemanticPageInfo
}
