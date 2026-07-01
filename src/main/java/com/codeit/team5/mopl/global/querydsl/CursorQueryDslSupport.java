package com.codeit.team5.mopl.global.querydsl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparablePath;
import com.querydsl.core.types.dsl.DateTimePath;
import java.time.Instant;
import java.util.UUID;

// createdAt + id 복합 커서 페이지네이션 공용 유틸
public final class CursorQueryDslSupport {

    private CursorQueryDslSupport() {
    }

    public static BooleanExpression cursorPredicate(
            DateTimePath<Instant> createdAt, ComparablePath<UUID> id,
            Instant cursor, UUID idAfter, boolean isAsc) {
        return isAsc
                ? createdAt.gt(cursor).or(createdAt.eq(cursor).and(id.gt(idAfter)))
                : createdAt.lt(cursor).or(createdAt.eq(cursor).and(id.lt(idAfter)));
    }

    // 문자열 커서를 파싱해 조건을 만든다. 둘 다 없으면 null, 한쪽만 있으면 파싱 실패로 예외
    public static BooleanExpression cursorPredicateFrom(
            DateTimePath<Instant> createdAt, ComparablePath<UUID> id,
            String cursor, String idAfter, boolean isAsc) {
        if (cursor == null && idAfter == null) {
            return null;
        }
        if (cursor == null || idAfter == null) {
            throw new IllegalArgumentException("cursor and idAfter must be provided together");
        }
        Instant cursorInstant = Instant.parse(cursor);
        UUID cursorId = UUID.fromString(idAfter);
        return cursorPredicate(createdAt, id, cursorInstant, cursorId, isAsc);
    }

    public static OrderSpecifier<?>[] cursorOrder(
            DateTimePath<Instant> createdAt, ComparablePath<UUID> id, boolean isAsc) {
        OrderSpecifier<?> primary = isAsc ? createdAt.asc() : createdAt.desc();
        OrderSpecifier<?> secondary = isAsc ? id.asc() : id.desc();
        return new OrderSpecifier<?>[]{primary, secondary};
    }
}
