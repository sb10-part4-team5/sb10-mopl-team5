package com.codeit.team5.mopl.playlist.controller;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistCreateRequest;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistCursorRequest;
import com.codeit.team5.mopl.playlist.dto.request.PlaylistUpdateRequest;
import com.codeit.team5.mopl.playlist.dto.response.PlaylistResponse;
import com.codeit.team5.mopl.playlist.mapper.PlaylistMapper;
import com.codeit.team5.mopl.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService service;
    private final PlaylistMapper mapper;

    @GetMapping("/{id}")
    public ResponseEntity<PlaylistResponse> find(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK).body(service.find(id));
    }

    @GetMapping
    public ResponseEntity<CursorResponse<PlaylistResponse>> findCursor(
            @ModelAttribute @Valid PlaylistCursorRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(service.findByCursor(mapper.toCommand(request)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlaylistResponse> create(Principal principal,
            @RequestBody PlaylistCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(principal.getName(), request));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlaylistResponse> update(Principal principal, @PathVariable UUID id,
            @RequestBody PlaylistUpdateRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(service.update(id, principal.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable UUID id) {
        service.delete(id, principal.getName());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> addContent(Principal principal,
            @PathVariable UUID playlistId, @PathVariable UUID contentId) {
        service.addContent(principal.getName(), playlistId, contentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> removeContent(Principal principal,
            @PathVariable UUID playlistId, @PathVariable UUID contentId) {
        service.removeContent(principal.getName(), playlistId, contentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
