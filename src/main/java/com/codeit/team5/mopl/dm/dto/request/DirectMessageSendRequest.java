package com.codeit.team5.mopl.dm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DirectMessageSendRequest(
        @NotBlank @Size(max = 1000) String content
) {
}
