package com.codeit.team5.mopl.user.repository.querydsl;

import com.codeit.team5.mopl.user.dto.request.UserCursorRequest;
import com.codeit.team5.mopl.user.entity.QUser;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.entity.UserRole;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QUser user = QUser.user;

    @Override
    public List<User> findUsers(UserCursorRequest request, int fetchLimit) {
        return queryFactory
                .selectFrom(user)
                .leftJoin(user.profileImage).fetchJoin()
                .where(buildWhere(request))
                .orderBy(buildOrder(request))
                .limit(fetchLimit)
                .fetch();
    }

    @Override
    public long countUsers(UserCursorRequest request) {
        BooleanBuilder where = new BooleanBuilder();
        applyFilters(where, request);

        Long count = queryFactory
                .select(user.count())
                .from(user)
                .where(where)
                .fetchOne();

        return count != null ? count : 0L;
    }

    private BooleanBuilder buildWhere(UserCursorRequest request) {
        BooleanBuilder where = new BooleanBuilder();
        applyFilters(where, request);
        applyCursor(where, request);
        return where;
    }

    private void applyFilters(BooleanBuilder where, UserCursorRequest request) {
        where.and(emailFilter(request.emailLike()));
        where.and(roleFilter(request.roleEqual()));
        where.and(lockedFilter(request.isLocked()));
    }

    private BooleanExpression emailFilter(String emailLike) {
        if (emailLike == null || emailLike.isBlank()) {
            return null;
        }
        return user.email.containsIgnoreCase(emailLike);
    }

    private BooleanExpression roleFilter(UserRole roleEqual) {
        return roleEqual != null ? user.role.eq(roleEqual) : null;
    }

    private BooleanExpression lockedFilter(Boolean isLocked) {
        return isLocked != null ? user.locked.eq(isLocked) : null;
    }

    private void applyCursor(BooleanBuilder where, UserCursorRequest request) {
        String cursor = request.cursor();
        if (cursor == null || cursor.isBlank() || request.idAfter() == null) {
            return;
        }

        boolean isAsc = request.sortDirection() == Direction.ASC;

        BooleanExpression cursorCondition = switch (request.sortBy()) {
            case NAME -> isAsc
                    ? user.name.gt(cursor)
                    .or(user.name.eq(cursor).and(user.id.gt(request.idAfter())))
                    : user.name.lt(cursor)
                            .or(user.name.eq(cursor).and(user.id.lt(request.idAfter())));

            case EMAIL -> isAsc
                    ? user.email.gt(cursor)
                    .or(user.email.eq(cursor).and(user.id.gt(request.idAfter())))
                    : user.email.lt(cursor)
                            .or(user.email.eq(cursor).and(user.id.lt(request.idAfter())));

            case CREATED_AT -> {
                Instant cursorInstant = Instant.parse(cursor);
                yield isAsc
                        ? user.createdAt.gt(cursorInstant)
                        .or(user.createdAt.eq(cursorInstant).and(user.id.gt(request.idAfter())))
                        : user.createdAt.lt(cursorInstant)
                                .or(user.createdAt.eq(cursorInstant).and(user.id.lt(request.idAfter())));
            }

            case LOCKED -> {
                boolean cursorBoolean = Boolean.parseBoolean(cursor);
                yield isAsc
                        ? user.locked.gt(cursorBoolean)
                        .or(user.locked.eq(cursorBoolean).and(user.id.gt(request.idAfter())))
                        : user.locked.lt(cursorBoolean)
                                .or(user.locked.eq(cursorBoolean).and(user.id.lt(request.idAfter())));
            }

            case ROLE -> isAsc
                    ? user.role.stringValue().gt(cursor)
                    .or(user.role.stringValue().eq(cursor).and(user.id.gt(request.idAfter())))
                    : user.role.stringValue().lt(cursor)
                            .or(user.role.stringValue().eq(cursor).and(user.id.lt(request.idAfter())));
        };

        where.and(cursorCondition);
    }

    private OrderSpecifier<?>[] buildOrder(UserCursorRequest request) {
        boolean isAsc = request.sortDirection() == Direction.ASC;

        OrderSpecifier<?> primary = switch (request.sortBy()) {
            case NAME -> isAsc ? user.name.asc() : user.name.desc();
            case EMAIL -> isAsc ? user.email.asc() : user.email.desc();
            case CREATED_AT -> isAsc ? user.createdAt.asc() : user.createdAt.desc();
            case LOCKED -> isAsc ? user.locked.asc() : user.locked.desc();
            case ROLE -> isAsc ? user.role.asc() : user.role.desc();
        };

        OrderSpecifier<?> secondary = isAsc ? user.id.asc() : user.id.desc();

        return new OrderSpecifier<?>[]{primary, secondary};
    }
}
