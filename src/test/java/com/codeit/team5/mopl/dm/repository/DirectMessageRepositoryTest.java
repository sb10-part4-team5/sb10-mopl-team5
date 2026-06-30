package com.codeit.team5.mopl.dm.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

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
                .findTopByConversationIdOrderByCreatedAtDesc(conversation.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(latest.getId());
        assertThat(result.get().getContent()).isEqualTo("세 번째");
    }

    @Test
    @DisplayName("받은 안 읽은 메시지 개수 조회 성공")
    void countByConversationIdAndReceiverIdAndReadFalse_success() {
        // given - userA가 보낸 메시지는 receiver가 userB
        persistMessage(userA, "안읽음1");
        persistMessage(userA, "안읽음2");
        DirectMessage readMessage = persistMessage(userA, "읽음");
        readMessage.markAsRead();
        persistMessage(userB, "userA에게");
        flush();
        clear();

        // when
        long unreadForB = directMessageRepository
                .countByConversationIdAndReceiverIdAndReadFalse(conversation.getId(), userB.getId());
        long unreadForA = directMessageRepository
                .countByConversationIdAndReceiverIdAndReadFalse(conversation.getId(), userA.getId());

        // then
        assertThat(unreadForB).isEqualTo(2L);
        assertThat(unreadForA).isEqualTo(1L);
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
                conversation.getId(), userB.getId(), atBoundary.getCreatedAt(), readAt);

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
    @DisplayName("대화별 메시지 최신순 페이지 조회 성공")
    void findByConversationIdOrderByCreatedAtDesc_success() {
        // given
        persistMessage(userA, "1");
        persistMessage(userB, "2");
        DirectMessage third = persistMessage(userA, "3");
        flush();
        clear();

        // when
        List<DirectMessage> firstPage = directMessageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversation.getId(), PageRequest.of(0, 2));

        // then
        assertThat(firstPage).hasSize(2);
        assertThat(firstPage.get(0).getId()).isEqualTo(third.getId());
        assertThat(firstPage)
                .extracting(DirectMessage::getContent)
                .containsExactly("3", "2");
    }
}
