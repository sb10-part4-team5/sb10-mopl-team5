package com.codeit.team5.mopl.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.DuplicatedEmailException;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
    @DisplayName("사용자 생성 성공")
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
    @DisplayName("중복 이메일 사용자 생성 실패")
    void createUser_duplicateEmail_throwsException() {
        // Given
        UserRegisterRequest request =
                new UserRegisterRequest("사용자", "duplicate@example.com", "password1");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicatedEmailException.class)
                .satisfies(exception -> assertThat(
                        ((DuplicatedEmailException) exception).getStatus()
                ).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    @DisplayName("사용자 상세 조회 성공")
    void getById_success() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = User.create("user@example.com", "encoded-password", "사용자");
        UserResponse expectedResponse = new UserResponse(
                userId,
                Instant.parse("2026-06-22T00:00:00Z"),
                "user@example.com",
                "사용자",
                null,
                "USER",
                false
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expectedResponse);

        // When
        UserResponse result = userService.getById(userId);

        // Then
        assertThat(result).isSameAs(expectedResponse);
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 실패")
    void getById_notFound_throwsException() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getById(userId))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(userMapper);
    }
}
