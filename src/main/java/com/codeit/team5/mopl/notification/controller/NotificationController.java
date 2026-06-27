package com.codeit.team5.mopl.notification.controller;

import com.codeit.team5.mopl.notification.controller.api.NotificationApi;
import com.codeit.team5.mopl.notification.dto.CursorResponseNotificationDto;
import com.codeit.team5.mopl.notification.service.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/notifications")
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;

    @Override
    @GetMapping
    public ResponseEntity<CursorResponseNotificationDto> getNotifications(
            // TODO: 인증 적용 후 @AuthenticationPrincipal MoplUserDetails 에서 receiverId 추출로 교체
            @RequestParam UUID receiverId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "DESCENDING") String sortDirection,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        log.info("알림 목록 요청 : GET /api/notifications, receiverId={}", receiverId);

        CursorResponseNotificationDto response = notificationService.getNotifications(
                receiverId, cursor, idAfter, limit, sortDirection, sortBy);

        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> readNotification(
            // TODO: 인증 적용 후 @AuthenticationPrincipal MoplUserDetails 에서 receiverId 추출로 교체
            @RequestParam UUID receiverId,
            @PathVariable UUID notificationId) {
        log.info("Notification read request: DELETE /api/notifications/{}, receiverId={}",
                notificationId, receiverId);

        notificationService.markAsRead(notificationId, receiverId);

        return ResponseEntity.noContent().build();
    }
}
