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

    // 1. findByWatcherId
    @Test
    @DisplayName("세션이 존재할 때 1번의 쿼리로 연관관계까지 조회_성공")
    void findByWatcherId_성공() {
        // given
        User user = createUser();
        Content content = createContent();
        createSession(user, content);
        flush();
        clear();

        // when
        Optional<WatchingSession> result = repository.findByWatcherId(user.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getWatcher().getEmail()).isEqualTo(user.getEmail());
        assertThat(result.get().getContent().getTitle()).isEqualTo(content.getTitle());
        ensureQueryCount(1); // EntityGraph를 통해 1번의 쿼리로 N+1 없이 가져와야 함
    }

    @Test
    @DisplayName("세션이 없을 때 빈 Optional 반환")
    void findByWatcherId_NotFound() {
        // given
        flush();
        clear();

        // when
        Optional<WatchingSession> result = repository.findByWatcherId(UUID.randomUUID());

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

        // when 1 (첫 번째 페이지 조회)
        Window<WatchingSession> result1 =
                repository.findByContentId(content.getId(), position, limit, sort);

        // then 1
        assertThat(result1.isEmpty()).isFalse();
        assertThat(result1.hasNext()).isTrue();
        assertThat(result1.getContent()).hasSize(1);

        WatchingSession session1 = result1.getContent().get(0);
        assertThat(session1.getWatcher().getEmail()).isNotNull();

        // 1-N 연관관계(tags 등)를 default_batch_fetch_size로 가져오는지 검증
        // EntityGraph에는 tags가 없지만, 컬렉션을 터치하면 In 쿼리가 나감
        session1.getContent().getContentTags().size();

        // when 2 (두 번째 페이지 조회)
        ScrollPosition nextPosition = result1.positionAt(result1.getContent().size() - 1);
        Window<WatchingSession> result2 =
                repository.findByContentId(content.getId(), nextPosition, limit, sort);

        // then 2
        assertThat(result2.isEmpty()).isFalse();
        assertThat(result2.hasNext()).isFalse();
        assertThat(result2.getContent()).hasSize(1);

        WatchingSession session2 = result2.getContent().get(0);
        assertThat(session2.getWatcher().getEmail()).isNotNull();
        session2.getContent().getContentTags().size();

        // 메인 쿼리 2번 + Batch Fetch 쿼리 1번(첫 조회 시 캐싱됨) = 총 3번의 쿼리 예상
        ensureQueryCount(3);
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
        Window<WatchingSession> result =
                repository.findByContentId(content.getId(), position, limit, sort);

        // then
        assertThat(result.isEmpty()).isFalse();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getContent()).hasSize(1);

        WatchingSession session = result.getContent().get(0);
        assertThat(session.getWatcher().getEmail()).isEqualTo(user.getEmail());
        assertThat(session.getContent().getTitle()).isEqualTo(content.getTitle());

        // 메인 쿼리 1번
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
        Window<WatchingSession> result =
                repository.findByContentId(randomContentId, position, limit, sort);

        // then
        assertThat(result.isEmpty()).isTrue();
        ensureQueryCount(1);
    }

    // 3. deleteByWatcherIdDirectly
    @Test
    @DisplayName("유저 ID로 세션을 직접 삭제_성공")
    void deleteByWatcherIdDirectly_성공() {
        // given
        User user = createUser();
        Content content = createContent();
        createSession(user, content);
        flush();
        clear();

        // when
        repository.deleteByWatcherIdDirectly(user.getId());

        // then
        assertThat(repository.existsByWatcherId(user.getId())).isFalse();
        ensureQueryCount(2); // DELETE 1번 + exists 1번 = 2번
    }

    // 4. existsByWatcherId
    @Test
    @DisplayName("세션이 존재하면 true 반환_성공")
    void existsByWatcherId_성공() {
        // given
        User user = createUser();
        Content content = createContent();
        createSession(user, content);
        flush();
        clear();

        // when
        boolean exists = repository.existsByWatcherId(user.getId());

        // then
        assertThat(exists).isTrue();
        ensureQueryCount(1);
    }

    @Test
    @DisplayName("세션이 존재하지 않으면 false 반환")
    void existsByWatcherId_NotFound() {
        // given
        UUID randomId = UUID.randomUUID();

        // when
        boolean exists = repository.existsByWatcherId(randomId);

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
        Content content = Content.createByExternalSource(ContentType.MOVIE, "test title", null,
                ContentSource.TMDB, "ext-id-123", null, null);
        return persistAndFlush(content);
    }

    private WatchingSession createSession(User user, Content content) {
        WatchingSession session = WatchingSession.of(user, content);
        return persistAndFlush(session);
    }
}
