package com.springboot.datagenerator.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.datagenerator.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GeneratorPropertiesTest extends BaseIntegrationTest {

    @Autowired
    private GeneratorProperties properties;

    @Test
    void yml값이_올바르게_바인딩된다() {
        // given - application-test.yml 기준

        // when / then
        assertThat(properties.user()).isEqualTo(10);
        assertThat(properties.content()).isEqualTo(20);
        assertThat(properties.tag()).isEqualTo(10);
        assertThat(properties.tagPerContent()).isEqualTo(2);
        assertThat(properties.reviewPerUser()).isEqualTo(2);
        assertThat(properties.playlistPerUser()).isEqualTo(2);
        assertThat(properties.itemPerPlaylist()).isEqualTo(3);
        assertThat(properties.subscriptionPerUser()).isEqualTo(2);
        assertThat(properties.followPerUser()).isEqualTo(3);
        assertThat(properties.notificationPerUser()).isEqualTo(5);
        assertThat(properties.dbBatchSize()).isEqualTo(100);
    }

    @Test
    void Min_제약_위반시_컨텍스트_로드에_실패한다() {
        // given
        ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GeneratorConfig.class)
            .withPropertyValues(
                "generator.user=0",  // @Min(1) 위반
                "generator.content=1",
                "generator.tag=1",
                "generator.tag-per-content=1",
                "generator.review-per-user=0",
                "generator.playlist-per-user=0",
                "generator.item-per-playlist=1",
                "generator.subscription-per-user=0",
                "generator.follow-per-user=0",
                "generator.notification-per-user=0",
                "generator.db-batch-size=1"
            );

        // when / then
        runner.run(context -> assertThat(context).hasFailed());
    }
}
