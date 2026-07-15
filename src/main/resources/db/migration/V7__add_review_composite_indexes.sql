-- 리뷰 목록 조회 커서 페이지네이션 성능 개선
-- QueryDSL ORDER BY: created_at DESC, id ASC / rating DESC, id ASC
-- QueryDSL ORDER BY: created_at ASC, id ASC / rating ASC, id ASC
-- id를 포함해야 filesort 없이 인덱스만으로 정렬 가능
CREATE INDEX idx_review_content_created_at_desc ON reviews (content_id, created_at DESC, id ASC);
CREATE INDEX idx_review_content_rating_desc ON reviews (content_id, rating DESC, id ASC);
CREATE INDEX idx_review_content_created_at_asc ON reviews (content_id, created_at ASC, id ASC);
CREATE INDEX idx_review_content_rating_asc ON reviews (content_id, rating ASC, id ASC);
