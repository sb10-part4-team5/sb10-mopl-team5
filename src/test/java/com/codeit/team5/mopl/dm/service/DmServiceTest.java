package com.codeit.team5.mopl.dm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.dm.dto.response.ConversationResponse;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.dm.exception.ConversationNotFoundException;
import com.codeit.team5.mopl.dm.exception.NotConversationParticipantException;
import com.codeit.team5.mopl.dm.exception.SelfConversationException;
import com.codeit.team5.mopl.notification.event.DirectMessageSentEvent;
import com.codeit.team5.mopl.dm.mapper.DmMapper;
import com.codeit.team5.mopl.dm.repository.ConversationRepository;
import com.codeit.team5.mopl.dm.repository.DirectMessageRepository;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DmServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DmMapper dmMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DmService dmService;

    private User userWithId(String email, String name, UUID id) {
        User user = User.create(email, "pw", name);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("새로운 대화 생성 성공")
    void getOrCreateConversation_createNew_success() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        User currentUser = userWithId("a@mopl.com", "A", currentUserId);
        User otherUser = userWithId("b@mopl.com", "B", otherId);
        UserSummaryResponse summary = new UserSummaryResponse(otherId, "B", null);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(otherId)).thenReturn(Optional.of(otherUser));
        when(conversationRepository.findByParticipant1IdAndParticipant2Id(any(), any()))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).then(returnsFirstArg());
        when(userMapper.toSummaryResponse(any(User.class))).thenReturn(summary);
        when(directMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.empty());
        when(directMessageRepository.countByConversationIdAndReceiverIdAndReadFalse(any(), any()))
                .thenReturn(0L);

        // when
        ConversationResponse result = dmService.getOrCreateConversation(currentUserId, otherId);

        // then
        assertThat(result.with()).isSameAs(summary);
        assertThat(result.hasUnread()).isFalse();
        assertThat(result.lastestMessage()).isNull();

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        Conversation saved = captor.getValue();
        assertThat(saved.getOtherParticipant(currentUser)).isSameAs(otherUser);
    }

    @Test
    @DisplayName("기존 대화 재사용 성공")
    void getOrCreateConversation_reuseExisting_success() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        User currentUser = userWithId("a@mopl.com", "A", currentUserId);
        User otherUser = userWithId("b@mopl.com", "B", otherId);
        Conversation existing = Conversation.create(currentUser, otherUser);
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        UserSummaryResponse summary = new UserSummaryResponse(otherId, "B", null);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(otherId)).thenReturn(Optional.of(otherUser));
        when(conversationRepository.findByParticipant1IdAndParticipant2Id(any(), any()))
                .thenReturn(Optional.of(existing));
        when(userMapper.toSummaryResponse(any(User.class))).thenReturn(summary);
        when(directMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.empty());
        when(directMessageRepository.countByConversationIdAndReceiverIdAndReadFalse(any(), any()))
                .thenReturn(2L);

        // when
        ConversationResponse result = dmService.getOrCreateConversation(currentUserId, otherId);

        // then
        assertThat(result.id()).isEqualTo(existing.getId());
        assertThat(result.with()).isSameAs(summary);
        assertThat(result.hasUnread()).isTrue();
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    @DisplayName("자기 자신과 대화를 시작하면 실패")
    void getOrCreateConversation_self_throwsException() {
        // given
        UUID userId = UUID.randomUUID();
        User user = userWithId("a@mopl.com", "A", userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(conversationRepository.findByParticipant1IdAndParticipant2Id(any(), any()))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> dmService.getOrCreateConversation(userId, userId))
                .isInstanceOf(SelfConversationException.class);
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    @DisplayName("상대 사용자가 없으면 실패")
    void getOrCreateConversation_otherUserNotFound_throwsException() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        User currentUser = userWithId("a@mopl.com", "A", currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(userRepository.findById(otherId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> dmService.getOrCreateConversation(currentUserId, otherId))
                .isInstanceOf(UserNotFoundException.class);
        verify(conversationRepository, never()).save(any(Conversation.class));
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
        when(dmMapper.toResponse(any(DirectMessage.class))).thenReturn(response);

        // when
        DirectMessageResponse result = dmService.sendMessage("a@mopl.com", conversationId, "hello");

        // then
        assertThat(result).isSameAs(response);
        verify(directMessageRepository).save(any(DirectMessage.class));
        verify(eventPublisher).publishEvent(any(DirectMessageSentEvent.class));
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
        assertThatThrownBy(() -> dmService.sendMessage("a@mopl.com", conversationId, "hello"))
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
        assertThatThrownBy(() -> dmService.sendMessage("c@mopl.com", conversationId, "hello"))
                .isInstanceOf(NotConversationParticipantException.class);
        verify(directMessageRepository, never()).save(any(DirectMessage.class));
    }

    @Test
    @DisplayName("대화 참여자 검증 성공")
    void validateParticipant_participant_success() {
        // given
        UUID conversationId = UUID.randomUUID();
        User participant1 = userWithId("a@mopl.com", "A", UUID.randomUUID());
        User participant2 = userWithId("b@mopl.com", "B", UUID.randomUUID());
        Conversation conversation = Conversation.create(participant1, participant2);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userRepository.findByEmail("a@mopl.com")).thenReturn(Optional.of(participant1));

        // when & then
        assertThatCode(() -> dmService.validateParticipant(conversationId, "a@mopl.com"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("대화 참여자가 아니면 검증 실패")
    void validateParticipant_notParticipant_throwsException() {
        // given
        UUID conversationId = UUID.randomUUID();
        User participant1 = userWithId("a@mopl.com", "A", UUID.randomUUID());
        User participant2 = userWithId("b@mopl.com", "B", UUID.randomUUID());
        User outsider = userWithId("c@mopl.com", "C", UUID.randomUUID());
        Conversation conversation = Conversation.create(participant1, participant2);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userRepository.findByEmail("c@mopl.com")).thenReturn(Optional.of(outsider));

        // when & then
        assertThatThrownBy(() -> dmService.validateParticipant(conversationId, "c@mopl.com"))
                .isInstanceOf(NotConversationParticipantException.class);
    }

    @Test
    @DisplayName("대화가 없으면 참여자 검증 실패")
    void validateParticipant_conversationNotFound_throwsException() {
        // given
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> dmService.validateParticipant(conversationId, "a@mopl.com"))
                .isInstanceOf(ConversationNotFoundException.class);
    }
}
