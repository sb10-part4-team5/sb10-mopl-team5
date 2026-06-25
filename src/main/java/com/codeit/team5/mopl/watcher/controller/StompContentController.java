package com.codeit.team5.mopl.watcher.controller;

import com.codeit.team5.mopl.watcher.dto.payload.ContentChatPayload;
import com.codeit.team5.mopl.watcher.dto.request.ContentChatCreatedRequest;
import com.codeit.team5.mopl.watcher.service.ContentChatService;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class StompContentController {

    private final ContentChatService chatService;

    @SendTo("/sub/contents/{contentId}/chat")
    @MessageMapping("/contents/{contentId}/chat")
    public ContentChatPayload sendChat(Principal principal, ContentChatCreatedRequest request) {
        UUID userId = UUID.fromString(principal.getName());
        return chatService.createContentChatPayload(userId, request);
    }
}
