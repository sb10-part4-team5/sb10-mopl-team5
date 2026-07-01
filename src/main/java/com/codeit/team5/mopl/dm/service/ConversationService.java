package com.codeit.team5.mopl.dm.service;

import com.codeit.team5.mopl.dm.dto.request.ConversationCursorRequest;
import com.codeit.team5.mopl.dm.dto.response.ConversationResponse;
import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.exception.ConversationNotFoundException;
import com.codeit.team5.mopl.dm.mapper.DmMapper;
import com.codeit.team5.mopl.dm.repository.ConversationRepository;
import com.codeit.team5.mopl.dm.repository.DirectMessageRepository;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.global.util.UuidUtils;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort.Direction;
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

        UUID[] participants = UuidUtils.sorted(currentUserId, otherParticipantId);

        Conversation conversation = conversationRepository
                .findByParticipant1IdAndParticipant2Id(participants[0], participants[1])
                .orElseGet(() -> conversationRepository.save(Conversation.create(currentUser, otherParticipant)));

        log.info("Conversation ready: id={}, currentUserId={}", conversation.getId(), currentUserId);
        return toConversationResponse(conversation, currentUser);
    }

    public ConversationResponse getConversation(UUID currentUserId, UUID conversationId) {
        User currentUser = getUser(currentUserId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        conversation.validateParticipant(currentUserId);
        return toConversationResponse(conversation, currentUser);
    }

    public CursorResponse<ConversationResponse> findMyConversations(UUID currentUserId, ConversationCursorRequest request) {
        User currentUser = getUser(currentUserId);
        List<Conversation> fetched = conversationRepository.findMyConversations(currentUserId, request);
        boolean hasNext = fetched.size() > request.limit();
        List<Conversation> page = hasNext ? fetched.subList(0, request.limit()) : fetched;

        List<ConversationResponse> data = toConversationResponses(page, currentUser);

        Conversation last = page.isEmpty() ? null : page.get(page.size() - 1);
        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext && last != null) {
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId().toString();
        }
        String direction = request.sortDirection() == Direction.ASC ? "ASCENDING" : "DESCENDING";
        return new CursorResponse<>(data, nextCursor, nextIdAfter, hasNext, 0L, "createdAt", direction);
    }

    public ConversationResponse getConversationWith(UUID currentUserId, UUID withUserId) {
        User currentUser = getUser(currentUserId);
        UUID[] participants = UuidUtils.sorted(currentUserId, withUserId);
        Conversation conversation = conversationRepository
                .findByParticipant1IdAndParticipant2Id(participants[0], participants[1])
                .orElseThrow(() -> ConversationNotFoundException.withUser(withUserId));
        return toConversationResponse(conversation, currentUser);
    }

    public void validateParticipant(UUID conversationId, String email) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        conversation.validateParticipant(user.getId());
    }

    private List<ConversationResponse> toConversationResponses(List<Conversation> conversations, User currentUser) {
        if (conversations.isEmpty()) {
            return List.of();
        }
        List<UUID> conversationIds = conversations.stream().map(Conversation::getId).toList();

        Map<UUID, DirectMessageResponse> latestByConversation = directMessageRepository
                .findLatestMessagesByConversationIds(conversationIds).stream()
                .collect(Collectors.toMap(
                        message -> message.getConversation().getId(),
                        dmMapper::toResponse,
                        (existing, ignored) -> existing));

        Set<UUID> unreadConversationIds = Set.copyOf(
                directMessageRepository.findConversationIdsWithUnread(conversationIds, currentUser.getId()));

        return conversations.stream()
                .map(conversation -> new ConversationResponse(
                        conversation.getId(),
                        userMapper.toSummaryResponse(conversation.getOtherParticipant(currentUser)),
                        latestByConversation.get(conversation.getId()),
                        unreadConversationIds.contains(conversation.getId())))
                .toList();
    }

    private ConversationResponse toConversationResponse(Conversation conversation, User currentUser) {
        UserSummaryResponse with = userMapper.toSummaryResponse(conversation.getOtherParticipant(currentUser));
        DirectMessageResponse latestMessage = directMessageRepository
                .findTopByConversationIdOrderByCreatedAtDescIdDesc(conversation.getId())
                .map(dmMapper::toResponse)
                .orElse(null);
        boolean hasUnread = directMessageRepository
                .existsByConversationIdAndReceiverIdAndReadFalse(conversation.getId(), currentUser.getId());
        return new ConversationResponse(conversation.getId(), with, latestMessage, hasUnread);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
