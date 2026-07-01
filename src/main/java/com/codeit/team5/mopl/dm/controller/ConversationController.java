package com.codeit.team5.mopl.dm.controller;

import com.codeit.team5.mopl.auth.security.details.MoplUserDetails;
import com.codeit.team5.mopl.dm.controller.api.ConversationApi;
import com.codeit.team5.mopl.dm.dto.request.ConversationCreateRequest;
import com.codeit.team5.mopl.dm.dto.request.ConversationCursorRequest;
import com.codeit.team5.mopl.dm.dto.response.ConversationResponse;
import com.codeit.team5.mopl.dm.service.ConversationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController implements ConversationApi {

    private final ConversationService conversationService;

    @Override
    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(
            @AuthenticationPrincipal MoplUserDetails userDetails,
            @Valid @RequestBody ConversationCreateRequest request) {
        log.info("Conversation create request: POST /api/conversations, withUser={}", request.withUserId());
        ConversationResponse response = conversationService.getOrCreateConversation(
                userDetails.getId(), request.withUserId());
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<CursorResponse<ConversationResponse>> getMyConversations(
            @AuthenticationPrincipal MoplUserDetails userDetails,
            @Valid ConversationCursorRequest request) {
        log.info("Request API: GET /api/conversations, userId={}", userDetails.getId());
        return ResponseEntity.ok(conversationService.findMyConversations(userDetails.getId(), request));
    }

    @Override
    @GetMapping("/with")
    public ResponseEntity<ConversationResponse> getConversationWith(
            @AuthenticationPrincipal MoplUserDetails userDetails,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(conversationService.getConversationWith(userDetails.getId(), userId));
    }

    @Override
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @AuthenticationPrincipal MoplUserDetails userDetails,
            @PathVariable UUID conversationId) {
        return ResponseEntity.ok(conversationService.getConversation(userDetails.getId(), conversationId));
    }
}
