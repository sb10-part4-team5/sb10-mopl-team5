package com.codeit.team5.mopl.watcher.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WatchingSessionCreatedRequest(@NotNull UUID watcherId,
                                            @NotNull UUID contentId) {

}
