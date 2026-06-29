package com.codeit.team5.mopl.notification.eventlistener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.follow.event.UserFollowedEvent;
import com.codeit.team5.mopl.notification.service.NotificationService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class NotificationEventListenerIntegrationTest {

    @MockitoBean                       // create 호출 여부만 검증 (실제 저장은 안 함)
    NotificationService notificationService;
    @Autowired
    ApplicationEventPublisher publisher;
    @Autowired
    TransactionTemplate tx;  // 트랜잭션 경계를 직접 제어

    @Test
    void notificationSuccess_afterCommit() {
        UUID receiverId = UUID.randomUUID();
        tx.executeWithoutResult(status -> {
            publisher.publishEvent(new UserFollowedEvent(receiverId, "다린"));
            verifyNoInteractions(notificationService); // 트랜잭션 블록 내에서 상호작용이 없음을 확인함.
        }); // ← 여기서 커밋
        verify(notificationService).create(
            eq(receiverId), eq(NotificationType.FOLLOWED),
            eq("다린" + "님이 나를 팔로우했어요."),
            eq(""),
            eq(NotificationLevel.INFO)
        );   // 커밋 후 호출 확인
    }

    private void verifyNoInteractions(NotificationService notificationService) {
    }

    @Test
    void notificationFail_whenRollback() {
        UUID receiverId = UUID.randomUUID();
        tx.executeWithoutResult(status -> {
            publisher.publishEvent(new UserFollowedEvent(receiverId, "다린"));
            status.setRollbackOnly();              // 강제 롤백
        });
        verify(notificationService, never()).create(
            any(), any(), any(), any(), any()
        );   // 호출 안 됨 확인
    }
}
