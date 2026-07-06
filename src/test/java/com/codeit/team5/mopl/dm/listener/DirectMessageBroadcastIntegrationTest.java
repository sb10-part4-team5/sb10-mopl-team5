package com.codeit.team5.mopl.dm.listener;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.dm.entity.Conversation;
import com.codeit.team5.mopl.dm.repository.ConversationRepository;
import com.codeit.team5.mopl.dm.repository.DirectMessageRepository;
import com.codeit.team5.mopl.dm.service.DirectMessageService;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class DirectMessageBroadcastIntegrationTest {

    @Autowired
    private DirectMessageService directMessageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private DirectMessageRepository directMessageRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @AfterEach
    void cleanup() {
        directMessageRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("메시지 전송 커밋 후 STOMP 브로드캐스트 성공")
    void sendMessage_broadcastsAfterCommit() {
        User sender = userRepository.save(User.create("sender@mopl.com", "password", "보낸이"));
        User receiver = userRepository.save(User.create("receiver@mopl.com", "password", "받는이"));
        Conversation conversation = conversationRepository.save(Conversation.create(sender, receiver));
        UUID conversationId = conversation.getId();

        directMessageService.sendMessage(sender.getEmail(), conversationId, "hello");

        verify(messagingTemplate, timeout(2000)).convertAndSend(
                eq("/sub/conversations/" + conversationId + "/direct-messages"),
                ArgumentMatchers.<Object>any());
    }
}
