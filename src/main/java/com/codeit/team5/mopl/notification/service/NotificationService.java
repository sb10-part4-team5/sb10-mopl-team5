package com.codeit.team5.mopl.notification.service;

import com.codeit.team5.mopl.notification.dto.CursorResponseNotificationDto;
import com.codeit.team5.mopl.notification.dto.NotificationResponse;
import com.codeit.team5.mopl.notification.entity.Notification;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.event.NotificationCreatedEvent;
import com.codeit.team5.mopl.notification.exception.NotificationNotFoundException;
import com.codeit.team5.mopl.notification.mapper.NotificationMapper;
import com.codeit.team5.mopl.notification.repository.NotificationRepository;
import java.time.Instant;
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
        NotificationResponse response = notificationMapper.toResponse(saved);
        log.info("알림 생성됨: id={}, receiverId={}, type={}",
                saved.getId(), receiverId, type);
        publisher.publishEvent(new NotificationCreatedEvent(response));
        return response;
    }

    // 수신자별 알림 목록 (커서 페이지네이션)
    public CursorResponseNotificationDto getNotifications(
            UUID receiverId, String cursor, UUID idAfter, int limit,
            String sortDirection, String sortBy) {
        Instant cursorInstant =
                (cursor == null || cursor.isBlank()) ? null : Instant.parse(cursor);
        boolean ascending = isAscending(sortDirection);

        // hasNext 판단을 위해 limit + 1 개를 조회
        Limit fetchLimit = Limit.of(limit + 1);
        List<Notification> rows = ascending
                ? notificationRepository.findPageByReceiverAsc(receiverId, cursorInstant, idAfter, fetchLimit)
                : notificationRepository.findPageByReceiverDesc(receiverId, cursorInstant, idAfter, fetchLimit);

        boolean hasNext = rows.size() > limit;
        List<Notification> page = hasNext ? rows.subList(0, limit) : rows;

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !page.isEmpty()) {
            Notification last = page.get(page.size() - 1);
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId();
        }

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

    private boolean isAscending(String sortDirection) {
        return sortDirection != null && sortDirection.toUpperCase().startsWith("ASC");
    }
}
