package com.codeit.team5.mopl.sse.dto;

import java.time.Instant;
import java.util.UUID;

// TODO: DM 도메인 구현 후 conversationId, sender(UserSummary), receiver(UserSummary) 필드 추가
public record DirectMessagePayload(
    UUID id,
    UUID receiverId,
    String content,
    Instant createdAt
) {
}
