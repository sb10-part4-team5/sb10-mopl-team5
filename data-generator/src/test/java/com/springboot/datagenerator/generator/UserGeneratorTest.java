package com.springboot.datagenerator.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.springboot.datagenerator.config.GeneratorProperties;
import com.springboot.datagenerator.support.BaseIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserGeneratorTest extends BaseIntegrationTest {

    @Autowired
    private UserGenerator userGenerator;

    @Autowired
    private GeneratorProperties properties;

    @Test
    void yml에_설정된_수만큼_유저가_삽입된다() {
        // given
        int expected = properties.user();

        // when
        List<UUID> ids = userGenerator.run();

        // then
        assertThat(ids).hasSize(expected);
        Integer count = template.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(count).isEqualTo(expected);
    }

    @Test
    void 삽입된_이메일은_중복되지_않는다() {
        // given / when
        userGenerator.run();

        // then
        Integer totalCount = template.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Integer distinctCount = template.queryForObject("SELECT COUNT(DISTINCT email) FROM users", Integer.class);
        assertThat(distinctCount).isEqualTo(totalCount);
    }

    @Test
    void 모든_유저의_role은_USER이다() {
        // given / when
        userGenerator.run();

        // then
        Integer userRoleCount = template.queryForObject(
            "SELECT COUNT(*) FROM users WHERE role = 'USER'", Integer.class);
        Integer totalCount = template.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(userRoleCount).isEqualTo(totalCount);
    }
}
