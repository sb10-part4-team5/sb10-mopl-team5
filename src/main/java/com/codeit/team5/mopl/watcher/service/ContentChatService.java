package com.codeit.team5.mopl.watcher.service;

import com.codeit.team5.mopl.global.logging.log.ExecutionTracer;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.dto.payload.ContentChatPayload;
import com.codeit.team5.mopl.watcher.dto.request.ContentChatCreatedRequest;
import com.codeit.team5.mopl.watcher.exception.ContentChatUserNotFoundException;
import com.codeit.team5.mopl.watcher.mapper.payload.ContentChatPayloadMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@ExecutionTracer
public class ContentChatService {

    private final UserRepository userRepository;
    private final ContentChatPayloadMapper payloadMapper;

    public ContentChatPayload createContentChatPayload(UUID watcherId,
            ContentChatCreatedRequest request) {
        User user = userRepository.findWithProfileImageById(watcherId)
                .orElseThrow(() -> new ContentChatUserNotFoundException(watcherId));
        log.debug("Content chat message created: watcherId={}", watcherId);
        return payloadMapper.toDto(user, request);
    }
}
