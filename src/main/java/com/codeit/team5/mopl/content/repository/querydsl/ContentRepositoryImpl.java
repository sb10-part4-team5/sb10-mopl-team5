package com.codeit.team5.mopl.content.repository.querydsl;

import com.codeit.team5.mopl.content.dto.request.ContentCursorRequest;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.QContent;
import com.codeit.team5.mopl.content.entity.QContentStats;
import com.codeit.team5.mopl.content.entity.QContentTag;
import com.codeit.team5.mopl.tag.entity.QTag;
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
public class ContentRepositoryImpl implements ContentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QContent content = QContent.content;
    private static final QContentStats stats = QContentStats.contentStats;
    private static final QContentTag contentTag = QContentTag.contentTag;
    private static final QTag tag = QTag.tag;

    /**
     * 클라이언트 요청 조건에 맞는 Content 목록을 조회합니다.
     * To-One 관계인 stats는 fetchJoin으로 한 번에 가져와 N+1을 방지하고,
     * Limit을 적용하여 다음 페이지 확인을 위한 데이터(fetchLimit)까지 안전하게 조회합니다.
     */
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

    /**
     * 전체 검색 결과의 총 개수를 반환합니다.
     * 페이징 기준점(커서)은 개수 산정에 영향을 주지 않으므로 커서 조건은 제외하고 검색(Filter) 조건만 적용합니다.
     */
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

    /**
     * 동적 쿼리의 핵심 조립기입니다.
     * 빈 BooleanBuilder 상자에 검색 조건(Filters)과 커서 조건(Cursor)을 차례대로 안전하게 담아 반환합니다.
     */
    private BooleanBuilder buildWhere(ContentCursorRequest request) {
        BooleanBuilder where = new BooleanBuilder();
        applyFilters(where, request);
        applyCursor(where, request);
        return where;
    }

    /**
     * 사용자가 입력한 검색 조건(타입, 키워드, 태그)을 쿼리에 추가합니다.
     * 특히 태그(To-Many) 검색 시 서브쿼리를 사용하여, 메인 쿼리의 Row 뻥튀기 및 페이징 데이터 유실을 방지합니다.
     */
    private void applyFilters(BooleanBuilder where, ContentCursorRequest request) {
        if (request.typeEqual() != null) {
            where.and(content.type.eq(request.typeEqual()));
        }
        if (request.keywordLike() != null && !request.keywordLike().isBlank()) {
            where.and(content.title.containsIgnoreCase(request.keywordLike())
                    .or(content.description.containsIgnoreCase(request.keywordLike())));
        }
        if (request.tagsIn() != null && !request.tagsIn().isEmpty()) {
            List<String> normalizedTags = request.tagsIn().stream()
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .toList();
            where.and(content.id.in(
                    queryFactory.select(contentTag.content.id)
                            .from(contentTag)
                            .join(contentTag.tag, tag)
                            .where(tag.name.in(normalizedTags))
            ));
        }
    }

    /**
     * 무한 스크롤 시 중복 조회를 방지하기 위한 다음 페이지 기준점(Keyset) 조건을 설정합니다.
     * 정렬 기준값과 고유 ID를 조합하고, 엄격한 부등호(<, >)를 사용하여 방금 본 마지막 데이터가 쿼리에서 완벽히 제외되도록 처리합니다.
     */
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
                yield isAsc
                        ? stats.ratingSum.gt(cursorVal)
                            .or(stats.ratingSum.eq(cursorVal).and(content.id.gt(id)))
                        : stats.ratingSum.lt(cursorVal)
                            .or(stats.ratingSum.eq(cursorVal).and(content.id.lt(id)));
            }
        };
        where.and(cursorCondition);
    }

    /**
     * 클라이언트의 정렬 요청(Enum)을 안전한 Q-Class 필드와 매핑합니다.
     * 오타로 인한 런타임 에러를 방지(타입 안정성)하며, 값이 같을 경우 고유 ID로 2차 정렬하여 페이징 순서가 꼬이는 것을 막습니다.
     */
    private OrderSpecifier<?>[] buildOrder(ContentCursorRequest request) {
        boolean isAsc = request.sortDirection() == Direction.ASC;
        OrderSpecifier<?> primary = switch (request.sortBy()) {
            case CREATED_AT -> isAsc ? content.createdAt.asc() : content.createdAt.desc();
            case WATCHER_COUNT -> isAsc ? stats.watcherCount.asc() : stats.watcherCount.desc();
            case RATE -> isAsc ? stats.ratingSum.asc() : stats.ratingSum.desc();
        };
        OrderSpecifier<?> secondary = isAsc ? content.id.asc() : content.id.desc();
        return new OrderSpecifier<?>[]{ primary, secondary };
    }
}
