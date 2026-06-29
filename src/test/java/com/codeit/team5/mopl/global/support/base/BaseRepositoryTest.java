package com.codeit.team5.mopl.global.support.base;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.global.support.config.HibernateConfig;
import com.codeit.team5.mopl.global.support.config.QueryDslTestConfig;
import com.codeit.team5.mopl.global.support.inspector.QueryInspector;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Import({HibernateConfig.class, QueryInspector.class, TestcontainersConfiguration.class, QueryDslTestConfig.class})
@DataJpaTest
@EnableJpaAuditing
@AutoConfigureTestDatabase(replace = Replace.NONE)
public abstract class BaseRepositoryTest {

    @Autowired
    protected TestEntityManager entityManager;
    @Autowired
    protected QueryInspector queryInspector;

    protected void flush() {
        entityManager.flush();
    }

    protected void clear() {
        entityManager.clear();
        queryInspector.clear();
    }

    protected void ensureQueryCount(int count) {
        Assertions.assertThat(queryInspector.getCount()).isEqualTo(count);
    }

    protected <T> T persistAndFlush(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    protected boolean compareInstant(Instant a, Instant b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return toTruncated(a).equals(toTruncated(b));
    }

    private Instant toTruncated(Instant time) {
        return time.truncatedTo(ChronoUnit.MILLIS);
    }
}
