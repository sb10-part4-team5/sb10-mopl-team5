package com.codeit.team5.mopl.watcher.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.team5.mopl.content.entity.Content;
import com.codeit.team5.mopl.content.entity.ContentSource;
import com.codeit.team5.mopl.content.entity.ContentType;
import com.codeit.team5.mopl.global.support.base.BaseRepositoryTest;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.watcher.entity.WatchingSession;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;

class WatchingSessionRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private WatchingSessionRepository repository;

    @BeforeEach
    void setUp() {
        clear();
    }

    // 1. findByUserId
    @Test
    @DisplayName("세션이 존재할 때 1번의 쿼리로 연관관계까지 조회_성공")
    void findByUserId_성공() {
        // given
        User user = createUser();
        Content content = createContent();
        createSession(user, content);
        flush();
        clear();

        // when
        Optional<WatchingSession> result = repository.findByUserId(user.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(result.get().getContent().getId()).isEqualTo(content.getId());
        ensureQueryCount(1);
    }

    @Test
    @DisplayName("세션이 없을 때 빈 Optional 반환")
    void findByUserId_NotFound() {
        // given
        UUID randomUserId = UUID.randomUUID();
        flush();
        clear();

        // when
        Optional<WatchingSession> result = repository.findByUserId(randomUserId);

        // then
        assertThat(result).isEmpty();
        ensureQueryCount(1);
    }

    // 2. findByContentId
    @Test
    @DisplayName("조회 결과가 Limit보다 많아 hasNext가 true_성공")
    void findByContentId_HasNextTrue_성공() {
        // given
        User user1 = createUser("test1@test.com");
        User user2 = createUser("test2@test.com");
        Content content = createContent();
        createSession(user1, content);
        createSession(user2, content);
        flush();
        clear();

        ScrollPosition position = ScrollPosition.keyset();
        Limit limit = Limit.of(1); // 1개만 조회하여 다음 페이지가 존재하도록 함
        Sort sort = Sort.by(Sort.Direction.DESC, "id");

        // when
        Window<WatchingSession> result = repository.findByContentId(content.getId(), position,
                limit, sort);

        // then
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.getContent()).hasSize(1);
        ensureQueryCount(1);
    }

    @Test
    @DisplayName("조회 결과가 Limit보다 적어 hasNext가 false_성공")
    void findByContentId_HasNextFalse_성공() {
        // given
        User user = createUser();
        Content content = createContent();
        createSession(user, content);
        flush();
        clear();

        ScrollPosition position = ScrollPosition.keyset();
        Limit limit = Limit.of(10);
        Sort sort = Sort.by(Sort.Direction.DESC, "id");

        // when
        Window<WatchingSession> result = repository.findByContentId(content.getId(), position,
                limit, sort);

        // then
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent().getId()).isEqualTo(content.getId());
        ensureQueryCount(1);
    }

    @Test
    @DisplayName("컨텐츠가 없을 때 빈 리스트 반환")
    void findByContentId_NotFound() {
        // given
        UUID randomContentId = UUID.randomUUID();

        ScrollPosition position = ScrollPosition.keyset();
        Limit limit = Limit.of(10);
        Sort sort = Sort.by(Sort.Direction.DESC, "id");

        // when
        Window<WatchingSession> result = repository.findByContentId(randomContentId, position,
                limit, sort);

        // then
        assertThat(result.isEmpty()).isTrue();
        ensureQueryCount(1);
    }

    // 3. deleteByUserIdDirectly
    @Test
    @DisplayName("유저 ID로 세션을 직접 삭제_성공")
    void deleteByUserIdDirectly_성공() {
        // given
        User user = createUser();
        Content content = createContent();
        createSession(user, content);
        flush();
        clear();

        // when
        repository.deleteByUserIdDirectly(user.getId());

        // then
        assertThat(repository.existsByUserId(user.getId())).isFalse();
        ensureQueryCount(2); // DELETE 1번 + exists 1번 = 2번
    }

    @Test
    @DisplayName("조건에 맞는 데이터가 없을 때 삭제되지 않음")
    void deleteByUserIdDirectly_NotFound() {
        // given
        UUID randomUserId = UUID.randomUUID();

        // when
        repository.deleteByUserIdDirectly(randomUserId);

        // then
        ensureQueryCount(1); // DELETE 1번
    }

    // 4. existsByUserId
    @Test
    @DisplayName("세션이 존재하면 true 반환_성공")
    void existsByUserId_성공() {
        // given
        User user = createUser();
        Content content = createContent();
        createSession(user, content);
        flush();
        clear();

        // when
        boolean exists = repository.existsByUserId(user.getId());

        // then
        assertThat(exists).isTrue();
        ensureQueryCount(1);
    }

    @Test
    @DisplayName("세션이 존재하지 않으면 false 반환")
    void existsByUserId_NotFound() {
        // given
        UUID randomUserId = UUID.randomUUID();

        // when
        boolean exists = repository.existsByUserId(randomUserId);

        // then
        assertThat(exists).isFalse();
        ensureQueryCount(1);
    }

    private User createUser() {
        return createUser("test@test.com");
    }

    private User createUser(String email) {
        User user = User.create(email, "test-password", "testUser");
        return persistAndFlush(user);
    }

    private Content createContent() {
        Content content = Content.createByExternalSource(
                ContentType.MOVIE,
                "test title",
                null,
                ContentSource.TMDB,
                "ext-id-123",
                null,
                null
        );
        return persistAndFlush(content);
    }

    private WatchingSession createSession(User user, Content content) {
        WatchingSession session = WatchingSession.of(user, content);
        return persistAndFlush(session);
    }
}
