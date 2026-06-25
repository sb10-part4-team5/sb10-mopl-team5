package com.codeit.team5.mopl.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.config.JpaAuditingConfig;
import com.codeit.team5.mopl.notification.entity.Notification;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.user.entity.User;
import jakarta.persistence.EntityManager;
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
@Import({JpaAuditingConfig.class, TestcontainersConfiguration.class})
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID persistReceiver(String email) {
        User user = User.create(email, "password", "수신자");
        entityManager.persist(user);
        entityManager.flush();
        return user.getId();
    }

    @Test
    @DisplayName("알림 저장에 성공하고 생성 시각이 자동으로 기록된다")
    void saveNotification_success() {
        // Given
        UUID receiverId = persistReceiver("save@example.com");
        Notification notification = Notification.create(
                receiverId, NotificationType.FOLLOWED, "제목", "내용", NotificationLevel.INFO);

        // When
        Notification saved = notificationRepository.save(notification);
        entityManager.flush();
        entityManager.clear();

        // Then
        Notification found = notificationRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getId()).isNotNull();
        assertThat(found.getReceiverId()).isEqualTo(receiverId);
        assertThat(found.getType()).isEqualTo(NotificationType.FOLLOWED);
        assertThat(found.getTitle()).isEqualTo("제목");
        assertThat(found.getContent()).isEqualTo("내용");
        assertThat(found.getLevel()).isEqualTo(NotificationLevel.INFO);
        assertThat(found.isRead()).isFalse();
        assertThat(found.getReadAt()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("식별자로 알림을 조회한다")
    void findById_success() {
        // Given
        UUID receiverId = persistReceiver("find@example.com");
        Notification saved = notificationRepository.save(Notification.create(
                receiverId, NotificationType.PLAYLIST_UPDATED, "조회 제목", "조회 내용",
                NotificationLevel.WARNING));
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Notification> result = notificationRepository.findById(saved.getId());

        // Then
        assertThat(result)
                .isPresent()
                .get()
                .extracting(Notification::getTitle, Notification::getLevel)
                .containsExactly("조회 제목", NotificationLevel.WARNING);
    }

    @Test
    @DisplayName("존재하지 않는 식별자로 조회하면 빈 결과를 반환한다")
    void findById_notFound() {
        // Given
        UUID nonexistentId = UUID.randomUUID();

        // When
        Optional<Notification> result = notificationRepository.findById(nonexistentId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("읽음 처리한 알림의 상태가 영속화된다")
    void markAsRead_persisted() {
        // Given
        UUID receiverId = persistReceiver("read@example.com");
        Notification saved = notificationRepository.save(Notification.create(
                receiverId, NotificationType.DIRECT_MESSAGE, "제목", "내용", NotificationLevel.INFO));

        // When
        saved.markAsRead();
        notificationRepository.save(saved);
        entityManager.flush();
        entityManager.clear();

        // Then
        Notification found = notificationRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.isRead()).isTrue();
        assertThat(found.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 수신자로 저장하면 예외가 발생한다")
    void saveWithInvalidReceiver_throwsException() {
        // Given
        Notification notification = Notification.create(
                UUID.randomUUID(), NotificationType.FOLLOWED, "제목", "내용", NotificationLevel.INFO);

        // When & Then
        assertThatThrownBy(() -> notificationRepository.saveAndFlush(notification))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
