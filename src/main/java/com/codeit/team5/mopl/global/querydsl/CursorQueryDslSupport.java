package com.codeit.team5.mopl.global.querydsl;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpression;
import com.querydsl.core.types.dsl.ComparablePath;
import java.util.UUID;

// 정렬 컬럼 + id 복합 커서 페이지네이션 공용 유틸
public final class CursorQueryDslSupport {

    private CursorQueryDslSupport() {
    }

    public static <T extends Comparable<?>> BooleanExpression cursorPredicate(
            ComparableExpression<T> sortColumn, ComparablePath<UUID> id,
            T cursor, UUID idAfter, boolean isAsc) {
        return isAsc
                ? sortColumn.gt(cursor).or(sortColumn.eq(cursor).and(id.gt(idAfter)))
                : sortColumn.lt(cursor).or(sortColumn.eq(cursor).and(id.lt(idAfter)));
    }

    public static <T extends Comparable<?>> OrderSpecifier<?>[] cursorOrder(
            ComparableExpression<T> sortColumn, ComparablePath<UUID> id, boolean isAsc) {
        OrderSpecifier<?> primary = isAsc ? sortColumn.asc() : sortColumn.desc();
        OrderSpecifier<?> secondary = isAsc ? id.asc() : id.desc();
        return new OrderSpecifier<?>[]{primary, secondary};
    }
}
