package com.codeit.team5.mopl.dm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.dm.dto.request.DirectMessageCursorRequest;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.dm.exception.ConversationNotFoundException;
import com.codeit.team5.mopl.dm.exception.DirectMessageNotFoundException;
import com.codeit.team5.mopl.dm.exception.NotConversationParticipantException;
import com.codeit.team5.mopl.dm.mapper.DirectMessageMapper;
import com.codeit.team5.mopl.dm.repository.ConversationRepository;
import com.codeit.team5.mopl.dm.repository.DirectMessageRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.notification.event.DirectMessageSentEvent;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Window;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceTest {

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DirectMessageMapper directMessageMapper;

    @Mock
    private WebSocketSessionStore webSocketSessionStore;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DirectMessageService directMessageService;

    private User userWithId(String email, String name, UUID id) {
        User user = User.create(email, "pw", name);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("DM 메시지 전송 성공")
    void sendMessage_success() {
        // given
        UUID conversationId = UUID.randomUUID();
        User sender = userWithId("a@mopl.com", "A", UUID.randomUUID());
        User receiver = userWithId("b@mopl.com", "B", UUID.randomUUID());
        Conversation conversation = Conversation.create(sender, receiver);
        DirectMessageResponse response = new DirectMessageResponse(
                UUID.randomUUID(), conversationId, null, null, "hello", null);

        when(userRepository.findByEmail("a@mopl.com")).thenReturn(Optional.of(sender));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(directMessageRepository.save(any(DirectMessage.class))).then(returnsFirstArg());
        when(directMessageMapper.toResponse(any(DirectMessage.class))).thenReturn(response);
        when(webSocketSessionStore.isSubscribed(any(), any())).thenReturn(false);

        // when
        DirectMessageResponse result = directMessageService.sendMessage("a@mopl.com", conversationId, "hello");

        // then
        assertThat(result).isSameAs(response);
        verify(directMessageRepository).save(any(DirectMessage.class));
        verify(eventPublisher).publishEvent(any(DirectMessageBroadcastEvent.class));
        verify(eventPublisher).publishEvent(any(DirectMessageSentEvent.class));
    }

    @Test
    @DisplayName("수신자가 대화방 활성 상태면 알림 미발행 성공")
    void sendMessage_receiverActive_noNotificationSuccess() {
        // given
        UUID conversationId = UUID.randomUUID();
        User sender = userWithId("a@mopl.com", "A", UUID.randomUUID());
        User receiver = userWithId("b@mopl.com", "B", UUID.randomUUID());
        Conversation conversation = Conversation.create(sender, receiver);
        DirectMessageResponse response = new DirectMessageResponse(
                UUID.randomUUID(), conversationId, null, null, "hello", null);

        when(userRepository.findByEmail("a@mopl.com")).thenReturn(Optional.of(sender));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(directMessageRepository.save(any(DirectMessage.class))).then(returnsFirstArg());
        when(directMessageMapper.toResponse(any(DirectMessage.class))).thenReturn(response);
        when(webSocketSessionStore.isSubscribed(any(), any())).thenReturn(true);

        // when
        directMessageService.sendMessage("a@mopl.com", conversationId, "hello");

        // then
        verify(eventPublisher).publishEvent(any(DirectMessageBroadcastEvent.class));
        verify(eventPublisher, never()).publishEvent(any(DirectMessageSentEvent.class));
    }

    @Test
    @DisplayName("대화가 없으면 DM 전송 실패")
    void sendMessage_conversationNotFound_throwsException() {
        // given
        UUID conversationId = UUID.randomUUID();
        User sender = userWithId("a@mopl.com", "A", UUID.randomUUID());

        when(userRepository.findByEmail("a@mopl.com")).thenReturn(Optional.of(sender));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> directMessageService.sendMessage("a@mopl.com", conversationId, "hello"))
                .isInstanceOf(ConversationNotFoundException.class);
        verify(directMessageRepository, never()).save(any(DirectMessage.class));
    }

    @Test
    @DisplayName("보낸 사람이 대화 참여자가 아니면 DM 전송 실패")
    void sendMessage_senderNotParticipant_throwsException() {
        // given
        UUID conversationId = UUID.randomUUID();
        User participant1 = userWithId("a@mopl.com", "A", UUID.randomUUID());
        User participant2 = userWithId("b@mopl.com", "B", UUID.randomUUID());
        User outsider = userWithId("c@mopl.com", "C", UUID.randomUUID());
        Conversation conversation = Conversation.create(participant1, participant2);

        when(userRepository.findByEmail("c@mopl.com")).thenReturn(Optional.of(outsider));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // when & then
        assertThatThrownBy(() -> directMessageService.sendMessage("c@mopl.com", conversationId, "hello"))
                .isInstanceOf(NotConversationParticipantException.class);
        verify(directMessageRepository, never()).save(any(DirectMessage.class));
    }

    @Test
    @DisplayName("참여자가 메시지 목록을 커서 조회하면 성공")
    void getMessages_participant_success() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        User currentUser = userWithId("a@mopl.com", "A", currentUserId);
        User other = userWithId("b@mopl.com", "B", UUID.randomUUID());
        Conversation conversation = Conversation.create(currentUser, other);
        DirectMessageResponse messageResponse = new DirectMessageResponse(
                UUID.randomUUID(), conversationId, null, null, "hi", null);
        CursorResponse<DirectMessageResponse> cursorResponse = new CursorResponse<>(
                List.of(messageResponse), null, null, false, 1L, "createdAt", "DESCENDING");

        Window<DirectMessage> window = Window.from(List.of(), i -> ScrollPosition.keyset());
        when(conversationRepository.existsByIdAndParticipantId(conversationId, currentUserId)).thenReturn(true);
        when(directMessageRepository.findByConversationId(eq(conversationId), any(ScrollPosition.class),
                any(Limit.class), any(Sort.class))).thenReturn(window);
        when(directMessageMapper.toCursor(eq(window), eq(Direction.DESC)))
                .thenReturn(cursorResponse);

        DirectMessageCursorRequest request = new DirectMessageCursorRequest(null, null, 2, Direction.DESC);

        // when
        CursorResponse<DirectMessageResponse> result =
                directMessageService.getMessages(currentUserId, conversationId, request);

        // then
        assertThat(result).isSameAs(cursorResponse);
    }

    @Test
    @DisplayName("비참여자가 메시지 목록을 조회하면 실패")
    void getMessages_notParticipant_throwsException() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        User participant1 = userWithId("b@mopl.com", "B", UUID.randomUUID());
        User participant2 = userWithId("c@mopl.com", "C", UUID.randomUUID());
        Conversation conversation = Conversation.create(participant1, participant2);

        when(conversationRepository.existsByIdAndParticipantId(conversationId, currentUserId)).thenReturn(false);
        when(conversationRepository.existsById(conversationId)).thenReturn(true);

        DirectMessageCursorRequest request = new DirectMessageCursorRequest(null, null, 2, Direction.DESC);

        // when & then
        assertThatThrownBy(() -> directMessageService.getMessages(currentUserId, conversationId, request))
                .isInstanceOf(NotConversationParticipantException.class);
        verify(directMessageRepository, never()).findByConversationId(any(), any(), any(), any());
    }

    @Test
    @DisplayName("대화가 없으면 메시지 목록 조회 실패")
    void getMessages_conversationNotFound_throwsException() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();

        when(conversationRepository.existsByIdAndParticipantId(conversationId, currentUserId)).thenReturn(false);
        when(conversationRepository.existsById(conversationId)).thenReturn(false);

        DirectMessageCursorRequest request = new DirectMessageCursorRequest(null, null, 2, Direction.DESC);

        // when & then
        assertThatThrownBy(() -> directMessageService.getMessages(currentUserId, conversationId, request))
                .isInstanceOf(ConversationNotFoundException.class);
        verify(directMessageRepository, never()).findByConversationId(any(), any(), any(), any());
    }

    @Test
    @DisplayName("참여자가 메시지를 읽음 처리하면 성공")
    void markMessagesAsRead_participant_success() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID directMessageId = UUID.randomUUID();
        User currentUser = userWithId("a@mopl.com", "A", currentUserId);
        User other = userWithId("b@mopl.com", "B", UUID.randomUUID());
        Conversation conversation = Conversation.create(currentUser, other);
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        DirectMessage message = DirectMessage.create(conversation, other, "hello");
        Instant createdAt = Instant.now();
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        ReflectionTestUtils.setField(message, "id", directMessageId);

        when(conversationRepository.existsByIdAndParticipantId(conversationId, currentUserId)).thenReturn(true);
        when(directMessageRepository.findById(directMessageId)).thenReturn(Optional.of(message));

        // when
        directMessageService.markMessagesAsRead(currentUserId, conversationId, directMessageId);

        // then
        verify(directMessageRepository)
                .markAsReadUntil(eq(conversationId), eq(currentUserId), eq(createdAt), eq(directMessageId),
                        any(Instant.class));
    }

    @Test
    @DisplayName("비참여자가 메시지를 읽음 처리하면 실패")
    void markMessagesAsRead_notParticipant_throwsException() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID directMessageId = UUID.randomUUID();
        User participant1 = userWithId("b@mopl.com", "B", UUID.randomUUID());
        User participant2 = userWithId("c@mopl.com", "C", UUID.randomUUID());
        Conversation conversation = Conversation.create(participant1, participant2);

        when(conversationRepository.existsByIdAndParticipantId(conversationId, currentUserId)).thenReturn(false);
        when(conversationRepository.existsById(conversationId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> directMessageService.markMessagesAsRead(currentUserId, conversationId, directMessageId))
                .isInstanceOf(NotConversationParticipantException.class);
        verify(directMessageRepository, never())
                .markAsReadUntil(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("대화가 없으면 읽음 처리 실패")
    void markMessagesAsRead_conversationNotFound_throwsException() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID directMessageId = UUID.randomUUID();

        when(conversationRepository.existsByIdAndParticipantId(conversationId, currentUserId)).thenReturn(false);
        when(conversationRepository.existsById(conversationId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> directMessageService.markMessagesAsRead(currentUserId, conversationId, directMessageId))
                .isInstanceOf(ConversationNotFoundException.class);
        verify(directMessageRepository, never())
                .markAsReadUntil(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("메시지가 없으면 읽음 처리 실패")
    void markMessagesAsRead_messageNotFound_throwsException() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID directMessageId = UUID.randomUUID();
        User currentUser = userWithId("a@mopl.com", "A", currentUserId);
        User other = userWithId("b@mopl.com", "B", UUID.randomUUID());
        Conversation conversation = Conversation.create(currentUser, other);

        when(conversationRepository.existsByIdAndParticipantId(conversationId, currentUserId)).thenReturn(true);
        when(directMessageRepository.findById(directMessageId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> directMessageService.markMessagesAsRead(currentUserId, conversationId, directMessageId))
                .isInstanceOf(DirectMessageNotFoundException.class);
        verify(directMessageRepository, never())
                .markAsReadUntil(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("다른 대화의 메시지 ID로 읽음 처리하면 실패")
    void markMessagesAsRead_messageFromOtherConversation_throwsException() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID directMessageId = UUID.randomUUID();
        User currentUser = userWithId("a@mopl.com", "A", currentUserId);
        User other = userWithId("b@mopl.com", "B", UUID.randomUUID());
        Conversation conversation = Conversation.create(currentUser, other);
        Conversation otherConversation = Conversation.create(currentUser, other);
        ReflectionTestUtils.setField(otherConversation, "id", UUID.randomUUID());
        DirectMessage message = DirectMessage.create(otherConversation, other, "hello");

        when(conversationRepository.existsByIdAndParticipantId(conversationId, currentUserId)).thenReturn(true);
        when(directMessageRepository.findById(directMessageId)).thenReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> directMessageService.markMessagesAsRead(currentUserId, conversationId, directMessageId))
                .isInstanceOf(DirectMessageNotFoundException.class);
        verify(directMessageRepository, never())
                .markAsReadUntil(any(), any(), any(), any(), any());
    }
}
