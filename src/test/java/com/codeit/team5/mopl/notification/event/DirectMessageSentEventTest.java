package com.codeit.team5.mopl.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.team5.mopl.notification.exception.InvalidContentException;
import com.codeit.team5.mopl.notification.exception.InvalidNicknameException;
import com.codeit.team5.mopl.notification.exception.InvalidReceiverIdException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DirectMessageSentEventTest {

    @Test
    @DisplayName("유효한 값이면 이벤트 생성에 성공한다")
    void create_success() {
        // given
        UUID receiverId = UUID.randomUUID();

        // when
        DirectMessageSentEvent event =
                new DirectMessageSentEvent(receiverId, "다린", "안녕하세요");

        // then
        assertThat(event.receiverId()).isEqualTo(receiverId);
        assertThat(event.senderNickname()).isEqualTo("다린");
        assertThat(event.content()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("receiverId가 null이면 예외가 발생한다")
    void create_receiverIdNull_exception() {
        // when & then
        assertThatThrownBy(() -> new DirectMessageSentEvent(null, "다린", "안녕하세요"))
                .isInstanceOf(InvalidReceiverIdException.class);
    }

    @Test
    @DisplayName("송신자 닉네임이 비어 있으면 예외가 발생한다")
    void create_blankNickname_exception() {
        // when & then
        assertThatThrownBy(() ->
                new DirectMessageSentEvent(UUID.randomUUID(), "  ", "안녕하세요"))
                .isInstanceOf(InvalidNicknameException.class);
    }

    @Test
    @DisplayName("송신자 닉네임이 null이면 예외가 발생한다")
    void create_nullNickname_exception() {
        // when & then
        assertThatThrownBy(() ->
                new DirectMessageSentEvent(UUID.randomUUID(), null, "안녕하세요"))
                .isInstanceOf(InvalidNicknameException.class);
    }

    @Test
    @DisplayName("메시지 내용이 비어 있으면 예외가 발생한다")
    void create_blankContent_exception() {
        // when & then
        assertThatThrownBy(() ->
                new DirectMessageSentEvent(UUID.randomUUID(), "다린", "  "))
                .isInstanceOf(InvalidContentException.class);
    }

    @Test
    @DisplayName("메시지 내용이 null이면 예외가 발생한다")
    void create_nullContent_exception() {
        // when & then
        assertThatThrownBy(() ->
                new DirectMessageSentEvent(UUID.randomUUID(), "다린", null))
                .isInstanceOf(InvalidContentException.class);
    }
}
