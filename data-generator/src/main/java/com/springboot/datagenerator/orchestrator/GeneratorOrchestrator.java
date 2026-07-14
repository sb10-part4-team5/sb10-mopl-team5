package com.springboot.datagenerator.orchestrator;

import com.springboot.datagenerator.config.GeneratorProperties;
import com.springboot.datagenerator.generator.BinaryContentGenerator;
import com.springboot.datagenerator.generator.ContentGenerator;
import com.springboot.datagenerator.generator.ContentTagGenerator;
import com.springboot.datagenerator.generator.ConversationGenerator;
import com.springboot.datagenerator.generator.DirectMessageGenerator;
import com.springboot.datagenerator.generator.FollowGenerator;
import com.springboot.datagenerator.generator.NotificationGenerator;
import com.springboot.datagenerator.generator.PlaylistGenerator;
import com.springboot.datagenerator.generator.PlaylistItemGenerator;
import com.springboot.datagenerator.generator.PlaylistSubscriptionGenerator;
import com.springboot.datagenerator.generator.ReviewGenerator;
import com.springboot.datagenerator.generator.TagGenerator;
import com.springboot.datagenerator.generator.UserGenerator;
import java.util.List;
import java.util.Map;
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
    private final BinaryContentGenerator binaryContentGenerator;
    private final ContentGenerator contentGenerator;
    private final TagGenerator tagGenerator;
    private final ContentTagGenerator contentTagGenerator;
    private final ReviewGenerator reviewGenerator;
    private final PlaylistGenerator playlistGenerator;
    private final PlaylistItemGenerator playlistItemGenerator;
    private final PlaylistSubscriptionGenerator playlistSubscriptionGenerator;
    private final FollowGenerator followGenerator;
    private final NotificationGenerator notificationGenerator;
    private final ConversationGenerator conversationGenerator;
    private final DirectMessageGenerator directMessageGenerator;
    private final GeneratorProperties properties;
    private final JdbcTemplate template;

    public void run() {
        truncate();

        int profileImageCount = properties.user() * properties.profileImagePercent() / 100;
        List<UUID> profileImageIds = binaryContentGenerator.run(profileImageCount);

        List<UUID> userIds = userGenerator.run(profileImageIds);
        List<UUID> contentIds = contentGenerator.run();
        List<UUID> tagIds = tagGenerator.run();

        contentTagGenerator.run(contentIds, tagIds);
        reviewGenerator.run(userIds, contentIds);
        updateContentStats();

        List<UUID> playlistIds = playlistGenerator.run(userIds);

        playlistItemGenerator.run(playlistIds, contentIds);
        playlistSubscriptionGenerator.run(userIds, playlistIds);
        updateSubscriberCount();
        followGenerator.run(userIds);
        notificationGenerator.run(userIds);

        Map<UUID, UUID[]> conversations = conversationGenerator.run(userIds);
        directMessageGenerator.run(conversations);
    }

    private void updateSubscriberCount() {
        log.info("Updating playlists.subscriber_count...");
        template.execute("""
            UPDATE playlists p
            SET subscriber_count = sub.cnt
            FROM (
                SELECT playlist_id, COUNT(*) AS cnt
                FROM playlist_subscriptions
                GROUP BY playlist_id
            ) sub
            WHERE p.id = sub.playlist_id
            """);
        log.info("subscriber_count updated");
    }

    private void updateContentStats() {
        log.info("Updating content_stats from reviews...");
        template.execute("""
            UPDATE content_stats cs
            SET review_count = sub.cnt,
                rating_sum   = sub.sum_rating
            FROM (
                SELECT content_id, COUNT(*) AS cnt, SUM(rating) AS sum_rating
                FROM reviews
                GROUP BY content_id
            ) sub
            WHERE cs.id = sub.content_id
            """);
        log.info("content_stats updated");
    }

    private void truncate() {
        log.info("Truncating existing data...");
        template.execute("""
            TRUNCATE TABLE
                direct_messages,
                conversations,
                binary_contents,
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
