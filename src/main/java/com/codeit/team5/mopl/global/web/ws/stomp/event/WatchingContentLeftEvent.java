package com.codeit.team5.mopl.global.web.ws.stomp.event;

import java.util.UUID;

public record WatchingContentLeftEvent(UUID contentId, UUID watcherId) {

}
