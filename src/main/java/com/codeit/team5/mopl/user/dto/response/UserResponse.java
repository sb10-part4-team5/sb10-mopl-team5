package com.codeit.team5.mopl.user.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        Instant createdAt,
        String email,
        String name,
        String profileImageUrl,
        String role,
        boolean locked
) {

}
