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

    public static OrderSpecifier<?>[] cursorOrder(
            DateTimePath<Instant> createdAt, ComparablePath<UUID> id, boolean isAsc) {
        OrderSpecifier<?> primary = isAsc ? createdAt.asc() : createdAt.desc();
        OrderSpecifier<?> secondary = isAsc ? id.asc() : id.desc();
        return new OrderSpecifier<?>[]{primary, secondary};
    }
}
