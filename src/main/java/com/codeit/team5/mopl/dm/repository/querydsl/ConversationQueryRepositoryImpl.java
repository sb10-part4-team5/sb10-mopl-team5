package com.codeit.team5.mopl.dm.repository.querydsl;

import com.codeit.team5.mopl.dm.dto.request.ConversationCursorRequest;
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.entity.QConversation;
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
public class ConversationQueryRepositoryImpl implements ConversationQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QConversation conversation = QConversation.conversation;

    @Override
    public List<Conversation> findMyConversations(UUID currentUserId, ConversationCursorRequest request,
            int fetchLimit) {
        return queryFactory
                .selectFrom(conversation)
                .join(conversation.participant1).fetchJoin()
                .join(conversation.participant2).fetchJoin()
                .where(buildWhere(currentUserId, request))
                .orderBy(buildOrder(request))
                .limit(fetchLimit)
                .fetch();
    }

    @Override
    public long countMyConversations(UUID currentUserId, ConversationCursorRequest request) {
        BooleanBuilder where = new BooleanBuilder();
        applyFilters(where, currentUserId, request);
        Long count = queryFactory
                .select(conversation.count())
                .from(conversation)
                .where(where)
                .fetchOne();
        return count != null ? count : 0L;
    }

    private BooleanBuilder buildWhere(UUID currentUserId, ConversationCursorRequest request) {
        BooleanBuilder where = new BooleanBuilder();
        applyFilters(where, currentUserId, request);
        applyCursor(where, request);
        return where;
    }

    private void applyFilters(BooleanBuilder where, UUID currentUserId, ConversationCursorRequest request) {
        where.and(participantFilter(currentUserId));
        where.and(keywordFilter(currentUserId, request.keywordLike()));
    }

    private BooleanExpression participantFilter(UUID me) {
        return conversation.participant1.id.eq(me).or(conversation.participant2.id.eq(me));
    }

    private BooleanExpression keywordFilter(UUID me, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return conversation.participant1.id.eq(me)
                .and(conversation.participant2.name.containsIgnoreCase(keyword))
                .or(conversation.participant2.id.eq(me)
                        .and(conversation.participant1.name.containsIgnoreCase(keyword)));
    }

    private void applyCursor(BooleanBuilder where, ConversationCursorRequest request) {
        String cursor = request.cursor();
        String idAfter = request.idAfter();
        if (cursor == null || idAfter == null) {
            return;
        }

        Instant cursorInstant = Instant.parse(cursor);
        UUID id = UUID.fromString(idAfter);
        boolean isAsc = request.sortDirection() == Direction.ASC;

        BooleanExpression cursorCondition = isAsc
                ? conversation.createdAt.gt(cursorInstant)
                        .or(conversation.createdAt.eq(cursorInstant).and(conversation.id.gt(id)))
                : conversation.createdAt.lt(cursorInstant)
                        .or(conversation.createdAt.eq(cursorInstant).and(conversation.id.lt(id)));
        where.and(cursorCondition);
    }

    private OrderSpecifier<?>[] buildOrder(ConversationCursorRequest request) {
        boolean isAsc = request.sortDirection() == Direction.ASC;
        OrderSpecifier<?> primary = isAsc ? conversation.createdAt.asc() : conversation.createdAt.desc();
        OrderSpecifier<?> secondary = isAsc ? conversation.id.asc() : conversation.id.desc();
        return new OrderSpecifier<?>[]{primary, secondary};
    }
}
