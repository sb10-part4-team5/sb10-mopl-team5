package com.codeit.team5.mopl.notification.eventlistener;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.DirectMessageNotificationEvent;
import com.codeit.team5.mopl.follow.repository.FollowRepository;
import com.codeit.team5.mopl.notification.dto.request.NotificationBatchCreateCommand;
import com.codeit.team5.mopl.subscription.repository.SubscriptionRepository;
import com.codeit.team5.mopl.notification.dto.request.NotificationCreateCommand;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import com.codeit.team5.mopl.watcher.event.WatchingSessionCreatedEvent;
import com.codeit.team5.mopl.user.event.RoleChangedEvent;
import com.codeit.team5.mopl.follow.event.UserFollowedEvent;
import com.codeit.team5.mopl.notification.service.NotificationService;
import com.codeit.team5.mopl.subscription.event.PlaylistSubscribedEvent;
import com.codeit.team5.mopl.playlist.event.PlaylistContentAddEvent;
import com.codeit.team5.mopl.watcher.exception.WatchingSessionNotFoundException;
import com.codeit.team5.mopl.watcher.repository.WatchingSessionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 여러 도메인에서 알림 생성이 트리거 될 때 작업을 수행하는 이벤트 리스너입니다.
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;
    private final FollowRepository followRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WatchingSessionRepository watchingSessionRepository;

    // 비활성 대화에 DM이 도착하면 알림을 생성 (SSE 전송과는 독립)
    @Async("dmEventExecutor")
    @EventListener
    public void onDirectMessageNotification(DirectMessageNotificationEvent event){
        DirectMessageResponse message = event.message();
        notificationService.create(new NotificationCreateCommand(
                message.receiver().userId(), NotificationType.DIRECT_MESSAGE,
                "[DM] " + message.sender().name(),
                StringUtils.truncate(message.content(), 50),
                NotificationLevel.INFO));
    }

    // 다른 사용자가 내 플레이리스트 구독 시 알림 생성
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPlaylistSubscribe(PlaylistSubscribedEvent event){
        notificationService.create(new NotificationCreateCommand(
                event.receiverId(), NotificationType.PLAYLIST_SUBSCRIBED,
                "[플레이리스트] " + event.playlistName(),
                event.subscriberNickname() + " 님이 플레이리스트를 구독하셨습니다.",
                NotificationLevel.INFO));
    }

    // 내가 구독한 플레이리스트에 콘텐츠가 추가되면 구독자 전원에게 알림 배치 생성 (fan-out)
    // [계약] 구독자 목록은 리스너 실행 시점(AFTER_COMMIT) 기준으로 조회합니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPlaylistContentAdd(PlaylistContentAddEvent event) {
        List<UUID> subscriberIds = subscriptionRepository.findSubscriberIdsByPlaylistId(event.playlistId());
        notificationService.createAll(new NotificationBatchCreateCommand(
                subscriberIds, NotificationType.PLAYLIST_UPDATED,
                "[플레이리스트] " + event.playlistName(),
                event.contentTitle() + "이/가 추가되었습니다.",
                NotificationLevel.INFO));
    }

    // 다른 유저가 나를 팔로우 했을 때 트리거되는 이벤트를 받고 알림 생성
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserFollowed(UserFollowedEvent event){
        notificationService.create(new NotificationCreateCommand(
                event.receiverId(), NotificationType.FOLLOWED,
                event.userName() + "님이 나를 팔로우했어요.",
                "",
                NotificationLevel.INFO));
    }

    // ADMIN이 나의 권한을 변경했을 때 알림 생성
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoleChanged(RoleChangedEvent event){
        notificationService.create(new NotificationCreateCommand(
                event.receiverId(), NotificationType.ROLE_CHANGED,
                "내 권한이 변경되었어요.",
                "내 권한이 [" + event.roleBefore() + "]에서 [" + event.roleAfter() + "]로 변경되었어요.",
                NotificationLevel.INFO));
    }

    // 시청 세션이 생성되면 해당 유저의 팔로워들에게 알림 생성 (fan-out)
    // [계약] 팔로워 목록은 리스너 실행 시점(AFTER_COMMIT) 기준으로 조회합니다.
    // 시청 시작과 리스너 실행 사이에 follow/unfollow가 발생하면 결과가 달라질 수 있으나,
    // 시청 알림 특성상 약간의 오차는 허용되는 것으로 간주합니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void onWatchingSessionCreated(WatchingSessionCreatedEvent event){
        WatchingSession watchingSession = watchingSessionRepository.findByWatcherId(event.watcherId()).orElseThrow(() -> new WatchingSessionNotFoundException(
            Map.of("watcherId", event.watcherId())));

        String contentTitle = watchingSession.getContent().getTitle();
        User watcher = watchingSession.getWatcher();

        List<UUID> followerIds = followRepository.findFollowerIdsByFolloweeId(watcher.getId());
        notificationService.createAll(new NotificationBatchCreateCommand(
                followerIds, NotificationType.WATCHING_ACTIVITY,
                watcher.getName() + " 님이 컨텐츠 시청중입니다.",
                contentTitle + " 시청 중",
                NotificationLevel.INFO));
    }


}
