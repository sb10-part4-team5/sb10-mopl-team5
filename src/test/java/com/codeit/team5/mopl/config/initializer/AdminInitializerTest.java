package com.codeit.team5.mopl.config.initializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.config.properties.AdminProperties;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.entity.UserRole;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String RAW_PASSWORD = "admin-password";
    private static final String ENCODED_PASSWORD = "encoded-admin-password";
    private static final String ADMIN_NAME = "관리자";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final AdminProperties adminProperties = new AdminProperties(
            ADMIN_EMAIL,
            RAW_PASSWORD,
            ADMIN_NAME
    );

    @Test
    @DisplayName("관리자 계정이 없으면 초기 관리자 계정을 생성한다")
    void run_missingAdmin_createsAdmin() {
        // Given
        AdminInitializer adminInitializer = new AdminInitializer(
                userRepository,
                passwordEncoder,
                adminProperties
        );
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        // When
        adminInitializer.run(null);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verify(passwordEncoder).encode(RAW_PASSWORD);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(ADMIN_EMAIL);
        assertThat(savedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(savedUser.getName()).isEqualTo(ADMIN_NAME);
        assertThat(savedUser.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("관리자 계정이 이미 ADMIN이면 초기화를 건너뛴다")
    void run_existingAdmin_skipsInitialization() {
        // Given
        AdminInitializer adminInitializer = new AdminInitializer(
                userRepository,
                passwordEncoder,
                adminProperties
        );
        User existingAdmin = User.create(ADMIN_EMAIL, ENCODED_PASSWORD, ADMIN_NAME);
        existingAdmin.updateRole(UserRole.ADMIN);
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existingAdmin));

        // When
        adminInitializer.run(null);

        // Then
        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verify(passwordEncoder, never()).encode(RAW_PASSWORD);
        verify(userRepository, never()).save(existingAdmin);
        assertThat(existingAdmin.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("관리자 이메일 계정이 USER이면 ADMIN으로 복구한다")
    void run_existingUser_recoversAdminRole() {
        // Given
        AdminInitializer adminInitializer = new AdminInitializer(
                userRepository,
                passwordEncoder,
                adminProperties
        );
        User existingUser = User.create(ADMIN_EMAIL, ENCODED_PASSWORD, ADMIN_NAME);
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existingUser));

        // When
        adminInitializer.run(null);

        // Then
        verify(userRepository).findByEmail(ADMIN_EMAIL);
        verify(passwordEncoder, never()).encode(RAW_PASSWORD);
        verify(userRepository).save(existingUser);
        assertThat(existingUser.getRole()).isEqualTo(UserRole.ADMIN);
    }
}
