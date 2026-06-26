package com.codeit.team5.mopl.follow.service;

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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;

    @Transactional
    public FollowResponse follow(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw new SelfFollowException(followerId);
        }
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new DuplicateFollowException(followerId, followeeId);
        }

        User follower = getUser(followerId);
        User followee = getUser(followeeId);

        Follow saved = followRepository.save(Follow.create(follower, followee));

        log.info("Follow created: follower={}, followee={}", followerId, followeeId);
        return followMapper.toDto(saved);
    }

    public FollowResponse getFollowedByMe(UUID followerId, UUID followeeId) {
        return followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .map(followMapper::toDto)
                .orElseThrow(() -> new FollowNotFoundException(followerId, followeeId));
    }

    public long countFollowers(UUID followeeId) {
        return followRepository.countByFolloweeId(followeeId);
    }

    @Transactional
    public void unfollow(UUID requesterId, UUID followId) {
        Follow follow = getFollow(followId);
        validateOwner(follow, requesterId);

        followRepository.delete(follow);
        log.info("Follow deleted: followId={}, requester={}", followId, requesterId);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Follow getFollow(UUID followId) {
        return followRepository.findById(followId)
                .orElseThrow(() -> new FollowNotFoundException(followId));
    }

    private void validateOwner(Follow follow, UUID requesterId) {
        if (!follow.isOwnedBy(requesterId)) {
            throw new FollowForbiddenException(follow.getId(), requesterId);
        }
    }
}
