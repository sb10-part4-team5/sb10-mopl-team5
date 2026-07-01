package com.codeit.team5.mopl.dm.repository.querydsl;

import com.codeit.team5.mopl.dm.dto.request.ConversationCursorRequest;
import com.codeit.team5.mopl.dm.entity.Conversation;
import java.util.List;
import java.util.UUID;

public interface ConversationQueryRepository {

    List<Conversation> findMyConversations(UUID currentUserId, ConversationCursorRequest request);
}
