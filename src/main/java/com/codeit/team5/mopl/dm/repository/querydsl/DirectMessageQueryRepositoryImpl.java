package com.codeit.team5.mopl.dm.repository.querydsl;

import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.dm.entity.QDirectMessage;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DirectMessageQueryRepositoryImpl implements DirectMessageQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QDirectMessage directMessage = QDirectMessage.directMessage;

    @Override
    public List<DirectMessage> findLatestMessagesByConversationIds(Collection<UUID> conversationIds) {
        if (conversationIds.isEmpty()) {
            return List.of();
        }
        QDirectMessage newer = new QDirectMessage("newer");
        return queryFactory
                .selectFrom(directMessage)
                .join(directMessage.sender).fetchJoin()
                .join(directMessage.receiver).fetchJoin()
                .where(directMessage.conversation.id.in(conversationIds)
                        .and(JPAExpressions.selectOne()
                                .from(newer)
                                .where(newer.conversation.id.eq(directMessage.conversation.id)
                                        .and(newer.createdAt.gt(directMessage.createdAt)
                                                .or(newer.createdAt.eq(directMessage.createdAt)
                                                        .and(newer.id.gt(directMessage.id)))))
                                .notExists()))
                .fetch();
    }

    @Override
    public List<UUID> findConversationIdsWithUnread(Collection<UUID> conversationIds, UUID receiverId) {
        if (conversationIds.isEmpty()) {
            return List.of();
        }
        return queryFactory
                .select(directMessage.conversation.id)
                .distinct()
                .from(directMessage)
                .where(directMessage.conversation.id.in(conversationIds)
                        .and(directMessage.receiver.id.eq(receiverId))
                        .and(directMessage.read.isFalse()))
                .fetch();
    }
}
