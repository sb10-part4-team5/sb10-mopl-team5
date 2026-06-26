-- 커서 페이지네이션(receiver_id + created_at + id tie-break)을 인덱스만으로 완결
CREATE INDEX idx_noti_receiver_created_id
    ON notifications (receiver_id, created_at DESC, id DESC);

-- 기존 (receiver_id, created_at) 인덱스는 위 3컬럼 인덱스의 prefix라 중복 → 제거
DROP INDEX IF EXISTS idx_noti_receiver;

-- idx_noti_unread (WHERE is_read = FALSE 부분 인덱스)는 용도가 달라 그대로 유지
