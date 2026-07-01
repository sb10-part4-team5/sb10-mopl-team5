package com.codeit.team5.mopl.dm.repository.querydsl;

import com.codeit.team5.mopl.dm.dto.request.DirectMessageCursorRequest;
import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.dm.entity.QDirectMessage;
import com.codeit.team5.mopl.dm.exception.InvalidCursorException;
import com.codeit.team5.mopl.global.querydsl.CursorQueryDslSupport;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DirectMessageQueryRepositoryImpl implements DirectMessageQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QDirectMessage directMessage = QDirectMessage.directMessage;

    @Override
    public List<DirectMessage> findMessages(UUID conversationId, DirectMessageCursorRequest request) {
        int fetchLimit = request.limit() + 1;
        return queryFactory
                .selectFrom(directMessage)
                .join(directMessage.sender).fetchJoin()
                .join(directMessage.receiver).fetchJoin()
                .where(buildWhere(conversationId, request))
                .orderBy(buildOrder(request))
                .limit(fetchLimit)
                .fetch();
    }

    @Override
    public List<DirectMessage> findLatestMessagesByConversationIds(Collection<UUID> conversationIds) {
        if (conversationIds.isEmpty()) {
            return List.of();
        }
        QDirectMessage latest = new QDirectMessage("latest");
        return queryFactory
                .selectFrom(directMessage)
                .join(directMessage.sender).fetchJoin()
                .join(directMessage.receiver).fetchJoin()
                .where(directMessage.conversation.id.in(conversationIds)
                        .and(directMessage.createdAt.eq(
                                JPAExpressions.select(latest.createdAt.max())
                                        .from(latest)
                                        .where(latest.conversation.id.eq(directMessage.conversation.id)))))
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

    private BooleanBuilder buildWhere(UUID conversationId, DirectMessageCursorRequest request) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(directMessage.conversation.id.eq(conversationId));
        applyCursor(where, request);
        return where;
    }

    private void applyCursor(BooleanBuilder where, DirectMessageCursorRequest request) {
        String cursor = request.cursor();
        String idAfter = request.idAfter();
        if (cursor == null || idAfter == null) {
            return;
        }

        Instant cursorInstant;
        UUID id;
        try {
            cursorInstant = Instant.parse(cursor);
            id = UUID.fromString(idAfter);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new InvalidCursorException(cursor, idAfter);
        }
        boolean isAsc = request.sortDirection() == Direction.ASC;
        where.and(CursorQueryDslSupport.cursorPredicate(
                directMessage.createdAt, directMessage.id, cursorInstant, id, isAsc));
    }

    private OrderSpecifier<?>[] buildOrder(DirectMessageCursorRequest request) {
        boolean isAsc = request.sortDirection() == Direction.ASC;
        return CursorQueryDslSupport.cursorOrder(directMessage.createdAt, directMessage.id, isAsc);
    }
}
