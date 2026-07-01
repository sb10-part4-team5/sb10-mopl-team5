package com.codeit.team5.mopl.subscription.controller;

import com.codeit.team5.mopl.subscription.service.SubscriptionService;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService service;

    @PostMapping("/playlists/{playlistId}/subscription")
    public ResponseEntity<Void> create(@PathVariable UUID playlistId, Principal principal) {
        service.create(playlistId, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
