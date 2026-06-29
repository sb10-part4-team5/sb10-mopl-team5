package com.codeit.team5.mopl.notification.service;

import com.codeit.team5.mopl.notification.dto.CursorResponseNotificationDto;
import com.codeit.team5.mopl.notification.dto.NotificationPayload;
import com.codeit.team5.mopl.notification.dto.NotificationResponse;
import com.codeit.team5.mopl.notification.entity.Notification;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.event.NotificationCreatedEvent;
import com.codeit.team5.mopl.notification.exception.InvalidCursorException;
import com.codeit.team5.mopl.notification.exception.InvalidSortByException;
import com.codeit.team5.mopl.notification.exception.InvalidSortDirectionException;
import com.codeit.team5.mopl.notification.exception.NotificationNotFoundException;
import com.codeit.team5.mopl.notification.mapper.NotificationMapper;
import com.codeit.team5.mopl.notification.repository.NotificationRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
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
    @Transactional
    public NotificationResponse create(
            UUID receiverId, NotificationType type, String title, String content,
            NotificationLevel level) {
        Notification notification = Notification.create(receiverId, type, title, content, level);
        Notification saved = notificationRepository.save(notification);
        NotificationPayload payload = notificationMapper.toPayload(saved); // Notification을 내부에서 사용 될 Payload로 변환
        NotificationResponse response = notificationMapper.toResponse(saved); // Notification을 ResponseDto로 변환
        log.info("알림 생성됨: type={}", type);
        publisher.publishEvent(new NotificationCreatedEvent(payload));
        return response;
    }

    // 수신자별 알림 목록 조회 (커서 페이지네이션)
    public CursorResponseNotificationDto getNotifications(
            UUID receiverId, String cursor, UUID idAfter, int limit,
            String sortDirection, String sortBy) {

        // 지원하는 정렬 값인지 검증하고 오름차순 여부를 결정 (응답 메타와 실제 정렬을 일치시킴)
        boolean ascending = resolveAscending(sortBy, sortDirection);

        // 주 커서는 createdAt 문자열로 들어오므로 Instant로 파싱. 없으면(null) 첫 페이지
        Instant cursorInstant;
        try{
            cursorInstant = (cursor == null || cursor.isBlank()) ? null : Instant.parse(cursor);
        } catch (DateTimeParseException e){
            throw new InvalidCursorException(cursor);
        }

        // 다음 페이지 존재 여부를 알기 위해 limit보다 1개 더 조회
        Limit fetchLimit = Limit.of(limit + 1);

        // 정렬 방향에 맞는 커서 조회 메서드 선택
        List<Notification> rows = ascending
                ? notificationRepository.findPageByReceiverAsc(receiverId, cursorInstant, idAfter, fetchLimit)
                : notificationRepository.findPageByReceiverDesc(receiverId, cursorInstant, idAfter, fetchLimit);

        // limit + 1개가 조회됐으면 다음 페이지가 있는 것. 초과분은 잘라내고 limit개만 노출
        boolean hasNext = rows.size() > limit;
        List<Notification> page = hasNext ? rows.subList(0, limit) : rows;

        // 다음 페이지가 있을 때만 마지막 요소의 (createdAt, id)로 다음 커서를 구성
        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !page.isEmpty()) {
            Notification last = page.get(page.size() - 1);
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId();
        }

        // 응답의 totalCount (수신자의 전체 알림 개수)
        long totalCount = notificationRepository.countByReceiverId(receiverId);

        return new CursorResponseNotificationDto(
                notificationMapper.toResponseList(page),
                nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection);
    }

    // 안 읽은 알림 개수
    public long countUnread(UUID receiverId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(receiverId);
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
