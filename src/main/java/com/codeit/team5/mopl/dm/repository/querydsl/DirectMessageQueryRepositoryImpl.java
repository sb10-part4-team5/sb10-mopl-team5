package com.codeit.team5.mopl.dm.repository.querydsl;

import com.codeit.team5.mopl.dm.dto.request.DirectMessageCursorRequest;
import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.dm.entity.QDirectMessage;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
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
    public List<DirectMessage> findMessages(UUID conversationId, DirectMessageCursorRequest request, int fetchLimit) {
        return queryFactory
                .selectFrom(directMessage)
                .where(buildWhere(conversationId, request))
                .orderBy(buildOrder(request))
                .limit(fetchLimit)
                .fetch();
    }

    @Override
    public long countMessages(UUID conversationId) {
        Long count = queryFactory
                .select(directMessage.count())
                .from(directMessage)
                .where(directMessage.conversation.id.eq(conversationId))
                .fetchOne();
        return count != null ? count : 0L;
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

        Instant cursorInstant = Instant.parse(cursor);
        UUID id = UUID.fromString(idAfter);
        boolean isAsc = request.sortDirection() == Direction.ASC;

        BooleanExpression cursorCondition = isAsc
                ? directMessage.createdAt.gt(cursorInstant)
                        .or(directMessage.createdAt.eq(cursorInstant).and(directMessage.id.gt(id)))
                : directMessage.createdAt.lt(cursorInstant)
                        .or(directMessage.createdAt.eq(cursorInstant).and(directMessage.id.lt(id)));
        where.and(cursorCondition);
    }

    private OrderSpecifier<?>[] buildOrder(DirectMessageCursorRequest request) {
        boolean isAsc = request.sortDirection() == Direction.ASC;
        OrderSpecifier<?> primary = isAsc ? directMessage.createdAt.asc() : directMessage.createdAt.desc();
        OrderSpecifier<?> secondary = isAsc ? directMessage.id.asc() : directMessage.id.desc();
        return new OrderSpecifier<?>[]{primary, secondary};
    }
}
