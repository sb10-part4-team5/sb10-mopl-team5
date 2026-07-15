-- 리뷰 목록 조회 커서 페이지네이션 성능 개선
-- DESC 정렬: created_at DESC, id DESC / rating DESC, id DESC
-- ASC 정렬:  created_at ASC,  id ASC  / rating ASC,  id ASC
-- id 타이브레이커를 정렬 방향과 맞춰 단방향 인덱스 2개로 커버
CREATE INDEX idx_review_content_created_at ON reviews (content_id, created_at, id);
