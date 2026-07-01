package com.codeit.team5.mopl.dm.repository.querydsl;

import com.codeit.team5.mopl.dm.dto.request.ConversationCursorRequest;
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.entity.QConversation;
import com.codeit.team5.mopl.dm.exception.InvalidCursorException;
import com.codeit.team5.mopl.global.querydsl.CursorQueryDslSupport;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.format.DateTimeParseException;
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
    public List<Conversation> findMyConversations(UUID currentUserId, ConversationCursorRequest request) {
        int fetchLimit = request.limit() + 1;
        return queryFactory
                .selectFrom(conversation)
                .join(conversation.participant1).fetchJoin()
                .join(conversation.participant2).fetchJoin()
                .where(buildWhere(currentUserId, request))
                .orderBy(buildOrder(request))
                .limit(fetchLimit)
                .fetch();
    }

    private BooleanBuilder buildWhere(UUID currentUserId, ConversationCursorRequest request) {
        BooleanBuilder where = new BooleanBuilder();
        applyFilters(where, currentUserId, request);
        applyCursor(where, request);
        return where;
    }

    private void applyFilters(BooleanBuilder where, UUID currentUserId, ConversationCursorRequest request) {
        BooleanExpression keyword = keywordFilter(currentUserId, request.keywordLike());
        where.and(keyword != null ? keyword : participantFilter(currentUserId));
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
        boolean isAsc = request.sortDirection() == Direction.ASC;
        try {
            BooleanExpression predicate = CursorQueryDslSupport.cursorPredicateFrom(
                    conversation.createdAt, conversation.id,
                    request.cursor(), request.idAfter(), isAsc);
            if (predicate != null) {
                where.and(predicate);
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new InvalidCursorException(request.cursor(), request.idAfter());
        }
    }

    private OrderSpecifier<?>[] buildOrder(ConversationCursorRequest request) {
        boolean isAsc = request.sortDirection() == Direction.ASC;
        return CursorQueryDslSupport.cursorOrder(conversation.createdAt, conversation.id, isAsc);
    }
}
