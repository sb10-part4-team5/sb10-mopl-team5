package com.codeit.team5.mopl.user.dto.request;

import com.codeit.team5.mopl.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(

        @NotNull
        UserRole role
) {

}
