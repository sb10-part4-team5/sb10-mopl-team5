-- 리뷰 목록 조회 커서 페이지네이션 성능 개선
-- DESC 정렬: created_at DESC, id DESC
-- ASC 정렬:  created_at ASC,  id ASC
-- id 타이브레이커를 정렬 방향과 맞춰 단방향 인덱스 1개로 커버
CREATE INDEX idx_review_content_created_at ON reviews (content_id, created_at, id);

-- 인덱스 중복을 해결하기 위해 기존의 content_id 인덱스를 제거
DROP INDEX idx_review_content;
