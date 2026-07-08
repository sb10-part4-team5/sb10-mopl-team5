package com.codeit.team5.mopl.global.web.ws.stomp.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.auth.jwt.JwtAuthenticationService;
import com.codeit.team5.mopl.auth.jwt.JwtTokenizer;
import com.codeit.team5.mopl.global.web.ws.stomp.store.WebSocketSessionStore;
import com.codeit.team5.mopl.watcher.service.WatchingSessionCommandService;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class BaseStompInboundChannelTest {

    @Autowired
    @Qualifier("clientInboundChannel")
    protected MessageChannel clientInboundChannel;

    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    @MockitoBean
    protected JwtTokenizer jwtTokenizer;

    @MockitoBean
    protected JwtAuthenticationService jwtAuthenticationService;

    @MockitoBean
    protected WebSocketSessionStore sessionStore;

    @MockitoBean
    protected WatchingSessionCommandService watchingSessionService;
}
