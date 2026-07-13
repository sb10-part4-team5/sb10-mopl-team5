// 콘텐츠 조회 "케이스" 정의 (파라미터 + 리포트용 태그) — compare/baseline 이 공유.
// 케이스를 추가/변경할 땐 여기 한 곳만 고치면 두 시나리오 모두에 반영된다.

import { ContentSortBy, SortDirection } from '../config.ts';
import { ContentListParams } from '../api/content.api.ts';

export type ContentQueryCase = 'latest' | 'popular' | 'rating' | 'keyword' | 'tag';

export const CONTENT_QUERY_CASE_NAMES: ContentQueryCase[] = [
  'latest', 'popular', 'rating', 'keyword', 'tag',
];

export const CONTENT_QUERY_TAGS: Record<ContentQueryCase, string> = {
  latest: 'GET /api/contents (latest)',   // 최신순 (createdAt)
  popular: 'GET /api/contents (popular)', // 인기순 (watcherCount)
  rating: 'GET /api/contents (rating)',   // 평점순 (rate, 계산 정렬)
  keyword: 'GET /api/contents (keyword)', // 키워드 검색 (LIKE)
  tag: 'GET /api/contents (tag)',         // 태그 필터 (서브쿼리 조인)
};

const BASE: ContentListParams = {
  limit: 20,
  sortBy: ContentSortBy.CREATED_AT,
  sortDirection: SortDirection.DESC,
};

// keyword/tag 케이스는 실제 매칭되는 값을 넣어야 결과셋 크기까지 정직하게 측정된다.
export function buildCaseParams(
  caseName: ContentQueryCase,
  variables: { keyword: string; tagName: string },
): ContentListParams {
  switch (caseName) {
    case 'latest':
      return { ...BASE, sortBy: ContentSortBy.CREATED_AT };
    case 'popular':
      return { ...BASE, sortBy: ContentSortBy.WATCHER_COUNT };
    case 'rating':
      return { ...BASE, sortBy: ContentSortBy.RATE };
    case 'keyword':
      return { ...BASE, keywordLike: variables.keyword };
    case 'tag':
      return { ...BASE, tagsIn: [variables.tagName] };
  }
}
