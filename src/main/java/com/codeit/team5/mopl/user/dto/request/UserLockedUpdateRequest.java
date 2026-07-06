package com.codeit.team5.mopl.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserLockedUpdateRequest(

        @NotNull
        Boolean locked
) {

}
