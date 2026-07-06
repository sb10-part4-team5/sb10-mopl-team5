package com.codeit.team5.mopl.auth.mapper;

import com.codeit.team5.mopl.auth.security.details.AuthUser;
import com.codeit.team5.mopl.auth.security.details.PasswordAuthUser;
import com.codeit.team5.mopl.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuthUserMapper {

    AuthUser toAuthUser(User user);

    @Mapping(target = "authUser", source = ".")
    @Mapping(target = "password", source = "password")
    PasswordAuthUser toAuthUserWithPassword(User user);
}
