package com.codeit.team5.mopl.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.team5.mopl.follow.dto.response.FollowResponse;
import com.codeit.team5.mopl.follow.entity.Follow;
import com.codeit.team5.mopl.follow.exception.DuplicateFollowException;
import com.codeit.team5.mopl.follow.exception.FollowForbiddenException;
import com.codeit.team5.mopl.follow.exception.FollowNotFoundException;
import com.codeit.team5.mopl.follow.exception.SelfFollowException;
import com.codeit.team5.mopl.follow.mapper.FollowMapper;
import com.codeit.team5.mopl.follow.repository.FollowRepository;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowMapper followMapper;

    @InjectMocks
    private FollowService followService;

    @Test
    @DisplayName("팔로우 성공")
    void follow_success() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        User follower = User.create("follower@mopl.com", "pw", "팔로워");
        User followee = User.create("followee@mopl.com", "pw", "팔로위");
        FollowResponse expected = new FollowResponse(UUID.randomUUID(), followeeId, followerId);

        when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).thenReturn(false);
        when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
        when(userRepository.findById(followeeId)).thenReturn(Optional.of(followee));
        when(followRepository.saveAndFlush(any(Follow.class))).then(returnsFirstArg());
        when(followMapper.toDto(any(Follow.class))).thenReturn(expected);

        // when
        FollowResponse result = followService.follow(followerId, followeeId);

        // then
        assertThat(result).isSameAs(expected);
        ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
        verify(followRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getFollower()).isSameAs(follower);
        assertThat(captor.getValue().getFollowee()).isSameAs(followee);
    }

    @Test
    @DisplayName("자기 자신을 팔로우하면 실패")
    void follow_self_throwsException() {
        // given
        UUID userId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> followService.follow(userId, userId))
                .isInstanceOf(SelfFollowException.class);
        verifyNoInteractions(followRepository, userRepository, followMapper);
    }

    @Test
    @DisplayName("팔로우 대상 사용자가 없으면 실패")
    void follow_followeeNotFound_throwsException() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).thenReturn(false);
        when(userRepository.findById(followerId)).thenReturn(Optional.of(User.create("f@mopl.com", "pw", "팔로워")));
        when(userRepository.findById(followeeId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.follow(followerId, followeeId))
                .isInstanceOf(UserNotFoundException.class);
        verify(followRepository, never()).saveAndFlush(any(Follow.class));
    }

    @Test
    @DisplayName("팔로우 요청자(본인)가 없으면 실패")
    void follow_followerNotFound_throwsException() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).thenReturn(false);
        when(userRepository.findById(followerId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.follow(followerId, followeeId))
                .isInstanceOf(UserNotFoundException.class);
        verify(followRepository, never()).saveAndFlush(any(Follow.class));
    }

    @Test
    @DisplayName("이미 팔로우한 사용자면 실패")
    void follow_duplicate_throwsException() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        when(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> followService.follow(followerId, followeeId))
                .isInstanceOf(DuplicateFollowException.class);
        verify(followRepository, never()).saveAndFlush(any(Follow.class));
        verifyNoInteractions(userRepository, followMapper);
    }

    @Test
    @DisplayName("팔로우 여부 확인 - 팔로우 중이면 성공")
    void getFollowedByMe_following_success() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        Follow follow = Follow.create(mock(User.class), mock(User.class));
        FollowResponse expected = new FollowResponse(UUID.randomUUID(), followeeId, followerId);
        when(followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId))
                .thenReturn(Optional.of(follow));
        when(followMapper.toDto(follow)).thenReturn(expected);

        // when
        FollowResponse result = followService.getFollowedByMe(followerId, followeeId);

        // then
        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("팔로우 여부 확인 - 팔로우하지 않으면 실패")
    void getFollowedByMe_notFollowing_throwsException() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        when(followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.getFollowedByMe(followerId, followeeId))
                .isInstanceOf(FollowNotFoundException.class);
    }

    @Test
    @DisplayName("팔로워 수 조회 성공")
    void countFollowers_success() {
        // given
        UUID followeeId = UUID.randomUUID();
        when(followRepository.countByFolloweeId(followeeId)).thenReturn(3L);

        // when
        long result = followService.countFollowers(followeeId);

        // then
        assertThat(result).isEqualTo(3L);
    }

    @Test
    @DisplayName("팔로우 취소 성공")
    void unfollow_success() {
        // given
        UUID requesterId = UUID.randomUUID();
        UUID followId = UUID.randomUUID();
        User follower = mock(User.class);
        when(follower.getId()).thenReturn(requesterId);
        Follow follow = Follow.create(follower, mock(User.class));
        when(followRepository.findById(followId)).thenReturn(Optional.of(follow));

        // when
        followService.unfollow(requesterId, followId);

        // then
        verify(followRepository).delete(follow);
    }

    @Test
    @DisplayName("취소할 팔로우가 없으면 실패")
    void unfollow_notFound_throwsException() {
        // given
        UUID requesterId = UUID.randomUUID();
        UUID followId = UUID.randomUUID();
        when(followRepository.findById(followId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.unfollow(requesterId, followId))
                .isInstanceOf(FollowNotFoundException.class);
        verify(followRepository, never()).delete(any(Follow.class));
    }

    @Test
    @DisplayName("본인의 팔로우가 아니면 취소 실패")
    void unfollow_notOwner_throwsException() {
        // given
        UUID requesterId = UUID.randomUUID();
        UUID followId = UUID.randomUUID();
        User otherFollower = mock(User.class);
        when(otherFollower.getId()).thenReturn(UUID.randomUUID());
        Follow follow = Follow.create(otherFollower, mock(User.class));
        ReflectionTestUtils.setField(follow, "id", followId);
        when(followRepository.findById(followId)).thenReturn(Optional.of(follow));

        // when & then
        assertThatThrownBy(() -> followService.unfollow(requesterId, followId))
                .isInstanceOf(FollowForbiddenException.class);
        verify(followRepository, never()).delete(any(Follow.class));
    }
}
