package com.codeit.team5.mopl.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.config.JpaAuditingConfig;
import com.codeit.team5.mopl.global.support.config.QueryDslTestConfig;
import com.codeit.team5.mopl.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import({JpaAuditingConfig.class, TestcontainersConfiguration.class, QueryDslTestConfig.class})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("사용자 저장에 성공한다")
    void saveUser_success() {
        // Given
        User user = User.create("save@example.com", "password", "저장 사용자");

        // When
        User savedUser = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // Then
        User foundUser = userRepository.findById(savedUser.getId()).orElseThrow();
        assertThat(foundUser.getId()).isNotNull();
        assertThat(foundUser.getEmail()).isEqualTo("save@example.com");
        assertThat(foundUser.getPassword()).isEqualTo("password");
        assertThat(foundUser.getName()).isEqualTo("저장 사용자");
        assertThat(foundUser.getRole()).isEqualTo(user.getRole());
        assertThat(foundUser.isLocked()).isFalse();
        assertThat(foundUser.getCreatedAt()).isNotNull();
        assertThat(foundUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("식별자로 사용자를 조회한다")
    void findById_success() {
        // Given
        User user = User.create("find@example.com", "password", "조회 사용자");
        User savedUser = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<User> result = userRepository.findById(savedUser.getId());

        // Then
        assertThat(result)
                .isPresent()
                .get()
                .extracting(User::getEmail, User::getName)
                .containsExactly("find@example.com", "조회 사용자");
    }

    @Test
    @DisplayName("존재하지 않는 식별자로 사용자를 조회하면 빈 결과를 반환한다")
    void findById_notFound() {
        // Given
        UUID nonexistentId = UUID.randomUUID();

        // When
        Optional<User> result = userRepository.findById(nonexistentId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자를 삭제한다")
    void delete_success() {
        // Given
        User user = User.create("delete@example.com", "password", "삭제 사용자");
        User savedUser = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // When
        userRepository.deleteById(savedUser.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
    }

    @Test
    @DisplayName("이메일이 존재하면 true를 반환한다")
    void existsByEmail_returnTrue() {
        // Given
        User user = User.create("exists@example.com", "password", "존재 사용자");
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // When
        boolean result = userRepository.existsByEmail("exists@example.com");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("이메일이 존재하지 않으면 false를 반환한다")
    void existsByEmail_returnFalse() {
        // Given
        String nonexistentEmail = "nonexistent@example.com";

        // When
        boolean result = userRepository.existsByEmail(nonexistentEmail);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("식별자로 사용자를 비관적 쓰기 잠금으로 조회한다")
    void findByIdForUpdate_success() {
        // Given
        User user = User.create("lock@example.com", "password", "잠금 사용자");
        User savedUser = userRepository.saveAndFlush(user);
        entityManager.clear();

        // When
        User foundUser = userRepository.findByIdForUpdate(savedUser.getId()).orElseThrow();

        // Then
        assertThat(foundUser.getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.getEmail()).isEqualTo("lock@example.com");
        assertThat(entityManager.getLockMode(foundUser)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("존재하지 않는 식별자를 비관적 쓰기 잠금으로 조회하면 빈 결과를 반환한다")
    void findByIdForUpdate_notFound() {
        // Given
        UUID nonexistentId = UUID.randomUUID();

        // When
        Optional<User> result = userRepository.findByIdForUpdate(nonexistentId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("중복 이메일로 저장하면 예외가 발생한다")
    void duplicateEmail_throwsException() {
        // Given
        User firstUser = User.create("duplicate@example.com", "password1", "첫 번째 사용자");
        User secondUser = User.create("duplicate@example.com", "password2", "두 번째 사용자");
        userRepository.saveAndFlush(firstUser);

        // When & Then
        assertThatThrownBy(() -> userRepository.saveAndFlush(secondUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("이메일이 null인 사용자를 저장하면 예외가 발생한다")
    void saveWithNullEmail_throwsException() {
        // Given
        User user = User.create(null, "password", "이메일 없음");

        // When & Then
        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("이름이 null인 사용자를 저장하면 예외가 발생한다")
    void saveWithNullName_throwsException() {
        // Given
        User user = User.create("null-name@example.com", "password", null);

        // When & Then
        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("255자를 초과하는 이메일로 저장하면 예외가 발생한다")
    void tooLongEmail_throwsException() {
        // Given
        String tooLongEmail = "a".repeat(244) + "@example.com";
        User user = User.create(tooLongEmail, "password", "긴 이메일 사용자");

        // When & Then
        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("100자를 초과하는 이름으로 저장하면 예외가 발생한다")
    void tooLongUsername_throwsException() {
        // Given
        User user = User.create("long-name@example.com", "password", "가".repeat(101));

        // When & Then
        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
