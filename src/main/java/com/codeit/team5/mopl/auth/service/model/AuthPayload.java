package com.codeit.team5.mopl.auth.service.model;

import com.codeit.team5.mopl.auth.dto.response.JwtResponse;

public record AuthPayload(
        JwtResponse jwtResponse,
        String refreshToken
) {
}
