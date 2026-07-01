package com.codeit.team5.mopl.dm.service;

import com.codeit.team5.mopl.dm.dto.request.DirectMessageCursorRequest;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.entity.DirectMessage;
import com.codeit.team5.mopl.dm.event.DirectMessageBroadcastEvent;
import com.codeit.team5.mopl.dm.exception.ConversationNotFoundException;
import com.codeit.team5.mopl.dm.exception.DirectMessageNotFoundException;
import com.codeit.team5.mopl.dm.mapper.DmMapper;
import com.codeit.team5.mopl.dm.repository.ConversationRepository;
import com.codeit.team5.mopl.dm.repository.DirectMessageRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.web.ws.stomp.constant.StompConstants;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.notification.event.DirectMessageSentEvent;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
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
public class DirectMessageService {

    private final DirectMessageRepository directMessageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final DmMapper dmMapper;
    private final WebSocketSessionStore webSocketSessionStore;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DirectMessageResponse sendMessage(String senderEmail, UUID conversationId, String content) {
        User sender = getUserByEmail(senderEmail);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        DirectMessage message = directMessageRepository.save(
                DirectMessage.create(conversation, sender, content));
        DirectMessageResponse response = dmMapper.toResponse(message);

        eventPublisher.publishEvent(new DirectMessageBroadcastEvent(conversationId, response));

        // 수신자가 대화방을 보고 있지 않을 때(비활성)만 알림
        User receiver = message.getReceiver();
        String destination = StompConstants.conversationDmDestination(conversationId);
        if (!webSocketSessionStore.isSubscribed(receiver.getEmail(), destination)) {
            eventPublisher.publishEvent(new DirectMessageSentEvent(
                    receiver.getId(), sender.getName(), content));
        }

        log.info("DM sent: conversationId={}, senderId={}", conversationId, sender.getId());
        return response;
    }

    public CursorResponse<DirectMessageResponse> getMessages(UUID currentUserId, UUID conversationId,
            DirectMessageCursorRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        conversation.validateParticipant(currentUserId);

        List<DirectMessage> fetched = directMessageRepository.findMessages(conversationId, request);
        boolean hasNext = fetched.size() > request.limit();
        List<DirectMessage> page = hasNext ? fetched.subList(0, request.limit()) : fetched;
        long totalCount = directMessageRepository.countMessages(conversationId);

        return dmMapper.toDirectMessageCursor(page, hasNext, totalCount, request.sortDirection());
    }

    @Transactional
    public void markMessagesAsRead(UUID currentUserId, UUID conversationId, UUID directMessageId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        conversation.validateParticipant(currentUserId);
        DirectMessage message = directMessageRepository.findById(directMessageId)
                .orElseThrow(() -> new DirectMessageNotFoundException(directMessageId));
        if (!message.getConversation().getId().equals(conversationId)) {
            throw new DirectMessageNotFoundException(directMessageId);
        }
        directMessageRepository.markAsReadUntil(
                conversationId, currentUserId, message.getCreatedAt(), Instant.now());
        log.info("DM marked as read: conversationId={}, userId={}, untilMessageId={}",
                conversationId, currentUserId, directMessageId);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }
}
