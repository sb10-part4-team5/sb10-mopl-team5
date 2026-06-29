package com.codeit.team5.mopl.watcher.service;

import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.dto.payload.ContentChatPayload;
import com.codeit.team5.mopl.watcher.dto.request.ContentChatCreatedRequest;
import com.codeit.team5.mopl.watcher.mapper.payload.ContentChatPayloadMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ContentChatService {

    private final UserRepository userRepository;
    private final ContentChatPayloadMapper payloadMapper;

    public ContentChatPayload createContentChatPayload(String email,
            ContentChatCreatedRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return payloadMapper.toDto(user, request);
    }
}
