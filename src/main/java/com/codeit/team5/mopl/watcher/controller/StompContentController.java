package com.codeit.team5.mopl.watcher.controller;

import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.watcher.dto.payload.ContentChatPayload;
import com.codeit.team5.mopl.watcher.dto.request.ContentChatCreatedRequest;
import com.codeit.team5.mopl.watcher.service.ContentChatService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

@Controller
@RequiredArgsConstructor
public class StompContentController {

    private final ContentChatService chatService;

    @SendTo(StompConstants.SUB_WATCHING_CONTENT_CHAT)
    @MessageMapping(StompConstants.PUB_WATCHING_CONTENT_CHAT)
    public ContentChatPayload sendChat(Principal principal,
            @Payload ContentChatCreatedRequest request) {
        if (!StringUtils.hasText(request.content())) {
            return null;
        }
        String email = principal.getName();
        return chatService.createContentChatPayload(email, request);
    }
}
