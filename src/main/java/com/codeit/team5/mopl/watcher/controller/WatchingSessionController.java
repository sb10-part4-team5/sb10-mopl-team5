package com.codeit.team5.mopl.watcher.controller;

import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.watcher.controller.api.WatchingSessionRestApi;
import com.codeit.team5.mopl.watcher.dto.request.WatchingSessionCursorRequest;
import com.codeit.team5.mopl.watcher.dto.response.WatchingSessionResponse;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;
import com.codeit.team5.mopl.watcher.service.WatchingSessionQueryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
class WatchingSessionController implements WatchingSessionRestApi {

    private final WatchingSessionQueryService service;

    @Override
    @GetMapping("/users/{watcherId}/watching-sessions")
    public ResponseEntity<WatchingSessionResponse> findWatchingSessionsByWatcher(
            @PathVariable UUID watcherId) {
        return ResponseEntity.status(HttpStatus.OK).body(service.findByWatcherId(watcherId));
    }

    @Override
    @GetMapping("/contents/{contentId}/watching-sessions")
    public ResponseEntity<CursorResponse<WatchingSessionResponse>> findWatchingSessionsByContent(
            @PathVariable UUID contentId,
            @ModelAttribute @Valid WatchingSessionCursorRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(service.findCursorByContentId(contentId, request));
    }
}
