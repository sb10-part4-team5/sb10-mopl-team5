package com.codeit.team5.mopl.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.config.JpaAuditingConfig;
import com.codeit.team5.mopl.notification.entity.Notification;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.exception.CursorIdAfterNotTogetherException;
import com.codeit.team5.mopl.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
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

    private void saveNotifications(UUID receiverId, int count) {
        for (int i = 0; i < count; i++) {
            notificationRepository.save(Notification.create(
                    receiverId, NotificationType.FOLLOWED, "알림" + i, null, NotificationLevel.INFO));
        }
    }

    @Test
    @DisplayName("커서 페이지네이션으로 다음 페이지가 겹치지 않고 이어진다 (최신순)")
    void findPageByReceiverDesc_pagination() {
        // Given
        UUID receiverId = persistReceiver("page@example.com");
        saveNotifications(receiverId, 5);
        // 다른 수신자의 알림 (조회 대상에서 제외되어야 함)
        UUID otherReceiverId = persistReceiver("page-other@example.com");
        saveNotifications(otherReceiverId, 3);
        entityManager.flush();
        entityManager.clear();

        // When: 첫 페이지 (limit=2)
        List<Notification> first = notificationRepository
                .findPageByReceiverDesc(receiverId, null, null, Limit.of(2));
        // 첫 페이지 마지막을 커서로 다음 페이지 조회
        Notification cursor = first.get(first.size() - 1);
        List<Notification> second = notificationRepository
                .findPageByReceiverDesc(receiverId, cursor.getCreatedAt(), cursor.getId(), Limit.of(2));

        // Then
        assertThat(first).hasSize(2);
        assertThat(second).hasSize(2);
        assertThat(second).extracting(Notification::getId)
                .doesNotContainAnyElementsOf(first.stream().map(Notification::getId).toList());
        assertThat(notificationRepository.countByReceiverId(receiverId)).isEqualTo(5);
    }

    @Test
    @DisplayName("cursor와 idAfter 중 하나만 주어지면 예외가 발생한다")
    void findPageByReceiverDesc_cursorIdAfterNotTogether_exception() {
        // Given
        UUID receiverId = UUID.randomUUID();

        // When & Then
        assertThatThrownBy(() -> notificationRepository
                .findPageByReceiverDesc(receiverId, Instant.now(), null, Limit.of(2)))
                .isInstanceOf(CursorIdAfterNotTogetherException.class);

        assertThatThrownBy(() -> notificationRepository
                .findPageByReceiverDesc(receiverId, null, UUID.randomUUID(), Limit.of(2)))
                .isInstanceOf(CursorIdAfterNotTogetherException.class);
    }

    @Test
    @DisplayName("최신순 조회는 createdAt, id 내림차순 정렬을 따른다")
    void findPageByReceiverDesc_ordering() {
        // Given
        UUID receiverId = persistReceiver("order-desc@example.com");
        saveNotifications(receiverId, 4);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Notification> all = notificationRepository
                .findPageByReceiverDesc(receiverId, null, null, Limit.of(10));

        // Then (주 정렬 기준 createdAt만 검증 - id tie-break은 페이지네이션 테스트가 담당.
        // Java UUID.compareTo와 Postgres uuid 정렬 순서가 달라 id 비교는 단언하지 않음)
        assertThat(all).hasSize(4);
        assertThat(all).isSortedAccordingTo(
                Comparator.comparing(Notification::getCreatedAt).reversed());
    }

    @Test
    @DisplayName("오래된순 조회는 createdAt, id 오름차순 정렬을 따른다")
    void findPageByReceiverAsc_ordering() {
        // Given
        UUID receiverId = persistReceiver("order-asc@example.com");
        saveNotifications(receiverId, 4);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Notification> all = notificationRepository
                .findPageByReceiverAsc(receiverId, null, null, Limit.of(10));

        // Then (주 정렬 기준 createdAt만 검증 - 위 DESC 테스트와 동일한 이유)
        assertThat(all).hasSize(4);
        assertThat(all).isSortedAccordingTo(
                Comparator.comparing(Notification::getCreatedAt));
    }

    @Test
    @DisplayName("안 읽은 알림 개수를 센다")
    void countByReceiverIdAndIsReadFalse_success() {
        // Given
        UUID receiverId = persistReceiver("count@example.com");
        notificationRepository.save(Notification.create(
                receiverId, NotificationType.FOLLOWED, "안읽음1", null, NotificationLevel.INFO));
        notificationRepository.save(Notification.create(
                receiverId, NotificationType.FOLLOWED, "안읽음2", null, NotificationLevel.INFO));
        Notification read = notificationRepository.save(Notification.create(
                receiverId, NotificationType.FOLLOWED, "읽음", null, NotificationLevel.INFO));
        read.markAsRead();
        notificationRepository.save(read);
        entityManager.flush();
        entityManager.clear();

        // When
        long count = notificationRepository.countByReceiverIdAndIsReadFalse(receiverId);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("식별자와 수신자가 모두 일치하면 알림을 조회한다")
    void findByIdAndReceiverId_success() {
        // Given
        UUID receiverId = persistReceiver("owner@example.com");
        Notification saved = notificationRepository.save(Notification.create(
                receiverId, NotificationType.FOLLOWED, "제목", "내용", NotificationLevel.INFO));
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Notification> result =
                notificationRepository.findByIdAndReceiverId(saved.getId(), receiverId);

        // Then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("수신자가 다르면 알림을 조회하지 못한다")
    void findByIdAndReceiverId_otherReceiver_empty() {
        // Given
        UUID receiverId = persistReceiver("real-owner@example.com");
        Notification saved = notificationRepository.save(Notification.create(
                receiverId, NotificationType.FOLLOWED, "제목", "내용", NotificationLevel.INFO));
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Notification> result =
                notificationRepository.findByIdAndReceiverId(saved.getId(), UUID.randomUUID());

        // Then
        assertThat(result).isEmpty();
    }
}
