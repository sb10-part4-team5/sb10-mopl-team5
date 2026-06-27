package com.codeit.team5.mopl.notification.eventlistener;

import com.codeit.team5.mopl.notification.entity.NotificationLevel;
import com.codeit.team5.mopl.notification.entity.NotificationType;
import com.codeit.team5.mopl.notification.event.DirectMessageSentEvent;
import com.codeit.team5.mopl.notification.event.FollowingUserWatchingEvent;
import com.codeit.team5.mopl.notification.event.PlaylistSubscribedEvent;
import com.codeit.team5.mopl.notification.event.PlaylistUpdatedEvent;
import com.codeit.team5.mopl.notification.event.RoleChangedEvent;
import com.codeit.team5.mopl.notification.event.UserFollowedEvent;
import com.codeit.team5.mopl.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 여러 도메인에서 알림 생성이 트리거 될 때 작업을 수행하는 이벤트 리스너입니다.
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private final NotificationService notificationService;

    // DM을 수신받으면 알림을 생성
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDirectMessageSent(DirectMessageSentEvent event){
        // 알림의 제목
        String title = "[DM] " + event.senderNickname();
        // DM 내용이 길면 50자로 줄여서 보여줌
        String content = StringUtils.truncate(event.content(), 50);
        notificationService.create(event.receiverId(), NotificationType.DIRECT_MESSAGE,
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

    // 내가 팔로우한 사용자가 시청을 할 때 알림 생성
    // WatchingSession 서비스 계층에서 FollowingUserWatchingEvent를 발행하면, 이벤트 리스닝함
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void onFollowingUserActivity(FollowingUserWatchingEvent event){
        String title = event.userNickname() + " 님이 컨텐츠 시청중입니다.";
        String content = event.contentName() + " 시청 중";

        notificationService.create(event.receiverId(), NotificationType.WATCHING_ACTIVITY,
            title, content, NotificationLevel.INFO);
    }


}
