package com.codeit.team5.mopl.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.auth.entity.TemporaryPassword;
import com.codeit.team5.mopl.auth.repository.TemporaryPasswordRepository;
import com.codeit.team5.mopl.auth.support.TemporaryPasswordGenerator;
import com.codeit.team5.mopl.user.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TemporaryPasswordServiceTest {

    @Mock
    private TemporaryPasswordRepository temporaryPasswordRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TemporaryPasswordGenerator temporaryPasswordGenerator;

    @InjectMocks
    private TemporaryPasswordService temporaryPasswordService;

    @Test
    @DisplayName("임시 비밀번호 발급에 성공하면 해시를 저장하고 원문 비밀번호를 반환한다")
    void issue_success() {
        // Given
        User user = userWithId(UUID.randomUUID());
        when(temporaryPasswordGenerator.generate()).thenReturn("Temp1234");
        when(passwordEncoder.encode("Temp1234")).thenReturn("encoded-temp-password");
        when(temporaryPasswordRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        // When
        String result = temporaryPasswordService.issue(user);

        // Then
        assertThat(result).isEqualTo("Temp1234");

        ArgumentCaptor<TemporaryPassword> captor = ArgumentCaptor.forClass(TemporaryPassword.class);
        verify(temporaryPasswordRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-temp-password");
        assertThat(captor.getValue().getExpiresAt())
                .isAfter(Instant.now().plus(2, ChronoUnit.MINUTES))
                .isBefore(Instant.now().plus(4, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("기존 임시 비밀번호가 있으면 새 해시와 만료시간으로 재발급한다")
    void issue_existingTemporaryPassword_success() {
        // Given
        User user = userWithId(UUID.randomUUID());
        TemporaryPassword existing = TemporaryPassword.create(
                user,
                "old-hash",
                Instant.now().minus(10, ChronoUnit.MINUTES)
        );
        when(temporaryPasswordGenerator.generate()).thenReturn("NewTemp1234");
        when(passwordEncoder.encode("NewTemp1234")).thenReturn("new-hash");
        when(temporaryPasswordRepository.findByUserId(user.getId())).thenReturn(Optional.of(existing));

        // When
        String result = temporaryPasswordService.issue(user);

        // Then
        assertThat(result).isEqualTo("NewTemp1234");
        assertThat(existing.getPasswordHash()).isEqualTo("new-hash");
        assertThat(existing.isValidAt(Instant.now())).isTrue();
        verify(temporaryPasswordRepository).save(existing);
    }

    @Test
    @DisplayName("유효한 임시 비밀번호가 일치하면 인증에 성공한다")
    void matches_success() {
        // Given
        UUID userId = UUID.randomUUID();
        TemporaryPassword temporaryPassword = TemporaryPassword.create(
                userWithId(userId),
                "encoded-temp-password",
                Instant.now()
        );
        when(temporaryPasswordRepository.findByUserId(userId)).thenReturn(Optional.of(temporaryPassword));
        when(passwordEncoder.matches("Temp1234", "encoded-temp-password")).thenReturn(true);

        // When
        boolean result = temporaryPasswordService.matches(userId, "Temp1234");

        // Then
        assertThat(result).isTrue();
        verify(passwordEncoder).matches("Temp1234", "encoded-temp-password");
    }

    @Test
    @DisplayName("임시 비밀번호가 일치하지 않으면 인증에 실패한다")
    void matches_mismatch_returnsFalse() {
        // Given
        UUID userId = UUID.randomUUID();
        TemporaryPassword temporaryPassword = TemporaryPassword.create(
                userWithId(userId),
                "encoded-temp-password",
                Instant.now()
        );
        when(temporaryPasswordRepository.findByUserId(userId)).thenReturn(Optional.of(temporaryPassword));
        when(passwordEncoder.matches("Wrong1234", "encoded-temp-password")).thenReturn(false);

        // When
        boolean result = temporaryPasswordService.matches(userId, "Wrong1234");

        // Then
        assertThat(result).isFalse();
        verify(passwordEncoder).matches("Wrong1234", "encoded-temp-password");
    }

    @Test
    @DisplayName("만료된 임시 비밀번호면 인증에 실패한다")
    void matches_expired_returnsFalse() {
        // Given
        UUID userId = UUID.randomUUID();
        TemporaryPassword temporaryPassword = TemporaryPassword.create(
                userWithId(userId),
                "encoded-temp-password",
                Instant.now().minus(4, ChronoUnit.MINUTES)
        );
        when(temporaryPasswordRepository.findByUserId(userId)).thenReturn(Optional.of(temporaryPassword));

        // When
        boolean result = temporaryPasswordService.matches(userId, "Temp1234");

        // Then
        assertThat(result).isFalse();
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("비어 있는 임시 비밀번호면 저장소 조회 없이 인증에 실패한다")
    void matches_blankPassword_returnsFalse() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        boolean result = temporaryPasswordService.matches(userId, "   ");

        // Then
        assertThat(result).isFalse();
        verify(temporaryPasswordRepository, never()).findByUserId(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("사용자 식별자로 임시 비밀번호 삭제에 성공한다")
    void deleteByUserId_success() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        temporaryPasswordService.deleteByUserId(userId);

        // Then
        verify(temporaryPasswordRepository).deleteByUserId(userId);
    }

    private User userWithId(UUID userId) {
        User user = User.create("user-" + userId + "@example.com", "encoded-password", "사용자");
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
