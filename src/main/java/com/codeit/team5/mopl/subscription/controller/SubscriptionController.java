package com.codeit.team5.mopl.subscription.controller;

import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.codeit.team5.mopl.subscription.controller.api.SubscriptionControllerApi;
import com.codeit.team5.mopl.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriptionController implements SubscriptionControllerApi {

    private final SubscriptionService service;

    @PostMapping("/playlists/{playlistId}/subscription")
    public ResponseEntity<Void> create(@AuthenticationPrincipal MoplPrincipal principal, @PathVariable UUID playlistId) {
        service.create(playlistId, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/playlists/{playlistId}/subscription")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal MoplPrincipal principal, @PathVariable UUID playlistId) {
        service.delete(playlistId, principal.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
