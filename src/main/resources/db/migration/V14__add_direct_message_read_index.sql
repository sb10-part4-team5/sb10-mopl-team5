-- 안 읽은 메시지 카운트/일괄 읽음 처리 조회 최적화
CREATE INDEX idx_dm_conversation_receiver_read
    ON direct_messages (conversation_id, receiver_id, is_read, created_at);
