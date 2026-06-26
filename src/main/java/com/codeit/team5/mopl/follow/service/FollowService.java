package com.codeit.team5.mopl.follow.service;

import com.codeit.team5.mopl.follow.dto.response.FollowResponse;
import com.codeit.team5.mopl.follow.entity.Follow;
import com.codeit.team5.mopl.follow.exception.DuplicateFollowException;
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
import org.springframework.dao.DataIntegrityViolationException;
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
        validateNotSelf(followerId, followeeId);
        validateNotDuplicate(followerId, followeeId);

        User follower = getUser(followerId);
        User followee = getUser(followeeId);

        Follow saved;
        try {
            saved = followRepository.saveAndFlush(Follow.create(follower, followee));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateFollowException(followerId, followeeId);
        }

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
        follow.validateOwnedBy(requesterId);

        followRepository.delete(follow);
        log.info("Follow deleted: followId={}, requester={}", followId, requesterId);
    }

    private void validateNotSelf(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw new SelfFollowException(followerId);
        }
    }

    private void validateNotDuplicate(UUID followerId, UUID followeeId) {
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new DuplicateFollowException(followerId, followeeId);
        }
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Follow getFollow(UUID followId) {
        return followRepository.findById(followId)
                .orElseThrow(() -> new FollowNotFoundException(followId));
    }
}
