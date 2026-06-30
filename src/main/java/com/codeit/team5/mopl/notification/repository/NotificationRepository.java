package com.codeit.team5.mopl.notification.repository;

import com.codeit.team5.mopl.notification.entity.Notification;
import com.codeit.team5.mopl.notification.exception.CursorIdAfterNotTogetherException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // 수신자별 알림 목록 (커서 페이지네이션, 최신순). cursor=null 이면 첫 페이지.
    // 안 읽은 알림만 반환 (이미 읽음 처리한 알림이 목록에 계속 다시 뜨는 것을 방지)
    default List<Notification> findPageByReceiverDesc(
            UUID receiverId, Instant cursor, UUID idAfter, Limit limit) {
        if((cursor == null) != (idAfter == null)){
            throw new CursorIdAfterNotTogetherException();
        }
        boolean firstPage = (cursor == null);
        return findPageByReceiverDescInternal(receiverId, firstPage, cursor, idAfter, limit);
    }

    // 수신자별 알림 목록 (커서 페이지네이션, 오래된순). cursor=null 이면 첫 페이지.
    default List<Notification> findPageByReceiverAsc(
            UUID receiverId, Instant cursor, UUID idAfter, Limit limit) {
        if((cursor == null) != (idAfter == null)){
            throw new CursorIdAfterNotTogetherException();
        }
        boolean firstPage = (cursor == null);
        return findPageByReceiverAscInternal(receiverId, firstPage, cursor, idAfter, limit);
    }

    // (createdAt, id) 복합 커서로 동일 시각 tie-break. firstPage 플래그로 첫 페이지를 구분
    @Query("""
        select n from Notification n
        where n.receiverId = :receiverId
          and n.isRead = false
          and (:firstPage = true
               or n.createdAt < :cursor
               or (n.createdAt = :cursor and n.id < :idAfter))
        order by n.createdAt desc, n.id desc
        """)
    List<Notification> findPageByReceiverDescInternal(
            @Param("receiverId") UUID receiverId,
            @Param("firstPage") boolean firstPage,
            @Param("cursor") Instant cursor,
            @Param("idAfter") UUID idAfter,
            Limit limit);

    @Query("""
        select n from Notification n
        where n.receiverId = :receiverId
          and  n.isRead = false
          and (:firstPage = true
               or n.createdAt > :cursor
               or (n.createdAt = :cursor and n.id > :idAfter))
        order by n.createdAt asc, n.id asc
        """)
    List<Notification> findPageByReceiverAscInternal(
            @Param("receiverId") UUID receiverId,
            @Param("firstPage") boolean firstPage,
            @Param("cursor") Instant cursor,
            @Param("idAfter") UUID idAfter,
            Limit limit);

    // 응답의 totalCount 용
    long countByReceiverId(UUID receiverId);

    // 안 읽은 알림 개수 - idx_noti_unread 활용
    long countByReceiverIdAndIsReadFalse(UUID receiverId);

    // 단건 읽음 처리 시 소유권 검증 조회
    Optional<Notification> findByIdAndReceiverId(UUID id, UUID receiverId);

    // 특정 알림 이후에 생성된 일반 알림 조회 (DM 제외, SSE 재연결 시 미수신 알림 전송용)
    @Query("""
        select n from Notification n, Notification ref
        where ref.id = :lastEventId
          and ref.receiverId = :receiverId
          and n.receiverId = :receiverId
          and n.type <> com.codeit.team5.mopl.notification.entity.NotificationType.DIRECT_MESSAGE
          and n.isRead <> true
          and (n.createdAt > ref.createdAt
            or (n.createdAt = ref.createdAt and n.id > ref.id))
        order by n.createdAt asc, n.id asc
        """)
    List<Notification> findMissedNotifications(
            @Param("receiverId") UUID receiverId,
            @Param("lastEventId") UUID lastEventId);

    // 특정 알림 이후에 생성된 DM 알림 조회 (SSE 재연결 시 미수신 DM 전송용)
    @Query("""
        select n from Notification n, Notification ref
        where ref.id = :lastEventId
          and ref.receiverId = :receiverId
          and n.receiverId = :receiverId
          and n.type = com.codeit.team5.mopl.notification.entity.NotificationType.DIRECT_MESSAGE
          and n.isRead <> true
          and (n.createdAt > ref.createdAt
            or (n.createdAt = ref.createdAt and n.id > ref.id))
        order by n.createdAt asc, n.id asc
        """)
    List<Notification> findMissedDirectMessages(
            @Param("receiverId") UUID receiverId,
            @Param("lastEventId") UUID lastEventId);
}
