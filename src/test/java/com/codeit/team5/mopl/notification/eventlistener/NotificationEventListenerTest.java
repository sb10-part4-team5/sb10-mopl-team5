package com.codeit.team5.mopl.notification.eventlistener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.repository.ContentRepository;
import com.codeit.team5.mopl.dm.event.DirectMessageNotificationEvent;
import com.codeit.team5.mopl.dm.fixture.DirectMessageTestFixtures;
import com.codeit.team5.mopl.follow.event.UserFollowedEvent;
import com.codeit.team5.mopl.follow.repository.FollowRepository;
import com.codeit.team5.mopl.notification.dto.request.NotificationBatchCreateCommand;
import com.codeit.team5.mopl.notification.dto.request.NotificationCreateCommand;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.service.NotificationService;
import com.codeit.team5.mopl.playlist.event.PlaylistContentAddEvent;
import com.codeit.team5.mopl.subscription.event.PlaylistSubscribedEvent;
import com.codeit.team5.mopl.subscription.repository.SubscriptionRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.event.RoleChangedEvent;
import com.codeit.team5.mopl.user.repository.UserRepository;
import com.codeit.team5.mopl.watcher.event.WatcherJoinedEvent;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    private DirectMessageNotificationEvent dmNotificationEvent(UUID receiverId, String senderName, String content) {
        return new DirectMessageNotificationEvent(
                DirectMessageTestFixtures.dmMessage(receiverId, senderName, content));
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
        verify(notificationService).create(eq(new NotificationCreateCommand(
                receiverId, NotificationType.DIRECT_MESSAGE,
                "[DM] 다린", "안녕하세요", NotificationLevel.INFO)));
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
        verify(notificationService).create(eq(new NotificationCreateCommand(
                receiverId, NotificationType.DIRECT_MESSAGE,
                "[DM] 다린", expectedContent, NotificationLevel.INFO)));
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
        verify(notificationService).create(eq(new NotificationCreateCommand(
                receiverId, NotificationType.PLAYLIST_SUBSCRIBED,
                "[플레이리스트] 내 플레이리스트",
                "다린 님이 플레이리스트를 구독하셨습니다.", NotificationLevel.INFO)));
    }

    @Test
    @DisplayName("플레이리스트 콘텐츠 추가 이벤트로 구독자 전원에게 배치로 PLAYLIST_UPDATED 알림을 생성한다")
    void onPlaylistContentAdd_createsNotificationForAllSubscribers() {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID subscriber1 = UUID.randomUUID();
        UUID subscriber2 = UUID.randomUUID();
        PlaylistContentAddEvent event =
                new PlaylistContentAddEvent(playlistId, "내 플레이리스트", "콘텐츠 제목");

        when(subscriptionRepository.findSubscriberIdsByPlaylistId(playlistId))
                .thenReturn(List.of(subscriber1, subscriber2));

        // when
        notificationEventListener.onPlaylistContentAdd(event);

        // then
        verify(notificationService).createAll(eq(new NotificationBatchCreateCommand(
                List.of(subscriber1, subscriber2), NotificationType.PLAYLIST_UPDATED,
                "[플레이리스트] 내 플레이리스트",
                "콘텐츠 제목이/가 추가되었습니다.", NotificationLevel.INFO)));
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
        verify(notificationService).create(eq(new NotificationCreateCommand(
                receiverId, NotificationType.FOLLOWED,
                "다린님이 나를 팔로우했어요.", "", NotificationLevel.INFO)));
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
        verify(notificationService).create(eq(new NotificationCreateCommand(
                receiverId, NotificationType.ROLE_CHANGED,
                "내 권한이 변경되었어요.",
                "내 권한이 [USER]에서 [ADMIN]로 변경되었어요.", NotificationLevel.INFO)));
    }

    @Test
    @DisplayName("시청 세션 생성 이벤트로 팔로워들에게 배치로 WATCHING_ACTIVITY 알림을 생성한다")
    void onWatchingSessionCreated_createsNotificationForFollowers() {
        // given
        UUID watcherUserId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        UUID follower1 = UUID.randomUUID();
        UUID follower2 = UUID.randomUUID();
        WatcherJoinedEvent event = new WatcherJoinedEvent(contentId, watcherUserId);

        User mockWatcher = mock(User.class);
        Content mockContent = mock(Content.class);
        
        when(mockWatcher.getId()).thenReturn(watcherUserId);
        when(mockWatcher.getName()).thenReturn("다린");
        when(mockContent.getTitle()).thenReturn("콘텐츠A");
        
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));
        when(userRepository.findById(watcherUserId)).thenReturn(Optional.of(mockWatcher));
        
        when(followRepository.findFollowerIdsByFolloweeId(watcherUserId))
                .thenReturn(List.of(follower1, follower2));

        // when
        notificationEventListener.onWatchingSessionCreated(event);

        // then
        verify(notificationService).createAll(eq(new NotificationBatchCreateCommand(
                List.of(follower1, follower2), NotificationType.WATCHING_ACTIVITY,
                "다린 님이 컨텐츠 시청중입니다.",
                "콘텐츠A 시청 중", NotificationLevel.INFO)));
    }

    @Test
    @DisplayName("시청 유저가 존재하지 않으면 알림을 생성하지 않고 종료한다")
    void onWatchingSessionCreated_returnsWhenUserNotFound() {
        // given
        UUID watcherUserId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        WatcherJoinedEvent event = new WatcherJoinedEvent(contentId, watcherUserId);
        Content mockContent = mock(Content.class);
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));
        when(userRepository.findById(watcherUserId)).thenReturn(Optional.empty());

        // when
        notificationEventListener.onWatchingSessionCreated(event);

        // then
        verify(notificationService, never()).createAll(any());
    }
}
