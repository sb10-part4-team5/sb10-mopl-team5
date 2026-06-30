package com.codeit.team5.mopl.dm.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("존재하지 않는 참여자 조합으로 조회하면 빈 Optional 반환")
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
    @DisplayName("내가 참여한 대화 목록 조회 성공")
    void findAllByParticipantId_success() {
        // given
        User userA = persistUser("a@mopl.com", "A");
        User userB = persistUser("b@mopl.com", "B");
        User userC = persistUser("c@mopl.com", "C");
        Conversation ab = persistAndFlush(Conversation.create(userA, userB));
        Conversation ac = persistAndFlush(Conversation.create(userA, userC));
        flush();
        clear();

        // when
        List<Conversation> aConversations = conversationRepository.findAllByParticipantId(userA.getId());
        List<Conversation> bConversations = conversationRepository.findAllByParticipantId(userB.getId());

        // then
        assertThat(aConversations)
                .extracting(Conversation::getId)
                .containsExactlyInAnyOrder(ab.getId(), ac.getId());
        assertThat(bConversations)
                .extracting(Conversation::getId)
                .containsExactly(ab.getId());
    }
}
