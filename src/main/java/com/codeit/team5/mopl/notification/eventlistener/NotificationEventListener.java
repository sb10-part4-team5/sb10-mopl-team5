package com.codeit.team5.mopl.notification.eventlistener;

import com.codeit.team5.mopl.dm.dto.response.DirectMessageResponse;
import com.codeit.team5.mopl.dm.event.DirectMessageNotificationEvent;
import com.codeit.team5.mopl.follow.repository.FollowRepository;
import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.watcher.event.WatchingSessionCreatedEvent;
import com.codeit.team5.mopl.user.event.RoleChangedEvent;
import com.codeit.team5.mopl.follow.event.UserFollowedEvent;
import com.codeit.team5.mopl.notification.service.NotificationService;
import com.codeit.team5.mopl.playlist.event.PlaylistSubscribedEvent;
import com.codeit.team5.mopl.playlist.event.PlaylistUpdatedEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 여러 도메인에서 알림 생성이 트리거 될 때 작업을 수행하는 이벤트 리스너입니다.
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;
    private final FollowRepository followRepository;

    // 비활성 대화에 DM이 도착하면 알림을 생성 (SSE 전송과는 독립)
    @Async("dmEventExecutor")
    @EventListener
    public void onDirectMessageNotification(DirectMessageNotificationEvent event){
        DirectMessageResponse message = event.message();
        // 알림의 제목
        String title = "[DM] " + message.sender().name();
        // DM 내용이 길면 50자로 줄여서 보여줌
        String content = StringUtils.truncate(message.content(), 50);
        notificationService.create(message.receiver().userId(), NotificationType.DIRECT_MESSAGE,
            title, content, NotificationLevel.INFO);
    }

    // 다른 사용자가 내 플레이리스트 구독 시 알림 생성
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPlaylistSubscribe(PlaylistSubscribedEvent event){
        String title = "[플레이리스트] " +  event.playlistName();
        String content = event.subscriberNickname() + " 님이 플레이리스트를 구독하셨습니다.";
        notificationService.create(event.receiverId(), NotificationType.PLAYLIST_SUBSCRIBED,
            title, content, NotificationLevel.INFO);
    }

    // 내가 구독한 플레이리스트가 업데이트 되면 알림 생성
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPlaylistUpdated(PlaylistUpdatedEvent event){
        String title = "[플레이리스트] " +  event.playlistName();
        String content =  "플레이리스트가 업데이트 되었습니다.";
        notificationService.create(event.receiverId(), NotificationType.PLAYLIST_UPDATED,
            title, content, NotificationLevel.INFO);
    }

    // 다른 유저가 나를 팔로우 했을 때 트리거되는 이벤트를 받고 알림 생성
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserFollowed(UserFollowedEvent event){
        String title = event.userName() + "님이 나를 팔로우했어요.";
        String content = "";
        notificationService.create(event.receiverId(), NotificationType.FOLLOWED,
            title, content, NotificationLevel.INFO);
    }

    // ADMIN이 나의 권한을 변경했을 때 알림 생성
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoleChanged(RoleChangedEvent event){
        String title = "내 권한이 변경되었어요.";
        String content = "내 권한이 ["+ event.roleBefore() + "]에서 [" + event.roleAfter() +"]로 변경되었어요." ;
        notificationService.create(event.receiverId(), NotificationType.ROLE_CHANGED,
            title, content, NotificationLevel.INFO);
    }

    // 시청 세션이 생성되면 해당 유저의 팔로워들에게 알림 생성 (fan-out)
    // [계약] 팔로워 목록은 리스너 실행 시점(AFTER_COMMIT) 기준으로 조회합니다.
    // 시청 시작과 리스너 실행 사이에 follow/unfollow가 발생하면 결과가 달라질 수 있으나,
    // 시청 알림 특성상 약간의 오차는 허용되는 것으로 간주합니다.
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void onWatchingSessionCreated(WatchingSessionCreatedEvent event){
        String title = event.watcherNickname() + " 님이 컨텐츠 시청중입니다.";
        String content = event.contentName() + " 시청 중";

        List<UUID> followerIds = followRepository.findFollowerIdsByFolloweeId(event.watcherUserId());
        for (UUID followerId : followerIds) {
            notificationService.create(followerId, NotificationType.WATCHING_ACTIVITY,
                title, content, NotificationLevel.INFO);
        }
    }


}
