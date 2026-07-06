package com.codeit.team5.mopl.dm.repository.querydsl;

import com.codeit.team5.mopl.dm.entity.DirectMessage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DirectMessageQueryRepository {

    // 대화별 최근 메시지 일괄 조회 (목록 N+1 방지)
    List<DirectMessage> findLatestMessagesByConversationIds(Collection<UUID> conversationIds);

    // 안 읽은 메시지가 있는 대화 ID 일괄 조회 (목록 N+1 방지)
    List<UUID> findConversationIdsWithUnread(Collection<UUID> conversationIds, UUID receiverId);
}
