package com.codeit.team5.mopl.review.repository.querydsl;

import com.codeit.team5.mopl.notification.exception.CursorIdAfterNotTogetherException;
import com.codeit.team5.mopl.notification.exception.InvalidCursorException;
import com.codeit.team5.mopl.review.entity.QReview;
import com.codeit.team5.mopl.review.entity.Review;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewQueryRepositoryImpl implements ReviewQueryRepository {

    private static final String SORT_BY_CREATED_AT = "createdAt";
    private static final String SORT_BY_RATING = "rating";

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Review> findPageByContentId(UUID contentId, String cursor, UUID idAfter,
        Limit limit, String sortBy, boolean ascending) {

        QReview r = QReview.review;

        BooleanExpression cursorCondition = buildCursorCondition(r, cursor, idAfter, sortBy, ascending);
        OrderSpecifier<?>[] orderSpecifiers = buildOrderSpecifiers(r, sortBy, ascending);

        return queryFactory.selectFrom(r)
            .where(r.contentId.eq(contentId), cursorCondition)
            .orderBy(orderSpecifiers)
            .limit(limit.max())
            .fetch();
    }

    // 커서 페이지네이션을 위한 BooleanExpression 생성
    private BooleanExpression buildCursorCondition(QReview r, String cursor, UUID idAfter,
        String sortBy, boolean ascending) {

        // cursor와 idAfter를 한 세트로 받아오기
        if((cursor != null && idAfter == null) || (cursor == null && idAfter != null)) {
            throw new CursorIdAfterNotTogetherException();
        }

        // 첫 페이지면 커서가 없으니 조건 없음 (전체 조회)
        if (cursor == null) {
            return null;
        }

        if (SORT_BY_RATING.equals(sortBy)) { // 점수 기준 정렬
            double cursorRating;
            try {
                cursorRating = Double.parseDouble(cursor); // string 커서를 double 형식으로 파싱
            } catch (NumberFormatException e) {
                throw new InvalidCursorException();
            }
            return ascending
                // 커서 평점보다 평점이 큰 리뷰, 두개가 같으면 보조 커서를 기준으로 정렬
                ? r.rating.gt(cursorRating).or(r.rating.eq(cursorRating).and(r.id.gt(idAfter)))
                : r.rating.lt(cursorRating).or(r.rating.eq(cursorRating).and(r.id.lt(idAfter)));
        }

        Instant cursorInstant; // createdAt 기준 정렬
        try {
            cursorInstant = Instant.parse(cursor); // string 커서 -> Instant 파싱
        } catch (DateTimeParseException e) {
            throw new InvalidCursorException();
        }
        return ascending
            ? r.createdAt.gt(cursorInstant).or(r.createdAt.eq(cursorInstant).and(r.id.gt(idAfter)))
            : r.createdAt.lt(cursorInstant).or(r.createdAt.eq(cursorInstant).and(r.id.lt(idAfter)));
    }

    // orderBy를 적용하기 위해 OrderSpecifier 생성
    private OrderSpecifier<?>[] buildOrderSpecifiers(QReview r, String sortBy, boolean ascending) {
        if (SORT_BY_RATING.equals(sortBy)) { // 평점 순 정렬
            return ascending
                ? new OrderSpecifier<?>[] {r.rating.asc(), r.id.asc()}
                : new OrderSpecifier<?>[] {r.rating.desc(), r.id.desc()};
        }
        return ascending // createdAt 순 정렬
            ? new OrderSpecifier<?>[] {r.createdAt.asc(), r.id.asc()}
            : new OrderSpecifier<?>[] {r.createdAt.desc(), r.id.desc()};
    }
}
