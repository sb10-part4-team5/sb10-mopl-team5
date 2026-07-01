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
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.exception.ConversationNotFoundException;
import com.codeit.team5.mopl.dm.exception.NotConversationParticipantException;
import com.codeit.team5.mopl.dm.exception.SelfConversationException;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private DirectMessageRepository directMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private DmMapper dmMapper;

    @InjectMocks
    private ConversationService conversationService;

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
        ConversationResponse result = conversationService.getOrCreateConversation(currentUserId, otherId);

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
        ConversationResponse result = conversationService.getOrCreateConversation(currentUserId, otherId);

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
        assertThatThrownBy(() -> conversationService.getOrCreateConversation(userId, userId))
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
        assertThatThrownBy(() -> conversationService.getOrCreateConversation(currentUserId, otherId))
                .isInstanceOf(UserNotFoundException.class);
        verify(conversationRepository, never()).save(any(Conversation.class));
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
        assertThatCode(() -> conversationService.validateParticipant(conversationId, "a@mopl.com"))
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
        assertThatThrownBy(() -> conversationService.validateParticipant(conversationId, "c@mopl.com"))
                .isInstanceOf(NotConversationParticipantException.class);
    }

    @Test
    @DisplayName("대화가 없으면 참여자 검증 실패")
    void validateParticipant_conversationNotFound_throwsException() {
        // given
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> conversationService.validateParticipant(conversationId, "a@mopl.com"))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    @DisplayName("참여자가 단건 대화 조회 성공")
    void getConversation_participant_success() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        User currentUser = userWithId("a@mopl.com", "A", currentUserId);
        User otherUser = userWithId("b@mopl.com", "B", otherId);
        Conversation conversation = Conversation.create(currentUser, otherUser);
        ReflectionTestUtils.setField(conversation, "id", conversationId);
        UserSummaryResponse summary = new UserSummaryResponse(otherId, "B", null);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userMapper.toSummaryResponse(any(User.class))).thenReturn(summary);
        when(directMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.empty());
        when(directMessageRepository.countByConversationIdAndReceiverIdAndReadFalse(any(), any()))
                .thenReturn(0L);

        // when
        ConversationResponse result = conversationService.getConversation(currentUserId, conversationId);

        // then
        assertThat(result.id()).isEqualTo(conversationId);
        assertThat(result.with()).isSameAs(summary);
        assertThat(result.hasUnread()).isFalse();
    }

    @Test
    @DisplayName("비참여자가 단건 대화 조회 실패")
    void getConversation_notParticipant_throwsException() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        User currentUser = userWithId("a@mopl.com", "A", currentUserId);
        User participant1 = userWithId("b@mopl.com", "B", UUID.randomUUID());
        User participant2 = userWithId("c@mopl.com", "C", UUID.randomUUID());
        Conversation conversation = Conversation.create(participant1, participant2);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // when & then
        assertThatThrownBy(() -> conversationService.getConversation(currentUserId, conversationId))
                .isInstanceOf(NotConversationParticipantException.class);
    }

    @Test
    @DisplayName("대화가 없으면 단건 대화 조회 실패")
    void getConversation_conversationNotFound_throwsException() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        User currentUser = userWithId("a@mopl.com", "A", currentUserId);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> conversationService.getConversation(currentUserId, conversationId))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    @DisplayName("상대방과의 대화가 존재하면 조회 성공")
    void getConversationWith_exists_success() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID withUserId = UUID.randomUUID();
        User currentUser = userWithId("a@mopl.com", "A", currentUserId);
        User withUser = userWithId("b@mopl.com", "B", withUserId);
        Conversation conversation = Conversation.create(currentUser, withUser);
        ReflectionTestUtils.setField(conversation, "id", UUID.randomUUID());
        UserSummaryResponse summary = new UserSummaryResponse(withUserId, "B", null);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(conversationRepository.findByParticipant1IdAndParticipant2Id(any(), any()))
                .thenReturn(Optional.of(conversation));
        when(userMapper.toSummaryResponse(any(User.class))).thenReturn(summary);
        when(directMessageRepository.findTopByConversationIdOrderByCreatedAtDesc(any()))
                .thenReturn(Optional.empty());
        when(directMessageRepository.countByConversationIdAndReceiverIdAndReadFalse(any(), any()))
                .thenReturn(3L);

        // when
        ConversationResponse result = conversationService.getConversationWith(currentUserId, withUserId);

        // then
        assertThat(result.id()).isEqualTo(conversation.getId());
        assertThat(result.with()).isSameAs(summary);
        assertThat(result.hasUnread()).isTrue();
    }

    @Test
    @DisplayName("상대방과의 대화가 없으면 조회 실패")
    void getConversationWith_notFound_throwsException() {
        // given
        UUID currentUserId = UUID.randomUUID();
        UUID withUserId = UUID.randomUUID();
        User currentUser = userWithId("a@mopl.com", "A", currentUserId);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(conversationRepository.findByParticipant1IdAndParticipant2Id(any(), any()))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> conversationService.getConversationWith(currentUserId, withUserId))
                .isInstanceOf(ConversationNotFoundException.class);
    }
}
