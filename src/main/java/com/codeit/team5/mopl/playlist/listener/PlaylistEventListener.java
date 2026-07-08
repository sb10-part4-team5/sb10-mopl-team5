package com.codeit.team5.mopl.playlist.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import com.codeit.team5.mopl.playlist.repository.PlaylistRepository;
import com.codeit.team5.mopl.user.event.UserLockedEvent;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlaylistEventListener {

    private final PlaylistRepository repository;

    @Async("outboxEventWorker")
    @Transactional
    @TransactionalEventListener
    public void handle(UserLockedEvent event) {
        if (event.locked()) {
            repository.bulkDecreaseSubscribeCountBySubscriberId(event.id());
            return;
        }
        repository.bulkIncreaseSubscribeCountBySubscriberId(event.id());
    }
}
