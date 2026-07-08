package com.codeit.team5.mopl.notification.controller;

import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.notification.controller.api.NotificationApi;
import com.codeit.team5.mopl.notification.dto.CursorResponseNotificationDto;
import com.codeit.team5.mopl.notification.dto.request.NotificationListQuery;
import com.codeit.team5.mopl.notification.service.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
@Validated
@Slf4j
@RequestMapping("/api/notifications")
public class NotificationController implements NotificationApi {

    private final NotificationService notificationService;

    @Override
    @GetMapping
    public ResponseEntity<CursorResponseNotificationDto> getNotifications(

        @AuthenticationPrincipal MoplUserDetails userDetails,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "DESCENDING") String sortDirection,
        @RequestParam(defaultValue = "createdAt") String sortBy) {

        UUID receiverId = userDetails.getId();

        log.info("알림 목록 요청 : GET /api/notifications");
        log.debug("알림 목록 요청 : GET /api/notifications, receiverId={}", receiverId);

        CursorResponseNotificationDto response = notificationService.getNotifications(
                new NotificationListQuery(receiverId, cursor, idAfter, limit, sortDirection, sortBy));

        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> readNotification(
        @AuthenticationPrincipal MoplUserDetails userDetails,
        @PathVariable UUID notificationId) {
        UUID receiverId = userDetails.getId();
        log.info("Notification read request: DELETE /api/notifications/{}", notificationId);
        log.debug("Notification read request: DELETE /api/notifications/{}, receiverId={}",
            notificationId, receiverId);


        notificationService.markAsRead(notificationId, receiverId);

        return ResponseEntity.noContent().build();
    }
}
