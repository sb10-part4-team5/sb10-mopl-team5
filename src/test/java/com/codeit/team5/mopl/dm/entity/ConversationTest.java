package com.codeit.team5.mopl.dm.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.team5.mopl.dm.exception.NotConversationParticipantException;
import com.codeit.team5.mopl.dm.exception.SelfConversationException;
import com.codeit.team5.mopl.global.util.UuidUtils;
import com.codeit.team5.mopl.user.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ConversationTest {

    private User userWithId(String email, String name, UUID id) {
        User user = User.create(email, "pw", name);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("참여자 정렬 후 대화 생성 성공")
    void create_ordersParticipants_success() {
        // given
        User userA = userWithId("a@mopl.com", "A", UUID.randomUUID());
        User userB = userWithId("b@mopl.com", "B", UUID.randomUUID());

        // when
        Conversation conversation = Conversation.create(userA, userB);

        // then
        UUID p1 = conversation.getParticipant1().getId();
        UUID p2 = conversation.getParticipant2().getId();
        assertThat(UuidUtils.compareUnsigned(p1, p2)).isLessThan(0);
    }

    @Test
    @DisplayName("자기 자신과 대화 생성 실패")
    void create_self_throwsException() {
        // given
        User user = userWithId("a@mopl.com", "A", UUID.randomUUID());

        // when & then
        assertThatThrownBy(() -> Conversation.create(user, user))
                .isInstanceOf(SelfConversationException.class);
    }

    @Test
    @DisplayName("상대 참여자 조회 성공")
    void getOtherParticipant_success() {
        // given
        User userA = userWithId("a@mopl.com", "A", UUID.randomUUID());
        User userB = userWithId("b@mopl.com", "B", UUID.randomUUID());
        Conversation conversation = Conversation.create(userA, userB);

        // when & then
        assertThat(conversation.getOtherParticipant(userA)).isSameAs(userB);
        assertThat(conversation.getOtherParticipant(userB)).isSameAs(userA);
    }

    @Test
    @DisplayName("참여자가 아닌 사용자의 상대 조회 실패")
    void getOtherParticipant_notParticipant_throwsException() {
        // given
        User userA = userWithId("a@mopl.com", "A", UUID.randomUUID());
        User userB = userWithId("b@mopl.com", "B", UUID.randomUUID());
        User outsider = userWithId("c@mopl.com", "C", UUID.randomUUID());
        Conversation conversation = Conversation.create(userA, userB);

        // when & then
        assertThatThrownBy(() -> conversation.getOtherParticipant(outsider))
                .isInstanceOf(NotConversationParticipantException.class);
    }

    @Test
    @DisplayName("참여자 검증 성공")
    void validateParticipant_participant_success() {
        // given
        User userA = userWithId("a@mopl.com", "A", UUID.randomUUID());
        User userB = userWithId("b@mopl.com", "B", UUID.randomUUID());
        Conversation conversation = Conversation.create(userA, userB);

        // when & then
        assertThatCode(() -> conversation.validateParticipant(userA.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("참여자가 아닌 사용자 검증 실패")
    void validateParticipant_notParticipant_throwsException() {
        // given
        User userA = userWithId("a@mopl.com", "A", UUID.randomUUID());
        User userB = userWithId("b@mopl.com", "B", UUID.randomUUID());
        Conversation conversation = Conversation.create(userA, userB);

        // when & then
        assertThatThrownBy(() -> conversation.validateParticipant(UUID.randomUUID()))
                .isInstanceOf(NotConversationParticipantException.class);
    }
}
