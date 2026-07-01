package com.codeit.team5.mopl.dm.controller;

import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.dm.controller.api.DirectMessageApi;
import com.codeit.team5.mopl.dm.dto.request.DirectMessageCursorRequest;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.service.DirectMessageService;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations/{conversationId}/direct-messages")
@RequiredArgsConstructor
@Slf4j
public class DirectMessageController implements DirectMessageApi {

    private final DirectMessageService directMessageService;

    @Override
    @GetMapping
    public ResponseEntity<CursorResponse<DirectMessageResponse>> getDirectMessages(
            @AuthenticationPrincipal MoplUserDetails userDetails,
            @PathVariable UUID conversationId,
            @Valid DirectMessageCursorRequest request) {
        log.info("Request API: GET /api/conversations/{}/direct-messages", conversationId);
        return ResponseEntity.ok(directMessageService.getMessages(userDetails.getId(), conversationId, request));
    }

    @Override
    @PostMapping("/{directMessageId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal MoplUserDetails userDetails,
            @PathVariable UUID conversationId,
            @PathVariable UUID directMessageId) {
        log.info("Request API: POST /api/conversations/{}/direct-messages/{}/read", conversationId, directMessageId);
        directMessageService.markMessagesAsRead(userDetails.getId(), conversationId, directMessageId);
        return ResponseEntity.ok().build();
    }
}
