package com.codeit.team5.mopl.watcher.dto.response;

import java.util.UUID;

public record WatcherResponse(UUID userId, String name, String profileImageUrl) {

}
