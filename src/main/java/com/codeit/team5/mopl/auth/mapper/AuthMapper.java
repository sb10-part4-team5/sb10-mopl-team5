package com.codeit.team5.mopl.auth.mapper;

import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.auth.service.model.AuthPayload;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface AuthMapper {

    JwtResponse toJwtResponse(UserResponse userDto, String accessToken);

    AuthPayload toAuthPayload(JwtResponse jwtResponse, String refreshToken);
}
