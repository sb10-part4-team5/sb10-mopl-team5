package com.codeit.team5.mopl.playlist.repository.query;

import static com.codeit.team5.mopl.playlist.entity.QPlaylist.playlist;
import static com.codeit.team5.mopl.user.entity.QUser.user;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import com.codeit.team5.mopl.playlist.constant.PlaylistSortBy;
import com.codeit.team5.mopl.playlist.dto.PlaylistCursorCommand;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PlaylistQueryRepositoryImpl implements PlaylistQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final Map<PlaylistSortBy, CursorConditionBuilder> SORT_PATH_MAP =
            Map.of(PlaylistSortBy.UPDATED_AT, cursorBuilder(playlist.updatedAt),
                    PlaylistSortBy.SUBSCRIBE_COUNT, cursorBuilder(playlist.subscriberCount));


    @Override
    public List<Playlist> findByCursor(PlaylistCursorCommand request) {
        return queryFactory.selectFrom(playlist).leftJoin(playlist.owner, user).fetchJoin()
                .where(keywordLikeQuery(request), ownerQuery(request), subscriberQuery(request),
                        cursorQuery(request))
                .orderBy(orderSpecifiers(request)).limit(request.limit() + 1).fetch();
    }

    @Override
    public long countByCommand(PlaylistCursorCommand request) {
        Long count = queryFactory.select(playlist.count())
                .from(playlist)
                .where(keywordLikeQuery(request), ownerQuery(request), subscriberQuery(request))
                .fetchOne();
        return count != null ? count : 0L;
    }

    private OrderSpecifier<?>[] orderSpecifiers(PlaylistCursorCommand request) {
        boolean isAsc = request.sortDirection().isAscending();
        OrderSpecifier<?> primary = switch (request.sortBy()) {
            case UPDATED_AT -> isAsc ? playlist.updatedAt.asc() : playlist.updatedAt.desc();
            case SUBSCRIBE_COUNT -> isAsc ? playlist.subscriberCount.asc()
                    : playlist.subscriberCount.desc();
        };
        OrderSpecifier<?> secondary = isAsc ? playlist.id.asc() : playlist.id.desc();
        return new OrderSpecifier[] {primary, secondary};
    }

    private BooleanExpression cursorQuery(PlaylistCursorCommand request) {
        Object cursor = request.cursor();
        UUID idAfter = request.idAfter();
        if (cursor == null || idAfter == null) {
            return null;
        }

        boolean isAsc = request.sortDirection().isAscending();
        return SORT_PATH_MAP.get(request.sortBy()).build(cursor, idAfter, isAsc);
    }

    private BooleanExpression subscriberQuery(PlaylistCursorCommand request) {
        if (request.subscriberIdEqual() == null) {
            return null;
        }
        return playlist.owner.id.eq(request.subscriberIdEqual());
    }

    private BooleanExpression ownerQuery(PlaylistCursorCommand request) {
        if (request.ownerIdEqual() == null) {
            return null;
        }
        return playlist.owner.id.eq(request.ownerIdEqual());
    }

    private BooleanExpression keywordLikeQuery(PlaylistCursorCommand request) {
        if (!StringUtils.hasText(request.keywordLike())) {
            return null;
        }
        return playlist.description.containsIgnoreCase(request.keywordLike())
                .or(playlist.title.containsIgnoreCase(request.keywordLike()));
    }

    @FunctionalInterface
    private interface CursorConditionBuilder {
        BooleanExpression build(Object cursor, UUID idAfter, boolean isAsc);
    }

    private CursorConditionBuilder cursorBuilder(Expression<?> path) {
        return (cursor, idAfter, isAsc) -> {
            Expression<Object> cursorConst = Expressions.constant(cursor);
            BooleanExpression gt = Expressions.predicate(Ops.GT, path, cursorConst);
            BooleanExpression lt = Expressions.predicate(Ops.LT, path, cursorConst);
            BooleanExpression eq = Expressions.predicate(Ops.EQ, path, cursorConst);
            return isAsc ? gt.or(eq.and(playlist.id.gt(idAfter)))
                    : lt.or(eq.and(playlist.id.lt(idAfter)));
        };
    }
}
