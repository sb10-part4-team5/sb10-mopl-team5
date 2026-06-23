package com.codeit.team5.mopl.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.global.exception.ErrorCode;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.DuplicatedEmailException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자 생성에 성공한다")
    void createUser_success() {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "User@Example.COM", "password1");
        String normalizedEmail = "user@example.com";
        UserResponse expectedResponse = new UserResponse(
                UUID.randomUUID(),
                Instant.parse("2026-06-22T00:00:00Z"),
                normalizedEmail,
                request.name(),
                null,
                "USER",
                false
        );
        String encodedPassword = "encoded-password";

        when(userRepository.existsByEmail(normalizedEmail)).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).then(returnsFirstArg());
        when(userMapper.toDto(any(User.class))).thenReturn(expectedResponse);

        // When
        UserResponse result = userService.create(request);

        // Then
        assertThat(result).isSameAs(expectedResponse);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).existsByEmail(normalizedEmail);
        verify(passwordEncoder).encode(request.password());
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        verify(userMapper).toDto(savedUser);
        assertThat(savedUser.getEmail()).isEqualTo(normalizedEmail);
        assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedUser.getName()).isEqualTo(request.name());
        assertThat(savedUser.getRole().name()).isEqualTo("USER");
        assertThat(savedUser.isLocked()).isFalse();
    }

    @Test
    @DisplayName("중복 이메일이면 사용자 생성에 실패한다")
    void createUser_duplicateEmail_throwsException() {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "duplicate@example.com", "password1");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicatedEmailException.class)
                .satisfies(exception -> assertThat(
                        ((DuplicatedEmailException) exception).getErrorCode()
                ).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(userMapper, passwordEncoder);
    }
}
