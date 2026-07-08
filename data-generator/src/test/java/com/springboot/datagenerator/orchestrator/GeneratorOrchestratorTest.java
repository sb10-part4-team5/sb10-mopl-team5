package com.springboot.datagenerator.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.datagenerator.config.GeneratorProperties;
import com.springboot.datagenerator.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GeneratorOrchestratorTest extends BaseIntegrationTest {

    @Autowired
    private GeneratorOrchestrator orchestrator;

    @Autowired
    private GeneratorProperties properties;

    @Test
    void 모든_테이블에_데이터가_삽입된다() {
        // given / when
        orchestrator.run();

        // then
        assertThat(count("users")).isEqualTo(properties.user());
        assertThat(count("contents")).isEqualTo(properties.content());
        assertThat(count("content_stats")).isEqualTo(properties.content());
        assertThat(count("tags")).isEqualTo(properties.tag());
        assertThat(count("content_tags")).isPositive();
        assertThat(count("reviews")).isPositive();
        assertThat(count("playlists")).isEqualTo(properties.user() * properties.playlistPerUser());
        assertThat(count("playlist_items")).isPositive();
        assertThat(count("playlist_subscriptions")).isPositive();
        assertThat(count("follows")).isPositive();
        assertThat(count("notifications")).isPositive();
    }

    @Test
    void 리뷰_생성_후_content_stats가_집계된다() {
        // given / when
        orchestrator.run();

        // then - 리뷰가 달린 콘텐츠의 review_count > 0
        Integer updatedCount = template.queryForObject(
            "SELECT COUNT(*) FROM content_stats WHERE review_count > 0", Integer.class);
        assertThat(updatedCount).isPositive();

        // rating_sum도 review_count에 맞게 업데이트됨
        Integer mismatchCount = template.queryForObject("""
            SELECT COUNT(*) FROM content_stats cs
            WHERE cs.review_count > 0 AND cs.rating_sum = 0
            """, Integer.class);
        assertThat(mismatchCount).isZero();
    }

    @Test
    void 구독_생성_후_playlist_subscriber_count가_집계된다() {
        // given / when
        orchestrator.run();

        // then - 구독자가 있는 플레이리스트의 subscriber_count > 0
        Integer updatedCount = template.queryForObject(
            "SELECT COUNT(*) FROM playlists WHERE subscriber_count > 0", Integer.class);
        assertThat(updatedCount).isPositive();

        // DB의 실제 구독 수와 subscriber_count가 일치함
        Integer mismatchCount = template.queryForObject("""
            SELECT COUNT(*) FROM playlists p
            WHERE p.subscriber_count != (
                SELECT COUNT(*) FROM playlist_subscriptions ps
                WHERE ps.playlist_id = p.id
            )
            """, Integer.class);
        assertThat(mismatchCount).isZero();
    }

    @Test
    void follow는_자기_자신을_팔로우하지_않는다() {
        // given / when
        orchestrator.run();

        // then
        Integer selfFollowCount = template.queryForObject(
            "SELECT COUNT(*) FROM follows WHERE follower_id = followee_id", Integer.class);
        assertThat(selfFollowCount).isZero();
    }

    private Integer count(String table) {
        return template.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
