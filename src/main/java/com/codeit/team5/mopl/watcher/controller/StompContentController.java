package com.codeit.team5.mopl.watcher.controller;

import java.util.UUID;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import com.codeit.team5.mopl.auth.security.details.MoplPrincipal;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.watcher.dto.payload.ContentChatPayload;
import com.codeit.team5.mopl.watcher.dto.request.ContentChatCreatedRequest;
import com.codeit.team5.mopl.watcher.service.ContentChatService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class StompContentController {

    private final ContentChatService chatService;

    @SendTo(StompConstants.SUB_WATCHING_CONTENT_CHAT)
    @MessageMapping(StompConstants.PUB_WATCHING_CONTENT_CHAT)
    public ContentChatPayload sendChat(@AuthenticationPrincipal MoplPrincipal principal,
            @Payload ContentChatCreatedRequest request) {
        if (!StringUtils.hasText(request.content())) {
            return null;
        }
        UUID watcherId = principal.getId();
        return chatService.createContentChatPayload(watcherId, request);
    }
}
