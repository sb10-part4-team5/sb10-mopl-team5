package com.codeit.team5.mopl.watcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.dto.payload.ContentChatPayload;
import com.codeit.team5.mopl.watcher.dto.request.ContentChatCreatedRequest;
import com.codeit.team5.mopl.watcher.mapper.payload.ContentChatPayloadMapper;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentChatServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentChatPayloadMapper payloadMapper;

    @InjectMocks
    private ContentChatService chatService;

    @Test
    @DisplayName("유효한 유저 이메일과 요청이 주어지면 Payload를 생성한다_성공")
    void createContentChatPayload_Success() {
        // Given
        String email = "test@test.com";
        ContentChatCreatedRequest request = new ContentChatCreatedRequest("Hello");
        User mockUser = mock(User.class);
        ContentChatPayload expectedPayload = new ContentChatPayload(null, "Hello");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
        given(payloadMapper.toDto(mockUser, request)).willReturn(expectedPayload);

        // When
        ContentChatPayload result = chatService.createContentChatPayload(email, request);

        // Then
        assertThat(result).isEqualTo(expectedPayload);
    }

    @Test
    @DisplayName("존재하지 않는 유저 이메일이 주어지면 예외가 발생한다_실패")
    void createContentChatPayload_UserNotFound() {
        // Given
        String email = "notfound@test.com";
        ContentChatCreatedRequest request = new ContentChatCreatedRequest("Hello");

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.createContentChatPayload(email, request))
                .isInstanceOf(UserNotFoundException.class);
    }
}
