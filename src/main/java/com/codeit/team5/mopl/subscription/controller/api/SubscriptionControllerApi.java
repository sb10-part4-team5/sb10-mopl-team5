package com.codeit.team5.mopl.subscription.controller.api;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "플레이리스트 관리")
public interface SubscriptionControllerApi {

    @Operation(summary = "플레이리스트 구독")
    ResponseEntity<Void> create(@PathVariable UUID playlistId, Principal principal);

    @Operation(summary = "플레이리스트 구독 취소")
    ResponseEntity<Void> delete(@PathVariable UUID playlistId, Principal principal);
}
