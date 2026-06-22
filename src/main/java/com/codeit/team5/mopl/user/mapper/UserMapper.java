package com.codeit.team5.mopl.user.mapper;

import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserMapper {
    @Mapping(target = "role", source = "user.role")
    UserResponse toDto(User user);

    default User toEntity(UserRegisterRequest request) {
        return User.create(request.email(), request.password(), request.name());
    }
}
