package com.codeit.team5.mopl.follow.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import com.codeit.team5.mopl.TestcontainersConfiguration;
import com.codeit.team5.mopl.config.JpaAuditingConfig;
import com.codeit.team5.mopl.follow.entity.Follow;
import com.codeit.team5.mopl.follow.exception.SelfFollowException;
import com.codeit.team5.mopl.user.entity.User;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Import({JpaAuditingConfig.class, TestcontainersConfiguration.class})
class FollowRepositoryTest {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private EntityManager entityManager;

    private User saveUser(String email, String name) {
        User user = User.create(email, "password", name);
        entityManager.persist(user);
        return user;
    }

    private Follow saveFollow(User follower, User followee) {
        Follow follow = Follow.create(follower, followee);
        entityManager.persist(follow);
        return follow;
    }

    @Test
    @DisplayName("팔로우 관계가 존재하면 true 반환 성공")
    void existsByFollowerIdAndFolloweeId_returnTrue_success() {
        // given
        User follower = saveUser("follower@mopl.com", "팔로워");
        User followee = saveUser("followee@mopl.com", "팔로위");
        saveFollow(follower, followee);
        entityManager.flush();
        entityManager.clear();

        // when
        boolean result = followRepository
                .existsByFollowerIdAndFolloweeId(follower.getId(), followee.getId());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("팔로우 관계가 없으면 false 반환 성공")
    void existsByFollowerIdAndFolloweeId_returnFalse_success() {
        // given
        User follower = saveUser("follower@mopl.com", "팔로워");
        User followee = saveUser("followee@mopl.com", "팔로위");
        entityManager.flush();
        entityManager.clear();

        // when
        boolean result = followRepository
                .existsByFollowerIdAndFolloweeId(follower.getId(), followee.getId());

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("follower/followee id로 팔로우 관계 조회 성공")
    void findByFollowerIdAndFolloweeId_present_success() {
        // given
        User follower = saveUser("follower@mopl.com", "팔로워");
        User followee = saveUser("followee@mopl.com", "팔로위");
        Follow saved = saveFollow(follower, followee);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<Follow> result = followRepository
                .findByFollowerIdAndFolloweeId(follower.getId(), followee.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("팔로우 관계가 없으면 빈 결과 반환 성공")
    void findByFollowerIdAndFolloweeId_empty_success() {
        // when
        Optional<Follow> result = followRepository
                .findByFollowerIdAndFolloweeId(UUID.randomUUID(), UUID.randomUUID());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("followee의 팔로워 수 집계 성공")
    void countByFolloweeId_success() {
        // given
        User target = saveUser("target@mopl.com", "타겟");
        User follower1 = saveUser("f1@mopl.com", "팔로워1");
        User follower2 = saveUser("f2@mopl.com", "팔로워2");
        saveFollow(follower1, target);
        saveFollow(follower2, target);
        // 노이즈: target이 다른 사람을 팔로우 → target의 팔로워 수에 잡히면 안 됨
        User other = saveUser("other@mopl.com", "기타");
        saveFollow(target, other);
        entityManager.flush();
        entityManager.clear();

        // when
        long count = followRepository.countByFolloweeId(target.getId());

        // then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("동일한 follower-followee로 중복 저장하면 실패")
    void save_duplicate_throwsException() {
        // given
        User follower = saveUser("follower@mopl.com", "팔로워");
        User followee = saveUser("followee@mopl.com", "팔로위");
        followRepository.saveAndFlush(Follow.create(follower, followee));

        // when & then
        Follow duplicate = Follow.create(follower, followee);
        assertThatThrownBy(() -> followRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("자기 자신으로 팔로우를 생성하면 실패")
    void create_self_throwsException() {
        // given
        User user = saveUser("self@mopl.com", "본인");

        // when & then
        assertThatThrownBy(() -> Follow.create(user, user))
                .isInstanceOf(SelfFollowException.class);
    }

    @Test
    @DisplayName("같은 미저장 사용자 인스턴스로 팔로우를 생성하면 실패")
    void create_sameTransientUser_throwsException() {
        User user = User.create("self@mopl.com", "password", "본인");

        assertThatThrownBy(() -> Follow.create(user, user))
                .isInstanceOf(SelfFollowException.class);
    }
}
