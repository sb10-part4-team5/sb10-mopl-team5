package com.codeit.team5.mopl.subscription.controller;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.codeit.team5.mopl.subscription.controller.api.SubscriptionControllerApi;
import com.codeit.team5.mopl.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriptionController implements SubscriptionControllerApi {

    private final SubscriptionService service;

    @PostMapping("/playlists/{playlistId}/subscription")
    public ResponseEntity<Void> create(@PathVariable UUID playlistId, Principal principal) {
        service.create(playlistId, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/playlists/{playlistId}/subscription")
    public ResponseEntity<Void> delete(@PathVariable UUID playlistId, Principal principal) {
        service.delete(playlistId, principal.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
