package com.codeit.team5.mopl.playlist.event;

import com.codeit.team5.mopl.notification.exception.InvalidNicknameException;
import com.codeit.team5.mopl.notification.exception.InvalidReceiverIdException;
import com.codeit.team5.mopl.playlist.exception.InvalidPlaylistNameException;
import java.util.UUID;
import org.springframework.util.StringUtils;

// 구독중인 플레이리스트가 수정되었을 때 발생되는 이벤트
public record PlaylistUpdatedEvent(
    UUID receiverId, // 알림 수신자의 ID
    String playlistName // 수정된 플레이리스트의 이름
) {
    public PlaylistUpdatedEvent{
        if (receiverId == null){
            throw new InvalidReceiverIdException();
        }
        if(!StringUtils.hasText(playlistName)){
            throw new InvalidPlaylistNameException();
        }
    }
}
