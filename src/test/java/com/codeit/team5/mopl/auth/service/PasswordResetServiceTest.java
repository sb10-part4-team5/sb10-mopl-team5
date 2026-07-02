package com.codeit.team5.mopl.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.auth.dto.request.ResetPasswordRequest;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TemporaryPasswordService temporaryPasswordService;

    @Mock
    private MailService mailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    @DisplayName("비밀번호 초기화 요청 시 이메일을 정규화하고 임시 비밀번호를 전송한다")
    void resetPassword_success() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest("User@Example.COM");
        User user = User.create("user@example.com", "encoded-password", "사용자");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(temporaryPasswordService.issue(user)).thenReturn("Temp1234");

        // When
        passwordResetService.resetPassword(request);

        // Then
        verify(userRepository).findByEmail("user@example.com");
        verify(temporaryPasswordService).issue(user);
        verify(mailService).sendTemporaryPassword("user@example.com", "Temp1234");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 비밀번호 초기화 요청하면 실패한다")
    void resetPassword_userNotFound_throwsException() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest("Unknown@Example.COM");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findByEmail("unknown@example.com");
        verify(temporaryPasswordService, never()).issue(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(mailService);
    }
}
