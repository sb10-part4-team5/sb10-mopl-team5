package com.codeit.team5.mopl.dm.service;

import com.codeit.team5.mopl.dm.dto.response.ConversationResponse;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.exception.ConversationNotFoundException;
import com.codeit.team5.mopl.dm.mapper.DmMapper;
import com.codeit.team5.mopl.dm.repository.ConversationRepository;
import com.codeit.team5.mopl.dm.repository.DirectMessageRepository;
import com.codeit.team5.mopl.global.util.UuidUtils;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final DmMapper dmMapper;

    @Transactional
    public ConversationResponse getOrCreateConversation(UUID currentUserId, UUID otherParticipantId) {
        User currentUser = getUser(currentUserId);
        User otherParticipant = getUser(otherParticipantId);

        UUID participant1Id = UuidUtils.min(currentUserId, otherParticipantId);
        UUID participant2Id = UuidUtils.max(currentUserId, otherParticipantId);

        Conversation conversation = conversationRepository
                .findByParticipant1IdAndParticipant2Id(participant1Id, participant2Id)
                .orElseGet(() -> conversationRepository.save(Conversation.create(currentUser, otherParticipant)));

        return toConversationResponse(conversation, currentUser);
    }

    public ConversationResponse getConversation(UUID currentUserId, UUID conversationId) {
        User currentUser = getUser(currentUserId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        conversation.validateParticipant(currentUserId);
        return toConversationResponse(conversation, currentUser);
    }

    public ConversationResponse getConversationWith(UUID currentUserId, UUID withUserId) {
        User currentUser = getUser(currentUserId);
        UUID participant1Id = UuidUtils.min(currentUserId, withUserId);
        UUID participant2Id = UuidUtils.max(currentUserId, withUserId);
        Conversation conversation = conversationRepository
                .findByParticipant1IdAndParticipant2Id(participant1Id, participant2Id)
                .orElseThrow(() -> new ConversationNotFoundException(null));
        return toConversationResponse(conversation, currentUser);
    }

    public void validateParticipant(UUID conversationId, String email) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        conversation.validateParticipant(user.getId());
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
}
