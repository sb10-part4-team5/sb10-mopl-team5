package com.codeit.team5.mopl.playlist.repository.query;

import static com.codeit.team5.mopl.binarycontent.entity.QBinaryContent.binaryContent;
import static com.codeit.team5.mopl.content.entity.QContent.content;
import static com.codeit.team5.mopl.content.entity.QContentStats.contentStats;
import static com.codeit.team5.mopl.playlist.entity.QPlaylist.playlist;
import static com.codeit.team5.mopl.playlist.entity.QPlaylistItem.playlistItem;
import static com.codeit.team5.mopl.subscription.entity.QSubscription.subscription;
import static com.codeit.team5.mopl.user.entity.QUser.user;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.playlist.dto.PlaylistContentsDto;
import com.codeit.team5.mopl.playlist.dto.PlaylistCursorCommand;
import com.codeit.team5.mopl.playlist.entity.Playlist;
import com.codeit.team5.mopl.playlist.entity.PlaylistItem;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PlaylistQueryRepositoryImpl implements PlaylistQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final BooleanPath IS_SUBSCRIBED = Expressions.booleanPath("isSubscribed");

    @Override
    public Optional<PlaylistContentsDto> findByIdWithContents(UUID id, UUID userId) {
        Tuple result = selectPlaylistWithSubscription(userId).where(playlist.id.eq(id)).fetchOne();
        if (result == null) {
            return Optional.empty();
        }
        Playlist foundPlaylist = result.get(playlist);
        boolean subscribedByMe = Boolean.TRUE.equals(result.get(IS_SUBSCRIBED));
        List<PlaylistItem> items = fetchPlaylistItemsWithContents(List.of(id));
        return Optional.of(new PlaylistContentsDto(foundPlaylist,
                items.stream().map(PlaylistItem::getContent).toList(), subscribedByMe));
    }

    @Override
    public List<PlaylistContentsDto> findByCursor(PlaylistCursorCommand request, UUID userId) {
        List<Tuple> results = selectPlaylistWithSubscription(userId)
                .where(keywordLikeQuery(request), ownerQuery(request), subscriberQuery(request),
                        cursorQuery(request))
                .orderBy(orderSpecifiers(request)).limit(request.limit() + 1).fetch();

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> playlistIds =
                results.stream().map(t -> t.get(playlist)).map(Playlist::getId).toList();
        List<PlaylistItem> items = fetchPlaylistItemsWithContents(playlistIds);
        Map<UUID, List<Content>> contentsByPlaylistId =
                items.stream().collect(Collectors.groupingBy(PlaylistItem::getPlaylistId,
                        Collectors.mapping(PlaylistItem::getContent, Collectors.toList())));
        return results.stream().map(t -> mapToDto(t, contentsByPlaylistId))
                .toList();
    }

    @Override
    public long countByCommand(PlaylistCursorCommand request) {
        Long count = queryFactory.select(playlist.count()).from(playlist)
                .where(keywordLikeQuery(request), ownerQuery(request), subscriberQuery(request))
                .fetchOne();
        return count != null ? count : 0L;
    }

    private List<PlaylistItem> fetchPlaylistItemsWithContents(List<UUID> playlistIds) {
        return queryFactory.selectFrom(playlistItem).leftJoin(playlistItem.content, content)
                .fetchJoin().leftJoin(content.thumbnail, binaryContent).fetchJoin()
                .leftJoin(content.stats, contentStats).fetchJoin()
                .where(playlistItem.playlistId.in(playlistIds))
                .orderBy(playlistItem.createdAt.asc()).fetch();
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

        CursorConditionBuilder builder = switch (request.sortBy()) {
            case UPDATED_AT -> cursorBuilder(playlist.updatedAt);
            case SUBSCRIBE_COUNT -> cursorBuilder(playlist.subscriberCount);
        };

        return builder.build(cursor, idAfter, isAsc);
    }

    private BooleanExpression subscriberQuery(PlaylistCursorCommand request) {
        if (request.subscriberIdEqual() == null) {
            return null;
        }
        return JPAExpressions.selectOne().from(subscription).where(subscription.playlist.id
                .eq(playlist.id).and(subscription.subscriber.id.eq(request.subscriberIdEqual())))
                .exists();
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

    private PlaylistContentsDto mapToDto(Tuple t, Map<UUID, List<Content>> contentsByPlaylistId) {
        Playlist p = t.get(playlist);
        return new PlaylistContentsDto(p,
                contentsByPlaylistId.getOrDefault(p.getId(), Collections.emptyList()),
                Boolean.TRUE.equals(t.get(IS_SUBSCRIBED)));
    }

    private BooleanExpression subscribedByMeSubquery(UUID userId) {
        return JPAExpressions.selectOne().from(subscription).where(
                subscription.playlist.id.eq(playlist.id).and(subscription.subscriber.id.eq(userId)))
                .exists();
    }

    private JPAQuery<Tuple> selectPlaylistWithSubscription(UUID userId) {
        return queryFactory.select(playlist, ExpressionUtils.as(subscribedByMeSubquery(userId), IS_SUBSCRIBED)).from(playlist)
                .leftJoin(playlist.owner, user).fetchJoin()
                .leftJoin(user.profileImage, binaryContent).fetchJoin();
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
