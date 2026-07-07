package com.codeit.team5.mopl.playlist.event;

import com.codeit.team5.mopl.content.exception.InvalidContentTitleException;
import com.codeit.team5.mopl.notification.exception.InvalidReceiverIdException;
import com.codeit.team5.mopl.playlist.exception.InvalidPlaylistNameException;
import java.util.List;
import java.util.UUID;
import org.springframework.util.StringUtils;

// 구독중인 플레이리스트에 콘텐츠가 추가되었을 때 발행되는 이벤트
public record PlaylistContentAddEvent(
    List<UUID> receiverIds, // 알림 수신자의 ID 목록
    String playlistName, // 수정된 플레이리스트의 이름
    String contentTitle // 추가된 콘텐츠의 타이틀
) {
    public PlaylistContentAddEvent {
        if (receiverIds == null){
            throw new InvalidReceiverIdException();
        }
        if(!StringUtils.hasText(playlistName)){
            throw new InvalidPlaylistNameException();
        }
        if(!StringUtils.hasText(contentTitle)){
            throw new InvalidContentTitleException();
        }
    }
}
