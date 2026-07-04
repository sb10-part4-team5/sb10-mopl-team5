package com.codeit.team5.mopl.subscription.controller.api;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "플레이리스트 구독 관리")
public interface SubscriptionControllerApi {

    @Operation(summary = "플레이리스트 구독")
    ResponseEntity<Void> create(@Parameter(hidden = true) MoplPrincipal principal,
            @PathVariable UUID playlistId);

    @Operation(summary = "플레이리스트 구독 취소")
    ResponseEntity<Void> delete(@Parameter(hidden = true) MoplPrincipal principal,
            @PathVariable UUID playlistId);
}
