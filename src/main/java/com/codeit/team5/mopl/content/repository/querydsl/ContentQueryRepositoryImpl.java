package com.codeit.team5.mopl.content.repository.querydsl;

import com.codeit.team5.mopl.content.dto.request.ContentCursorRequest;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.content.entity.QContent;
import com.codeit.team5.mopl.content.entity.QContentStats;
import com.codeit.team5.mopl.content.entity.QContentTag;
import com.codeit.team5.mopl.tag.entity.QTag;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Repository;

// TODO(통합 테스트): typeEqual/keywordLike/tagsIn 필터, createdAt/watcherCount/rate 커서 페이지네이션,
//  hasNext 판별을 Testcontainers(PostgreSQL) 기반 @DataJpaTest로 검증 필요
@Repository
@RequiredArgsConstructor
public class ContentQueryRepositoryImpl implements ContentQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QContent content = QContent.content;
    private static final QContentStats stats = QContentStats.contentStats;
    private static final QContentTag contentTag = QContentTag.contentTag;
    private static final QTag tag = QTag.tag;

    @Override
    public List<Content> findContents(ContentCursorRequest request, int fetchLimit) {
        return queryFactory
                .selectFrom(content)
                .leftJoin(content.stats, stats).fetchJoin()
                .where(buildWhere(request))
                .orderBy(buildOrder(request))
                .limit(fetchLimit)
                .fetch();
    }

    @Override
    public long countContents(ContentCursorRequest request) {
        BooleanBuilder where = new BooleanBuilder();
        applyFilters(where, request);
        Long count = queryFactory
                .select(content.count())
                .from(content)
                .where(where)
                .fetchOne();
        return count != null ? count : 0L;
    }

    private BooleanBuilder buildWhere(ContentCursorRequest request) {
        BooleanBuilder where = new BooleanBuilder();
        applyFilters(where, request);
        applyCursor(where, request);
        return where;
    }

    private void applyFilters(BooleanBuilder where, ContentCursorRequest request) {
        where.and(typeFilter(request.typeEqual()));
        where.and(keywordFilter(request.keywordLike()));
        where.and(tagsFilter(request.tagsIn()));
    }

    private BooleanExpression typeFilter(ContentType typeEqual) {
        return typeEqual != null ? content.type.eq(typeEqual) : null;
    }

    private BooleanExpression keywordFilter(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return content.title.containsIgnoreCase(keyword)
                .or(content.description.containsIgnoreCase(keyword));
    }

    private BooleanExpression tagsFilter(List<String> tagsIn) {
        if (tagsIn == null || tagsIn.isEmpty()) return null;
        List<String> normalizedTags = tagsIn.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();
        return content.id.in(
                queryFactory.select(contentTag.content.id)
                        .from(contentTag)
                        .join(contentTag.tag, tag)
                        .where(tag.name.in(normalizedTags))
        );
    }

    private void applyCursor(BooleanBuilder where, ContentCursorRequest request) {
        String cursor = request.cursor();
        String idAfter = request.idAfter();
        if (cursor == null || idAfter == null) {
            return;
        }

        UUID id = UUID.fromString(idAfter);
        boolean isAsc = request.sortDirection() == Direction.ASC;

        BooleanExpression cursorCondition = switch (request.sortBy()) {
            case CREATED_AT -> {
                Instant cursorInstant = Instant.parse(cursor);
                yield isAsc
                        ? content.createdAt.gt(cursorInstant)
                            .or(content.createdAt.eq(cursorInstant).and(content.id.gt(id)))
                        : content.createdAt.lt(cursorInstant)
                            .or(content.createdAt.eq(cursorInstant).and(content.id.lt(id)));
            }
            case WATCHER_COUNT -> {
                long cursorVal = Long.parseLong(cursor);
                yield isAsc
                        ? stats.watcherCount.gt(cursorVal)
                            .or(stats.watcherCount.eq(cursorVal).and(content.id.gt(id)))
                        : stats.watcherCount.lt(cursorVal)
                            .or(stats.watcherCount.eq(cursorVal).and(content.id.lt(id)));
            }
            case RATE -> {
                double cursorVal = Double.parseDouble(cursor);
                NumberExpression<Double> avgRating = averageRating();
                yield isAsc
                        ? avgRating.gt(cursorVal)
                            .or(avgRating.eq(cursorVal).and(content.id.gt(id)))
                        : avgRating.lt(cursorVal)
                            .or(avgRating.eq(cursorVal).and(content.id.lt(id)));
            }
        };
        where.and(cursorCondition);
    }

    private NumberExpression<Double> averageRating() {
        return new CaseBuilder()
                .when(stats.reviewCount.eq(0))
                .then(0.0)
                .otherwise(stats.ratingSum.divide(stats.reviewCount.doubleValue()));
    }

    private OrderSpecifier<?>[] buildOrder(ContentCursorRequest request) {
        boolean isAsc = request.sortDirection() == Direction.ASC;
        NumberExpression<Double> avgRating = averageRating();
        OrderSpecifier<?> primary = switch (request.sortBy()) {
            case CREATED_AT -> isAsc ? content.createdAt.asc() : content.createdAt.desc();
            case WATCHER_COUNT -> isAsc ? stats.watcherCount.asc() : stats.watcherCount.desc();
            case RATE -> isAsc ? avgRating.asc() : avgRating.desc();
        };
        OrderSpecifier<?> secondary = isAsc ? content.id.asc() : content.id.desc();
        return new OrderSpecifier<?>[]{ primary, secondary };
    }
}
