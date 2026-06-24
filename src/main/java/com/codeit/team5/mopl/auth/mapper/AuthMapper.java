package com.codeit.team5.mopl.auth.mapper;

import com.codeit.team5.mopl.auth.dto.response.JwtResponse;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface AuthMapper {

    JwtResponse toDto(UserResponse userDto, String accessToken);
}
