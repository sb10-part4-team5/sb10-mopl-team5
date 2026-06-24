package com.codeit.team5.mopl.auth.dto.response;

import com.codeit.team5.mopl.user.dto.response.UserResponse;

public record JwtResponse(
        UserResponse userDto,
        String accessToken
) {

}
