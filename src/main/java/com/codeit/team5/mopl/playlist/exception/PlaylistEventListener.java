package com.codeit.team5.mopl.playlist.exception;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import com.codeit.team5.mopl.playlist.event.UserDeletedEvent;
import com.codeit.team5.mopl.playlist.service.PlaylistService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlaylistEventListener {
    private final PlaylistService service;

    @TransactionalEventListener
    public void handle(UserDeletedEvent event) {

    }
}
