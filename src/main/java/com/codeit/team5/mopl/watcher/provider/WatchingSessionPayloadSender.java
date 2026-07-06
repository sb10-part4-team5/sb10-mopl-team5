package com.codeit.team5.mopl.watcher.provider;

import java.util.UUID;

public interface WatchingSessionPayloadSender {

    void send(UUID targetId, Object payload);
}
