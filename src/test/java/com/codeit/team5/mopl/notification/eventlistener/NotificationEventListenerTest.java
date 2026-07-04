package com.codeit.team5.mopl.notification.eventlistener;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.DirectMessageNotificationEvent;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.follow.repository.FollowRepository;
import com.codeit.team5.mopl.user.dto.response.UserSummaryResponse;
import com.codeit.team5.mopl.watcher.event.WatchingSessionCreatedEvent;
import com.codeit.team5.mopl.user.event.RoleChangedEvent;
import com.codeit.team5.mopl.follow.event.UserFollowedEvent;
import com.codeit.team5.mopl.notification.service.NotificationService;
import java.time.Instant;
import com.codeit.team5.mopl.playlist.event.PlaylistSubscribedEvent;
import com.codeit.team5.mopl.playlist.event.PlaylistUpdatedEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    private DirectMessageNotificationEvent dmNotificationEvent(UUID receiverId, String senderName, String content) {
        UserSummaryResponse sender = new UserSummaryResponse(UUID.randomUUID(), senderName, null);
        UserSummaryResponse receiver = new UserSummaryResponse(receiverId, "받는이", null);
        DirectMessageResponse message = new DirectMessageResponse(
                UUID.randomUUID(), UUID.randomUUID(), sender, receiver, content, Instant.now());
        return new DirectMessageNotificationEvent(message);
    }

    @Test
    @DisplayName("비활성 DM 이벤트로 DIRECT_MESSAGE 알림을 생성한다")
    void onDirectMessageNotification_createsNotification() {
        // given
        UUID receiverId = UUID.randomUUID();
        DirectMessageNotificationEvent event = dmNotificationEvent(receiverId, "다린", "안녕하세요");

        // when
        notificationEventListener.onDirectMessageNotification(event);

        // then
        verify(notificationService).create(
                eq(receiverId), eq(NotificationType.DIRECT_MESSAGE),
                eq("[DM] 다린"), eq("안녕하세요"), eq(NotificationLevel.INFO));
    }

    @Test
    @DisplayName("DM 내용이 50자를 초과하면 잘라서 알림을 생성한다")
    void onDirectMessageNotification_truncatesContent() {
        // given
        UUID receiverId = UUID.randomUUID();
        String longContent = "가".repeat(60);
        String expectedContent = "가".repeat(50);
        DirectMessageNotificationEvent event = dmNotificationEvent(receiverId, "다린", longContent);

        // when
        notificationEventListener.onDirectMessageNotification(event);

        // then
        verify(notificationService).create(
                eq(receiverId), eq(NotificationType.DIRECT_MESSAGE),
                eq("[DM] 다린"), eq(expectedContent), eq(NotificationLevel.INFO));
    }

    @Test
    @DisplayName("플레이리스트 구독 이벤트로 PLAYLIST_SUBSCRIBED 알림을 생성한다")
    void onPlaylistSubscribe_createsNotification() {
        // given
        UUID receiverId = UUID.randomUUID();
        PlaylistSubscribedEvent event =
                new PlaylistSubscribedEvent(receiverId, "다린", "내 플레이리스트");

        // when
        notificationEventListener.onPlaylistSubscribe(event);

        // then
        verify(notificationService).create(
                eq(receiverId), eq(NotificationType.PLAYLIST_SUBSCRIBED),
                eq("[플레이리스트] 내 플레이리스트"),
                eq("다린 님이 플레이리스트를 구독하셨습니다."), eq(NotificationLevel.INFO));
    }

    @Test
    @DisplayName("플레이리스트 수정 이벤트로 PLAYLIST_UPDATED 알림을 생성한다")
    void onPlaylistUpdated_createsNotification() {
        // given
        UUID receiverId = UUID.randomUUID();
        PlaylistUpdatedEvent event =
                new PlaylistUpdatedEvent(receiverId, "내 플레이리스트");

        // when
        notificationEventListener.onPlaylistUpdated(event);

        // then
        verify(notificationService).create(
                eq(receiverId), eq(NotificationType.PLAYLIST_UPDATED),
                eq("[플레이리스트] 내 플레이리스트"),
                eq("플레이리스트가 업데이트 되었습니다."), eq(NotificationLevel.INFO));
    }

    @Test
    @DisplayName("팔로우 이벤트로 FOLLOWED 알림을 생성한다")
    void onUserFollowed_createsNotification() {
        // given
        UUID receiverId = UUID.randomUUID();
        UserFollowedEvent event = new UserFollowedEvent(receiverId, "다린");

        // when
        notificationEventListener.onUserFollowed(event);

        // then
        verify(notificationService).create(
                eq(receiverId), eq(NotificationType.FOLLOWED),
                eq("다린님이 나를 팔로우했어요."), eq(""), eq(NotificationLevel.INFO));
    }

    @Test
    @DisplayName("권한 변경 이벤트로 ROLE_CHANGED 알림을 생성한다")
    void onRoleChanged_createsNotification() {
        // given
        UUID receiverId = UUID.randomUUID();
        RoleChangedEvent event = new RoleChangedEvent(receiverId, "USER", "ADMIN");

        // when
        notificationEventListener.onRoleChanged(event);

        // then
        verify(notificationService).create(
                eq(receiverId), eq(NotificationType.ROLE_CHANGED),
                eq("내 권한이 변경되었어요."),
                eq("내 권한이 [USER]에서 [ADMIN]로 변경되었어요."), eq(NotificationLevel.INFO));
    }

    @Test
    @DisplayName("시청 세션 생성 이벤트로 팔로워들에게 WATCHING_ACTIVITY 알림을 생성한다")
    void onWatchingSessionCreated_createsNotificationForFollowers() {
        // given
        UUID watcherUserId = UUID.randomUUID();
        UUID follower1 = UUID.randomUUID();
        UUID follower2 = UUID.randomUUID();
        WatchingSessionCreatedEvent event =
                new WatchingSessionCreatedEvent(watcherUserId, "다린", "콘텐츠A");

        when(followRepository.findFollowerIdsByFolloweeId(watcherUserId))
                .thenReturn(List.of(follower1, follower2));

        // when
        notificationEventListener.onWatchingSessionCreated(event);

        // then
        verify(notificationService).create(
                eq(follower1), eq(NotificationType.WATCHING_ACTIVITY),
                eq("다린 님이 컨텐츠 시청중입니다."),
                eq("콘텐츠A 시청 중"), eq(NotificationLevel.INFO));
        verify(notificationService).create(
                eq(follower2), eq(NotificationType.WATCHING_ACTIVITY),
                eq("다린 님이 컨텐츠 시청중입니다."),
                eq("콘텐츠A 시청 중"), eq(NotificationLevel.INFO));
    }
}
