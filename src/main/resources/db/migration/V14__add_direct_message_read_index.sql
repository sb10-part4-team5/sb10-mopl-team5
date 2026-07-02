-- receiver 단독 안읽음 인덱스는 DM에서 미사용이라 제거
DROP INDEX IF EXISTS idx_dm_unread;

-- 안 읽은 메시지 존재/일괄 읽음 처리 조회 최적화 (안읽음만 부분 인덱싱)
CREATE INDEX idx_dm_conversation_receiver_unread
    ON direct_messages (conversation_id, receiver_id, created_at)
    WHERE is_read = false;
