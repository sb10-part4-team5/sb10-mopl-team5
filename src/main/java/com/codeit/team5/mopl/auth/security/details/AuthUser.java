package com.codeit.team5.mopl.auth.security.details;

import java.util.UUID;

public record AuthUser(
        UUID id,
        String email,
        String role,
        boolean locked
) {
}
