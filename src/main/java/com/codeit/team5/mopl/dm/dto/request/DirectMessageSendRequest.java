package com.codeit.team5.mopl.dm.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DirectMessageSendRequest(
        @NotBlank String content
) {
}
