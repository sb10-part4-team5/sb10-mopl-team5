package com.codeit.team5.mopl.notification.repository.querydsl;

import com.codeit.team5.mopl.notification.entity.Notification;
import com.codeit.team5.mopl.notification.entity.QNotification;
import com.codeit.team5.mopl.notification.exception.CursorIdAfterNotTogetherException;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepositoryImpl implements NotificationQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Notification> findPageByReceiverDesc(UUID receiverId, Instant cursor, UUID idAfter, Limit limit) {
        if ((cursor == null) != (idAfter == null)) {
            throw new CursorIdAfterNotTogetherException();
        }
        QNotification n = QNotification.notification;

        BooleanExpression cursorCondition = cursor == null ? null :
                n.createdAt.lt(cursor).or(n.createdAt.eq(cursor).and(n.id.lt(idAfter)));

        return queryFactory.selectFrom(n)
                .where(
                        n.receiverId.eq(receiverId),
                        n.isRead.isFalse(),
                        cursorCondition
                )
                .orderBy(n.createdAt.desc(), n.id.desc())
                .limit(limit.max())
                .fetch();
    }

    @Override
    public List<Notification> findPageByReceiverAsc(UUID receiverId, Instant cursor, UUID idAfter, Limit limit) {
        if ((cursor == null) != (idAfter == null)) {
            throw new CursorIdAfterNotTogetherException();
        }
        QNotification n = QNotification.notification;

        BooleanExpression cursorCondition = cursor == null ? null :
                n.createdAt.gt(cursor).or(n.createdAt.eq(cursor).and(n.id.gt(idAfter)));

        return queryFactory.selectFrom(n)
                .where(
                        n.receiverId.eq(receiverId),
                        n.isRead.isFalse(),
                        cursorCondition
                )
                .orderBy(n.createdAt.asc(), n.id.asc())
                .limit(limit.max())
                .fetch();
    }

    @Override
    public List<Notification> findMissedNotifications(UUID receiverId, UUID lastEventId) {
        QNotification n = QNotification.notification;

        Notification ref = queryFactory.selectFrom(n)
            .where(n.id.eq(lastEventId).and(n.receiverId.eq(receiverId)))
            .fetchOne();

        if (ref == null) {
            return List.of();
        }

        BooleanExpression afterCursor = n.createdAt.gt(ref.getCreatedAt())
            .or(n.createdAt.eq(ref.getCreatedAt()).and(n.id.gt(ref.getId())));

        return queryFactory.selectFrom(n)
            .where(
                n.receiverId.eq(receiverId),
                n.isRead.isFalse(),
                afterCursor)
            .orderBy(n.createdAt.asc(), n.id.asc())
            .fetch();
    }

}
