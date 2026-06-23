package com.codeit.team5.mopl.watcher.dto;

import java.util.UUID;

public record WatchingSessionCreatedRequest(UUID watcherId, UUID contentId) {

}
