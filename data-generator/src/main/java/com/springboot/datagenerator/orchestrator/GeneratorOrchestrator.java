package com.springboot.datagenerator.orchestrator;

import com.springboot.datagenerator.generator.ContentGenerator;
import com.springboot.datagenerator.generator.ContentTagGenerator;
import com.springboot.datagenerator.generator.FollowGenerator;
import com.springboot.datagenerator.generator.NotificationGenerator;
import com.springboot.datagenerator.generator.PlaylistGenerator;
import com.springboot.datagenerator.generator.PlaylistItemGenerator;
import com.springboot.datagenerator.generator.PlaylistSubscriptionGenerator;
import com.springboot.datagenerator.generator.ReviewGenerator;
import com.springboot.datagenerator.generator.TagGenerator;
import com.springboot.datagenerator.generator.UserGenerator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeneratorOrchestrator {

    private final UserGenerator userGenerator;
    private final ContentGenerator contentGenerator;
    private final TagGenerator tagGenerator;
    private final ContentTagGenerator contentTagGenerator;
    private final ReviewGenerator reviewGenerator;
    private final PlaylistGenerator playlistGenerator;
    private final PlaylistItemGenerator playlistItemGenerator;
    private final PlaylistSubscriptionGenerator playlistSubscriptionGenerator;
    private final FollowGenerator followGenerator;
    private final NotificationGenerator notificationGenerator;
    private final JdbcTemplate template;

    public void run() {
        truncate();

        List<UUID> userIds = userGenerator.run();
        List<UUID> contentIds = contentGenerator.run();
        List<UUID> tagIds = tagGenerator.run();

        contentTagGenerator.run(contentIds, tagIds);
        reviewGenerator.run(userIds, contentIds);

        List<UUID> playlistIds = playlistGenerator.run(userIds);

        playlistItemGenerator.run(playlistIds, contentIds);
        playlistSubscriptionGenerator.run(userIds, playlistIds);
        followGenerator.run(userIds);
        notificationGenerator.run(userIds);
    }

    private void truncate() {
        log.info("Truncating existing data...");
        template.execute("""
            TRUNCATE TABLE
                follows,
                playlist_subscriptions,
                playlist_items,
                content_tags,
                reviews,
                notifications,
                playlists,
                content_stats,
                contents,
                tags,
                users
            RESTART IDENTITY CASCADE
            """);
        log.info("Truncate complete");
    }
}
