package com.codeit.team5.mopl.dm.controller;

import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.dm.controller.api.ConversationApi;
import com.codeit.team5.mopl.dm.dto.request.ConversationCreateRequest;
import com.codeit.team5.mopl.dm.dto.response.ConversationResponse;
import com.codeit.team5.mopl.dm.service.DmService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController implements ConversationApi {

    private final DmService dmService;

    @Override
    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @AuthenticationPrincipal MoplUserDetails userDetails,
            @Valid @RequestBody ConversationCreateRequest request) {
        log.info("Conversation create request: POST /api/conversations, withUser={}", request.withUserId());

        ConversationResponse response = dmService.getOrCreateConversation(userDetails.getId(), request.withUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/with")
    public ResponseEntity<ConversationResponse> getConversationWith(
            @AuthenticationPrincipal MoplUserDetails userDetails,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(dmService.getConversationWith(userDetails.getId(), userId));
    }

    @Override
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @AuthenticationPrincipal MoplUserDetails userDetails,
            @PathVariable UUID conversationId) {
        return ResponseEntity.ok(dmService.getConversation(userDetails.getId(), conversationId));
    }

    @Override
    @PostMapping("/{conversationId}/direct-messages/{directMessageId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal MoplUserDetails userDetails,
            @PathVariable UUID conversationId,
            @PathVariable UUID directMessageId) {
        dmService.markMessagesAsRead(userDetails.getId(), conversationId, directMessageId);
        return ResponseEntity.ok().build();
    }
}
