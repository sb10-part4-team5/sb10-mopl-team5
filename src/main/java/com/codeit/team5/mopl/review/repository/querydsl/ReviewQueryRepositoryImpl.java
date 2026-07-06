package com.codeit.team5.mopl.review.repository.querydsl;

import com.codeit.team5.mopl.review.entity.QReview;
import com.codeit.team5.mopl.review.entity.Review;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewQueryRepositoryImpl implements ReviewQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QReview r = QReview.review;

    @Override
    public List<Review> findPageByContentIdSortByRating(UUID contentId, Double cursor, UUID idAfter,
        Limit limit, Sort.Direction sortDirection) {

        BooleanExpression cursorCondition = null;
        if (cursor != null) {
            cursorCondition = sortDirection.isDescending()
                ? r.rating.lt(cursor).or(r.rating.eq(cursor).and(r.id.gt(idAfter)))
                : r.rating.gt(cursor).or(r.rating.eq(cursor).and(r.id.gt(idAfter)));
        }

        return queryFactory.selectFrom(r)
            .join(r.author).fetchJoin()
            .where(r.content.id.eq(contentId), cursorCondition)
            .orderBy(ratingOrder(sortDirection))
            .limit(limit.max())
            .fetch();
    }

    @Override
    public List<Review> findPageByContentIdSortByCreatedAt(UUID contentId, Instant cursor, UUID idAfter,
        Limit limit, Sort.Direction sortDirection) {

        BooleanExpression cursorCondition = null;
        if (cursor != null) {
            cursorCondition = sortDirection.isDescending()
                ? r.createdAt.lt(cursor).or(r.createdAt.eq(cursor).and(r.id.gt(idAfter)))
                : r.createdAt.gt(cursor).or(r.createdAt.eq(cursor).and(r.id.gt(idAfter)));
        }

        return queryFactory.selectFrom(r)
            .join(r.author).fetchJoin()
            .where(r.content.id.eq(contentId), cursorCondition)
            .orderBy(createdAtOrder(sortDirection))
            .limit(limit.max())
            .fetch();
    }

    private OrderSpecifier<?>[] ratingOrder(Sort.Direction direction) {
        return direction.isDescending()
            ? new OrderSpecifier<?>[] {r.rating.desc(), r.id.asc()}
            : new OrderSpecifier<?>[] {r.rating.asc(), r.id.asc()};
    }

    private OrderSpecifier<?>[] createdAtOrder(Sort.Direction direction) {
        return direction.isDescending()
            ? new OrderSpecifier<?>[] {r.createdAt.desc(), r.id.asc()}
            : new OrderSpecifier<?>[] {r.createdAt.asc(), r.id.asc()};
    }
}
