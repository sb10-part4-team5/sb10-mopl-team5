package com.codeit.team5.mopl.dm.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.team5.mopl.dm.dto.request.ConversationCursorRequest;
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;

class ConversationRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @BeforeEach
    void setUp() {
        clear();
    }

    private User persistUser(String email, String name) {
        return persistAndFlush(User.create(email, "pw", name));
    }

    private Conversation persistConversation(User me, String otherEmail, String otherName) {
        User other = persistUser(otherEmail, otherName);
        Conversation conversation = persistAndFlush(Conversation.create(me, other));
        sleep();
        return conversation;
    }

    private void sleep() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("정렬된 두 참여자로 대화 조회 성공")
    void findByParticipant1IdAndParticipant2Id_success() {
        // given
        User userA = persistUser("a@mopl.com", "A");
        User userB = persistUser("b@mopl.com", "B");
        Conversation conversation = persistAndFlush(Conversation.create(userA, userB));
        flush();
        clear();

        UUID p1 = conversation.getParticipant1().getId();
        UUID p2 = conversation.getParticipant2().getId();

        // when
        Optional<Conversation> result = conversationRepository.findByParticipant1IdAndParticipant2Id(p1, p2);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(conversation.getId());
    }

    @Test
    @DisplayName("존재하지 않는 참여자 조합 조회 시 빈 Optional 반환 성공")
    void findByParticipant1IdAndParticipant2Id_notFound() {
        // given
        User userA = persistUser("a@mopl.com", "A");
        User userB = persistUser("b@mopl.com", "B");
        persistAndFlush(Conversation.create(userA, userB));
        flush();
        clear();

        // when
        Optional<Conversation> result = conversationRepository
                .findByParticipant1IdAndParticipant2Id(UUID.randomUUID(), UUID.randomUUID());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("참여자 여부 exists 검증 성공")
    void existsByIdAndParticipantId_participant_success() {
        // given
        User userA = persistUser("a@mopl.com", "A");
        User userB = persistUser("b@mopl.com", "B");
        Conversation conversation = persistAndFlush(Conversation.create(userA, userB));
        flush();
        clear();

        // when & then
        assertThat(conversationRepository.existsByIdAndParticipantId(conversation.getId(), userA.getId())).isTrue();
        assertThat(conversationRepository.existsByIdAndParticipantId(conversation.getId(), userB.getId())).isTrue();
    }

    @Test
    @DisplayName("비참여자 exists 검증 실패")
    void existsByIdAndParticipantId_notParticipant_success() {
        // given
        User userA = persistUser("a@mopl.com", "A");
        User userB = persistUser("b@mopl.com", "B");
        User stranger = persistUser("c@mopl.com", "C");
        Conversation conversation = persistAndFlush(Conversation.create(userA, userB));
        flush();
        clear();

        // when & then
        assertThat(conversationRepository.existsByIdAndParticipantId(conversation.getId(), stranger.getId())).isFalse();
    }

    @Test
    @DisplayName("내 대화 목록만 커서 조회 성공")
    void findMyConversations_onlyMine_success() {
        // given
        User me = persistUser("me@mopl.com", "ME");
        Conversation mine = persistConversation(me, "o1@mopl.com", "O1");
        User x = persistUser("x@mopl.com", "X");
        User z = persistUser("z@mopl.com", "Z");
        persistAndFlush(Conversation.create(x, z));
        flush();
        clear();

        ConversationCursorRequest request = new ConversationCursorRequest(null, null, null, 10, Direction.DESC);

        // when
        List<Conversation> result = conversationRepository.findMyConversations(me.getId(), request);

        // then
        assertThat(result).extracting(Conversation::getId).containsExactly(mine.getId());
    }

    @Test
    @DisplayName("상대방 이름 검색으로 내 대화 목록 필터 성공")
    void findMyConversations_keywordFilter_success() {
        // given
        User me = persistUser("me@mopl.com", "ME");
        Conversation alice = persistConversation(me, "alice@mopl.com", "Alice");
        persistConversation(me, "bob@mopl.com", "Bob");
        flush();
        clear();

        ConversationCursorRequest request = new ConversationCursorRequest("ali", null, null, 10, Direction.DESC);

        // when
        List<Conversation> result = conversationRepository.findMyConversations(me.getId(), request);

        // then
        assertThat(result).extracting(Conversation::getId).containsExactly(alice.getId());
    }

    @Test
    @DisplayName("내 대화 목록 커서 다음 페이지 조회 성공")
    void findMyConversations_cursorPagination_success() {
        // given
        User me = persistUser("me@mopl.com", "ME");
        Conversation c1 = persistConversation(me, "o1@mopl.com", "O1");
        Conversation c2 = persistConversation(me, "o2@mopl.com", "O2");
        Conversation c3 = persistConversation(me, "o3@mopl.com", "O3");
        Conversation c4 = persistConversation(me, "o4@mopl.com", "O4");
        flush();
        clear();

        // when: DESC 최신순 → c4, c3, c2, c1
        ConversationCursorRequest first = new ConversationCursorRequest(null, null, null, 2, Direction.DESC);
        List<Conversation> firstFetched = conversationRepository.findMyConversations(me.getId(), first);
        List<Conversation> firstPage = firstFetched.subList(0, 2);
        Conversation cursor = firstPage.get(1);
        ConversationCursorRequest second = new ConversationCursorRequest(
                null, cursor.getCreatedAt(), cursor.getId(), 2, Direction.DESC);
        List<Conversation> secondFetched = conversationRepository.findMyConversations(me.getId(), second);

        // then
        assertThat(firstFetched).hasSize(3);
        assertThat(firstPage).extracting(Conversation::getId).containsExactly(c4.getId(), c3.getId());
        assertThat(secondFetched).extracting(Conversation::getId).containsExactly(c2.getId(), c1.getId());
        assertThat(secondFetched).extracting(Conversation::getId)
                .doesNotContain(c4.getId(), c3.getId());
    }

    @Test
    @DisplayName("내 대화 목록 오래된순 정렬 조회 성공")
    void findMyConversations_ascOrder_success() {
        // given
        User me = persistUser("me@mopl.com", "ME");
        Conversation c1 = persistConversation(me, "o1@mopl.com", "O1");
        Conversation c2 = persistConversation(me, "o2@mopl.com", "O2");
        Conversation c3 = persistConversation(me, "o3@mopl.com", "O3");
        flush();
        clear();

        ConversationCursorRequest request = new ConversationCursorRequest(null, null, null, 10, Direction.ASC);

        // when
        List<Conversation> result = conversationRepository.findMyConversations(me.getId(), request);

        // then
        assertThat(result).extracting(Conversation::getId)
                .containsExactly(c1.getId(), c2.getId(), c3.getId());
    }
}
