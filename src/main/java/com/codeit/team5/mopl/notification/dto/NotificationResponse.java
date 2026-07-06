package com.codeit.team5.mopl.notification.dto;

import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Schema(name="NotificationDto")
public record NotificationResponse(
    @NotNull UUID id,
    @NotNull Instant createdAt,
    @NotNull UUID receiverId,
    String title,
    String content,
    @NotNull NotificationLevel level
) {

}
