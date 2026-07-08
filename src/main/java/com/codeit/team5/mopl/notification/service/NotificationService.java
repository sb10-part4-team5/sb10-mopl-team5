package com.codeit.team5.mopl.notification.service;

import com.codeit.team5.mopl.notification.dto.CursorResponseNotificationDto;
import com.codeit.team5.mopl.notification.dto.request.NotificationBatchCreateCommand;
import com.codeit.team5.mopl.notification.dto.request.NotificationCreateCommand;
import com.codeit.team5.mopl.notification.dto.request.NotificationListQuery;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.dto.response.NotificationResponse;
import com.codeit.team5.mopl.notification.entity.Notification;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.event.NotificationCreatedEvent;
import com.codeit.team5.mopl.notification.event.NotificationsBatchCreatedEvent;
import com.codeit.team5.mopl.notification.exception.InvalidCursorException;
import com.codeit.team5.mopl.notification.exception.InvalidSortByException;
import com.codeit.team5.mopl.notification.exception.InvalidSortDirectionException;
import com.codeit.team5.mopl.notification.exception.NotificationNotFoundException;
import com.codeit.team5.mopl.notification.mapper.NotificationMapper;
import com.codeit.team5.mopl.notification.repository.NotificationRepository;
import com.codeit.team5.mopl.sse.exception.InvalidLastEventIdException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final String SORT_BY_CREATED_AT = "createdAt";
    private static final String SORT_ASCENDING = "ASCENDING";
    private static final String SORT_DESCENDING = "DESCENDING";

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher publisher;

    // 알림 생성 및 저장
    // AFTER_COMMIT 이벤트 리스너(트랜잭션 동기화 콜백)에서 호출되므로,
    // 활성 트랜잭션 유무와 무관하게 항상 새 물리 트랜잭션을 보장하기 위해 REQUIRES_NEW 사용
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponse create(NotificationCreateCommand command) {
        Notification notification = Notification.create(
                command.receiverId(), command.type(), command.title(), command.content(), command.level());
        Notification saved = notificationRepository.save(notification);
        NotificationPayload payload = notificationMapper.toPayload(saved);
        NotificationResponse response = notificationMapper.toResponse(saved);
        log.info("알림 생성됨: type={}", command.type());

        publisher.publishEvent(new NotificationCreatedEvent(payload));


        return response;
    }

    // 여러 수신자에게 동일한 알림을 배치로 생성한다.
    // N번의 개별 트랜잭션 대신 단일 트랜잭션 + saveAll로 DB 왕복을 줄인다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createAll(NotificationBatchCreateCommand command) {
        if (command.receiverIds().isEmpty()) {
            return;
        }
        List<Notification> notifications = command.receiverIds().stream()
                .map(receiverId -> Notification.create(
                        receiverId, command.type(), command.title(), command.content(), command.level()))
                .toList();
        List<Notification> saved = notificationRepository.saveAll(notifications);
        log.info("배치 알림 생성됨: type={}, count={}", command.type(), saved.size());

        if (command.type() != NotificationType.DIRECT_MESSAGE) {
            List<NotificationPayload> payloads = saved.stream()
                    .map(notificationMapper::toPayload)
                    .toList();
            publisher.publishEvent(new NotificationsBatchCreatedEvent(payloads));
        }
    }

    // 수신자별 알림 목록 조회 (커서 페이지네이션)
    // 안 읽은 알림만 조회하여, 이미 읽은 알림이 조회할 때마다 계속 다시 뜨는 것을 방지
    public CursorResponseNotificationDto getNotifications(NotificationListQuery query) {

        // 지원하는 정렬 값인지 검증하고 오름차순 여부를 결정 (응답 메타와 실제 정렬을 일치시킴)
        boolean ascending = resolveAscending(query.sortBy(), query.sortDirection());

        // 주 커서는 createdAt 문자열로 들어오므로 Instant로 파싱. 없으면(null) 첫 페이지
        Instant cursorInstant;
        try {
            cursorInstant = (query.cursor() == null || query.cursor().isBlank())
                    ? null : Instant.parse(query.cursor());
        } catch (DateTimeParseException e) {
            throw new InvalidCursorException();
        }

        // 다음 페이지 존재 여부를 알기 위해 limit보다 1개 더 조회
        Limit fetchLimit = Limit.of(query.limit() + 1);

        // 정렬 방향에 맞는 커서 조회 메서드 선택
        List<Notification> rows = ascending
                ? notificationRepository.findPageByReceiverAsc(query.receiverId(), cursorInstant, query.idAfter(), fetchLimit)
                : notificationRepository.findPageByReceiverDesc(query.receiverId(), cursorInstant, query.idAfter(), fetchLimit);

        // limit + 1개가 조회됐으면 다음 페이지가 있는 것. 초과분은 잘라내고 limit개만 노출
        boolean hasNext = rows.size() > query.limit();
        List<Notification> page = hasNext ? rows.subList(0, query.limit()) : rows;

        // 다음 페이지가 있을 때만 마지막 요소의 (createdAt, id)로 다음 커서를 구성
        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !page.isEmpty()) {
            Notification last = page.get(page.size() - 1);
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId();
        }

        // 응답의 totalCount (안 읽은 알림 수)
        long totalCount = notificationRepository.countByReceiverIdAndIsReadFalse(query.receiverId());

        return new CursorResponseNotificationDto(
                notificationMapper.toResponseList(page),
                nextCursor, nextIdAfter, hasNext, totalCount, query.sortBy(), query.sortDirection());
    }

    // 안 읽은 알림 개수
    public long countUnread(UUID receiverId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    // SSE 재연결 시 미수신 일반 알림 조회 (DM 제외)
    public List<NotificationPayload> findMissedNotifications(UUID receiverId, UUID lastEventId) {
        validateLastEventId(receiverId, lastEventId);

        return notificationRepository.findMissedNotifications(receiverId, lastEventId).stream()
                .map(notificationMapper::toPayload)
                .toList();
    }

    // Last-Event-ID 유효성 검증
    private void validateLastEventId(UUID receiverId, UUID lastEventId) {
        notificationRepository.findByIdAndReceiverId(lastEventId, receiverId)
                .orElseThrow(() -> new InvalidLastEventIdException(lastEventId.toString()));
    }

    // 단건 읽음 처리
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID receiverId) {
        Notification notification = notificationRepository
                .findByIdAndReceiverId(notificationId, receiverId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notification.markAsRead();
        log.info("Notification read: id={}, receiverId={}", notificationId, receiverId);
        return notificationMapper.toResponse(notification);
    }

    // 지원하는 정렬 값만 허용하고(아니면 400), 응답 메타데이터가 실제 정렬과 일치하도록 보장한다.
    private boolean resolveAscending(String sortBy, String sortDirection) {
        if (!SORT_BY_CREATED_AT.equals(sortBy)) {
            throw new InvalidSortByException(sortBy);
        }
        if (SORT_ASCENDING.equalsIgnoreCase(sortDirection)) {
            return true;
        }
        if (SORT_DESCENDING.equalsIgnoreCase(sortDirection)) {
            return false;
        }
        throw new InvalidSortDirectionException(sortDirection);
    }
}
