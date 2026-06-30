package com.codeit.team5.mopl.dm.service;

import com.codeit.team5.mopl.dm.dto.response.ConversationResponse;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.dm.exception.ConversationNotFoundException;
import com.codeit.team5.mopl.dm.mapper.DmMapper;
import com.codeit.team5.mopl.dm.repository.ConversationRepository;
import com.codeit.team5.mopl.dm.repository.DirectMessageRepository;
import com.codeit.team5.mopl.global.util.UuidUtils;
import com.codeit.team5.mopl.notification.event.DirectMessageSentEvent;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class DmService {

    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final DmMapper dmMapper;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ConversationResponse getOrCreateConversation(UUID currentUserId, UUID otherParticipantId) {
        User currentUser = getUser(currentUserId);
        User otherParticipant = getUser(otherParticipantId);

        UUID participant1Id = UuidUtils.compareUnsigned(currentUserId, otherParticipantId) < 0
                ? currentUserId : otherParticipantId;
        UUID participant2Id = UuidUtils.compareUnsigned(currentUserId, otherParticipantId) < 0
                ? otherParticipantId : currentUserId;

        Conversation conversation = conversationRepository
                .findByParticipant1IdAndParticipant2Id(participant1Id, participant2Id)
                .orElseGet(() -> conversationRepository.save(Conversation.create(currentUser, otherParticipant)));

        return toConversationResponse(conversation, currentUser);
    }

    @Transactional
    public DirectMessageResponse sendMessage(String senderEmail, UUID conversationId, String content) {
        User sender = getUserByEmail(senderEmail);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        DirectMessage message = directMessageRepository.save(
                DirectMessage.create(conversation, sender, content));
        DirectMessageResponse response = dmMapper.toResponse(message);

        eventPublisher.publishEvent(new DirectMessageBroadcastEvent(conversationId, response));
        eventPublisher.publishEvent(new DirectMessageSentEvent(
                message.getReceiver().getId(), sender.getName(), content));

        log.info("DM sent: conversationId={}, senderId={}", conversationId, sender.getId());
        return response;
    }

    public void validateParticipant(UUID conversationId, String email) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        conversation.validateParticipant(getUserByEmail(email).getId());
    }

    private ConversationResponse toConversationResponse(Conversation conversation, User currentUser) {
        UserSummaryResponse with = userMapper.toSummaryResponse(conversation.getOtherParticipant(currentUser));
        DirectMessageResponse lastestMessage = directMessageRepository
                .findTopByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .map(dmMapper::toResponse)
                .orElse(null);
        boolean hasUnread = directMessageRepository
                .countByConversationIdAndReceiverIdAndReadFalse(conversation.getId(), currentUser.getId()) > 0;
        return new ConversationResponse(conversation.getId(), with, lastestMessage, hasUnread);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }
}
