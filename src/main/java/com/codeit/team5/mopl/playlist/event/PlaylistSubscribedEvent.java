package com.codeit.team5.mopl.playlist.event;

import com.codeit.team5.mopl.notification.exception.InvalidNicknameException;
import com.codeit.team5.mopl.notification.exception.InvalidReceiverIdException;
import com.codeit.team5.mopl.playlist.exception.InvalidPlaylistNameException;
import java.util.UUID;
import org.springframework.util.StringUtils;

public record PlaylistSubscribedEvent(
    UUID receiverId,
    String subscriberNickname,
    String playlistName
) {
    public PlaylistSubscribedEvent{
        if (receiverId == null){
            throw new InvalidReceiverIdException();
        }
        if(!StringUtils.hasText(subscriberNickname)){
            throw new InvalidNicknameException();
        }
        if(!StringUtils.hasText(playlistName)){
            throw new InvalidPlaylistNameException();
        }
    }
}
