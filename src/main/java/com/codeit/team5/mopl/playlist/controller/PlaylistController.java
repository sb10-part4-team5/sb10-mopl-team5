package com.codeit.team5.mopl.playlist.controller;

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
import com.codeit.team5.mopl.playlist.controller.api.PlaylistControllerApi;
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
public class PlaylistController implements PlaylistControllerApi {

    private final PlaylistService service;
    private final PlaylistMapper mapper;

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<PlaylistResponse> find(@AuthenticationPrincipal MoplPrincipal principal,
            @PathVariable UUID id) {
        UUID userId = principal.getId();
        return ResponseEntity.status(HttpStatus.OK).body(service.find(id, userId));
    }

    @Override
    @GetMapping
    public ResponseEntity<CursorResponse<PlaylistResponse>> findCursor(
            @AuthenticationPrincipal MoplPrincipal principal,
            @ModelAttribute @Valid PlaylistCursorRequest request) {
        UUID userId = principal.getId();
        return ResponseEntity.status(HttpStatus.OK)
                .body(service.findByCursor(mapper.toCommand(request), userId));
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlaylistResponse> create(@AuthenticationPrincipal MoplPrincipal principal,
            @RequestBody @Valid PlaylistCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(principal.getId(), request));
    }

    @Override
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlaylistResponse> update(@AuthenticationPrincipal MoplPrincipal principal,
            @PathVariable UUID id, @RequestBody PlaylistUpdateRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(service.update(id, principal.getId(), request));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal MoplPrincipal principal,
            @PathVariable UUID id) {
        service.delete(id, principal.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    @PostMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> addContent(@AuthenticationPrincipal MoplPrincipal principal,
            @PathVariable UUID playlistId, @PathVariable UUID contentId) {
        service.addContent(principal.getId(), playlistId, contentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    @DeleteMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> removeContent(@AuthenticationPrincipal MoplPrincipal principal,
            @PathVariable UUID playlistId, @PathVariable UUID contentId) {
        service.removeContent(principal.getId(), playlistId, contentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
