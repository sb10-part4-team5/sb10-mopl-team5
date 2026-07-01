package com.codeit.team5.mopl.dm.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.team5.mopl.dm.dto.request.DirectMessageCursorRequest;
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
import org.springframework.data.domain.Sort.Direction;

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
        DirectMessageCursorRequest first = new DirectMessageCursorRequest(null, null, 2, Direction.DESC);
        List<DirectMessage> firstFetched = directMessageRepository.findMessages(conversation.getId(), first, 3);
        List<DirectMessage> firstPage = firstFetched.subList(0, 2);
        DirectMessage cursor = firstPage.get(1);
        DirectMessageCursorRequest second = new DirectMessageCursorRequest(
                cursor.getCreatedAt().toString(), cursor.getId().toString(), 2, Direction.DESC);
        List<DirectMessage> secondFetched = directMessageRepository.findMessages(conversation.getId(), second, 3);

        // then
        assertThat(firstFetched).hasSize(3);
        assertThat(firstPage).extracting(DirectMessage::getId).containsExactly(m5.getId(), m4.getId());
        assertThat(secondFetched).extracting(DirectMessage::getId)
                .doesNotContain(m5.getId(), m4.getId())
                .contains(m3.getId(), m2.getId(), m1.getId());
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

        DirectMessageCursorRequest request = new DirectMessageCursorRequest(null, null, 10, Direction.ASC);

        // when
        List<DirectMessage> result = directMessageRepository.findMessages(conversation.getId(), request, 11);

        // then
        assertThat(result).extracting(DirectMessage::getContent).containsExactly("1", "2", "3");
    }

    @Test
    @DisplayName("다른 대화의 메시지는 제외하고 메시지 개수 조회 성공")
    void countMessages_success() {
        // given
        persistMessage(userA, "1");
        persistMessage(userB, "2");
        User userC = persistUser("c@mopl.com", "C");
        Conversation other = persistAndFlush(Conversation.create(userA, userC));
        persistAndFlush(DirectMessage.create(other, userA, "other"));
        flush();
        clear();

        // when
        long count = directMessageRepository.countMessages(conversation.getId());

        // then
        assertThat(count).isEqualTo(2L);
    }
}
