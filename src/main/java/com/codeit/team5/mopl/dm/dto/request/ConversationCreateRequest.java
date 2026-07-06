package com.codeit.team5.mopl.dm.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConversationCreateRequest(
        @NotNull UUID withUserId
) {
}
