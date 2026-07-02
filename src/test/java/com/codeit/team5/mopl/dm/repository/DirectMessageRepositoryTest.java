package com.codeit.team5.mopl.dm.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.global.util.UuidUtils;
import com.codeit.team5.mopl.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;

class DirectMessageRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private DirectMessageRepository directMessageRepository;

    private User userA;
    private User userB;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        clear();
        userA = persistUser("a@mopl.com", "A");
        userB = persistUser("b@mopl.com", "B");
        conversation = persistAndFlush(Conversation.create(userA, userB));
    }

    private User persistUser(String email, String name) {
        return persistAndFlush(User.create(email, "pw", name));
    }

    private DirectMessage persistMessage(User sender, String content) {
        DirectMessage message = persistAndFlush(DirectMessage.create(conversation, sender, content));
        sleep();
        return message;
    }

    private void sleep() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("대화의 가장 최근 메시지 조회 성공")
    void findTopByConversationIdOrderByCreatedAtDesc_success() {
        // given
        persistMessage(userA, "첫 번째");
        persistMessage(userB, "두 번째");
        DirectMessage latest = persistMessage(userA, "세 번째");
        flush();
        clear();

        // when
        Optional<DirectMessage> result = directMessageRepository
                .findTopByConversationIdOrderByCreatedAtDescIdDesc(conversation.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(latest.getId());
        assertThat(result.get().getContent()).isEqualTo("세 번째");
    }

    @Test
    @DisplayName("받은 안 읽은 메시지 존재 여부 조회 성공")
    void existsByConversationIdAndReceiverIdAndReadFalse_success() {
        // given - userA가 보낸 메시지는 receiver가 userB
        persistMessage(userA, "안읽음1");
        DirectMessage readMessage = persistMessage(userB, "userA에게");
        readMessage.markAsRead();
        persistAndFlush(readMessage);
        flush();
        clear();

        // when
        boolean unreadForB = directMessageRepository
                .existsByConversationIdAndReceiverIdAndReadFalse(conversation.getId(), userB.getId());
        boolean unreadForA = directMessageRepository
                .existsByConversationIdAndReceiverIdAndReadFalse(conversation.getId(), userA.getId());

        // then
        assertThat(unreadForB).isTrue();
        assertThat(unreadForA).isFalse();
    }

    @Test
    @DisplayName("기준 시점 이하의 안 읽은 메시지만 일괄 읽음 처리 성공")
    void markAsReadUntil_success() {
        // given - userA가 보낸 메시지는 receiver가 userB
        DirectMessage alreadyRead = persistMessage(userA, "이미읽음");
        alreadyRead.markAsRead();
        persistAndFlush(alreadyRead);
        Instant alreadyReadAt = alreadyRead.getReadAt();

        DirectMessage before = persistMessage(userA, "기준이전");
        DirectMessage atBoundary = persistMessage(userA, "기준시점");
        DirectMessage after = persistMessage(userA, "기준이후");
        DirectMessage otherReceiver = persistMessage(userB, "userA에게");
        flush();
        clear();

        Instant readAt = Instant.parse("2030-01-01T00:00:00Z");

        // when
        int updated = directMessageRepository.markAsReadUntil(
                conversation.getId(), userB.getId(), atBoundary.getCreatedAt(), atBoundary.getId(), readAt);

        // then
        assertThat(updated).isEqualTo(2);

        DirectMessage reloadedBefore = directMessageRepository.findById(before.getId()).orElseThrow();
        assertThat(reloadedBefore.isRead()).isTrue();
        assertThat(compareInstant(reloadedBefore.getReadAt(), readAt)).isTrue();

        DirectMessage reloadedBoundary = directMessageRepository.findById(atBoundary.getId()).orElseThrow();
        assertThat(reloadedBoundary.isRead()).isTrue();
        assertThat(compareInstant(reloadedBoundary.getReadAt(), readAt)).isTrue();

        DirectMessage reloadedAfter = directMessageRepository.findById(after.getId()).orElseThrow();
        assertThat(reloadedAfter.isRead()).isFalse();
        assertThat(reloadedAfter.getReadAt()).isNull();

        DirectMessage reloadedOther = directMessageRepository.findById(otherReceiver.getId()).orElseThrow();
        assertThat(reloadedOther.isRead()).isFalse();
        assertThat(reloadedOther.getReadAt()).isNull();

        DirectMessage reloadedAlreadyRead = directMessageRepository.findById(alreadyRead.getId()).orElseThrow();
        assertThat(reloadedAlreadyRead.isRead()).isTrue();
        assertThat(compareInstant(reloadedAlreadyRead.getReadAt(), alreadyReadAt)).isTrue();
    }

    @Test
    @DisplayName("메시지 최신순 커서 다음 페이지 조회 성공")
    void findMessages_cursorPaginationDesc_success() {
        // given
        DirectMessage m1 = persistMessage(userA, "1");
        DirectMessage m2 = persistMessage(userB, "2");
        DirectMessage m3 = persistMessage(userA, "3");
        DirectMessage m4 = persistMessage(userB, "4");
        DirectMessage m5 = persistMessage(userA, "5");
        flush();
        clear();

        // when: DESC 최신순 → m5, m4, m3, m2, m1
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        Window<DirectMessage> first = directMessageRepository.findByConversationId(
                conversation.getId(), ScrollPosition.keyset(), Limit.of(2), sort);

        DirectMessage cursor = first.getContent().get(1);
        Window<DirectMessage> second = directMessageRepository.findByConversationId(
                conversation.getId(),
                ScrollPosition.forward(Map.of("createdAt", cursor.getCreatedAt(), "id", cursor.getId())),
                Limit.of(2), sort);

        // then
        assertThat(first.getContent()).extracting(DirectMessage::getId).containsExactly(m5.getId(), m4.getId());
        assertThat(first.hasNext()).isTrue();
        assertThat(second.getContent()).extracting(DirectMessage::getId)
                .containsExactly(m3.getId(), m2.getId())
                .doesNotContain(m5.getId(), m4.getId());
    }

    @Test
    @DisplayName("메시지 오래된순 정렬 조회 성공")
    void findMessages_ascOrder_success() {
        // given
        persistMessage(userA, "1");
        persistMessage(userB, "2");
        persistMessage(userA, "3");
        flush();
        clear();

        // when
        Sort sort = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
        Window<DirectMessage> result = directMessageRepository.findByConversationId(
                conversation.getId(), ScrollPosition.keyset(), Limit.of(10), sort);

        // then
        assertThat(result.getContent()).extracting(DirectMessage::getContent).containsExactly("1", "2", "3");
    }

    @Test
    @DisplayName("대화별 최근 메시지 일괄 조회 성공")
    void findLatestMessagesByConversationIds_success() {
        // given
        User userC = persistUser("c@mopl.com", "C");
        Conversation other = persistAndFlush(Conversation.create(userA, userC));
        persistMessage(userA, "1");
        persistMessage(userB, "2");
        persistAndFlush(DirectMessage.create(other, userA, "other1"));
        sleep();
        persistAndFlush(DirectMessage.create(other, userC, "other2"));
        sleep();
        flush();
        clear();

        // when
        List<DirectMessage> result = directMessageRepository
                .findLatestMessagesByConversationIds(List.of(conversation.getId(), other.getId()));

        // then
        assertThat(result).extracting(DirectMessage::getContent)
                .containsExactlyInAnyOrder("2", "other2");
    }

    @Test
    @DisplayName("안 읽은 메시지가 있는 대화 ID 일괄 조회 성공")
    void findConversationIdsWithUnread_success() {
        // given
        User userC = persistUser("c@mopl.com", "C");
        Conversation other = persistAndFlush(Conversation.create(userB, userC));
        persistMessage(userA, "안읽음");
        DirectMessage readMessage = persistAndFlush(DirectMessage.create(other, userC, "읽음"));
        readMessage.markAsRead();
        persistAndFlush(readMessage);
        flush();
        clear();

        // when
        List<UUID> result = directMessageRepository.findConversationIdsWithUnread(
                List.of(conversation.getId(), other.getId()), userB.getId());

        // then
        assertThat(result).containsExactly(conversation.getId());
    }

    @Test
    @DisplayName("최근 메시지 조회 시 createdAt이 같으면 id로 tie-break 성공")
    void findLatestMessagesByConversationIds_tieBreak_success() {
        // given - 동일 createdAt 메시지 2건 (createdAt은 updatable=false라 native로 통일)
        DirectMessage m1 = persistAndFlush(DirectMessage.create(conversation, userA, "동시1"));
        DirectMessage m2 = persistAndFlush(DirectMessage.create(conversation, userA, "동시2"));
        Instant sameTime = Instant.parse("2030-01-01T00:00:00Z");
        entityManager.getEntityManager()
                .createNativeQuery("update direct_messages set created_at = :t where id in (:a, :b)")
                .setParameter("t", sameTime)
                .setParameter("a", m1.getId())
                .setParameter("b", m2.getId())
                .executeUpdate();
        flush();
        clear();

        // when
        List<DirectMessage> result = directMessageRepository
                .findLatestMessagesByConversationIds(List.of(conversation.getId()));

        // then
        assertThat(result).hasSize(1);
        UUID expectedId = UuidUtils.compareUnsigned(m1.getId(), m2.getId()) >= 0 ? m1.getId() : m2.getId();
        assertThat(result.get(0).getId()).isEqualTo(expectedId);
    }
}
