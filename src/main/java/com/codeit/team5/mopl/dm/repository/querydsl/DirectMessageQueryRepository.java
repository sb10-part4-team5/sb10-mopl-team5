package com.codeit.team5.mopl.dm.repository.querydsl;

import com.codeit.team5.mopl.dm.dto.request.DirectMessageCursorRequest;
import com.codeit.team5.mopl.dm.entity.DirectMessage;
import java.util.List;
import java.util.UUID;

public interface DirectMessageQueryRepository {

    List<DirectMessage> findMessages(UUID conversationId, DirectMessageCursorRequest request, int fetchLimit);

    long countMessages(UUID conversationId);
}
