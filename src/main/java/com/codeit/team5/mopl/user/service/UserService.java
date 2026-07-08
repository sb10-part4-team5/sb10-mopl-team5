package com.codeit.team5.mopl.user.service;

import com.codeit.team5.mopl.auth.service.RefreshTokenStore;
import com.codeit.team5.mopl.auth.service.TemporaryPasswordService;
import com.codeit.team5.mopl.auth.support.EmailNormalizer;
import com.codeit.team5.mopl.binarycontent.dto.UploadedBinaryContent;
import com.codeit.team5.mopl.binarycontent.service.BinaryContentService;
import com.codeit.team5.mopl.global.dto.CursorResponse;
import com.codeit.team5.mopl.user.dto.request.ChangePasswordRequest;
import com.codeit.team5.mopl.user.dto.request.UserCursorRequest;
import com.codeit.team5.mopl.user.dto.request.UserLockedUpdateRequest;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.request.UserRoleUpdateRequest;
import com.codeit.team5.mopl.user.dto.request.UserUpdateRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.entity.UserRole;
import com.codeit.team5.mopl.user.event.RoleChangedEvent;
import com.codeit.team5.mopl.user.event.UserLockedEvent;
import com.codeit.team5.mopl.user.exception.DuplicatedEmailException;
import com.codeit.team5.mopl.user.exception.UserForbiddenException;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final BinaryContentService binaryContentService;
    private final TemporaryPasswordService temporaryPasswordService;
    private final ApplicationEventPublisher eventPublisher;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional
    public UserResponse create(UserRegisterRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());

        validateDuplicateEmail(normalizedEmail);

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.create(normalizedEmail, encodedPassword, request.name());

        User savedUser = userRepository.save(user);

        log.info(
                "User created: 회원이 성공적으로 생성되었습니다. userId={}",
                savedUser.getId()
        );

        return userMapper.toDto(savedUser);
    }

    public UserResponse getById(UUID userId) {
        log.debug("User detail lookup: userId={}", userId);

        User user = getUser(userId);

        log.debug("User retrieved: userId={}", userId);
        return userMapper.toDto(user);
    }

    @Transactional
    public UserResponse update(UUID userId, UserUpdateRequest request, UploadedBinaryContent uploadedImage) {
        User user = getUserWithProfileImage(userId);

        user.updateName(request.name());
        if (uploadedImage != null) {
            user.updateProfileImage(binaryContentService.saveCompleted(uploadedImage));
        }

        log.info("User updated: userId={}", userId);
        return userMapper.toDto(user);
    }

    @Transactional
    public void updateRole(UUID userId, UserRoleUpdateRequest request) {
        User requestedUser = getUser(userId);
        UserRole roleBefore = requestedUser.getRole();

        requestedUser.updateRole(request.role());
        refreshTokenStore.deleteByUserId(userId);

        eventPublisher.publishEvent(
                new RoleChangedEvent(
                        requestedUser.getId(),
                        roleBefore.name(),
                        request.role().name()
                )
        );

        log.info(
                "User role updated: userId={}, roleBefore={}, roleAfter={}",
                userId,
                roleBefore,
                request.role()
        );
    }

    @Transactional
    public void updateLock(UUID userId, UserLockedUpdateRequest request) {
        User requestUser = getUser(userId);

        requestUser.updateLocked(request.locked());
        refreshTokenStore.deleteByUserId(userId);
        eventPublisher.publishEvent(new UserLockedEvent(userId, request.locked()));
        log.info("User lock status updated: userId={}, isLocked={}", userId, request.locked());
    }

    @Transactional
    public void updatePassword(UUID currentUserId, UUID userId, ChangePasswordRequest request) {
        validateOwner(currentUserId, userId);

        User requestUser = getUser(userId);
        String encodedPassword = passwordEncoder.encode(request.password());

        requestUser.updatePassword(encodedPassword);

        temporaryPasswordService.deleteByUserId(userId);
        refreshTokenStore.deleteByUserId(userId);

        log.info("User password updated: userId={}", userId);
    }

    public CursorResponse<UserResponse> findUsers(UserCursorRequest request) {
        int fetchLimit = request.limit() + 1;
        List<User> fetched = userRepository.findUsers(request, fetchLimit);
        boolean hasNext = fetched.size() > request.limit();
        List<User> page = hasNext ? fetched.subList(0, request.limit()) : fetched;
        long totalCount = userRepository.countUsers(request);

        return userMapper.toCursor(page, hasNext, totalCount, request.sortBy(), request.sortDirection());
    }

    private User getUserWithProfileImage(UUID userId) {
        return userRepository.findWithProfileImageById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: userId={}", userId);
                    return new UserNotFoundException(userId);
                });
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: userId={}", userId);
                    return new UserNotFoundException(userId);
                });
    }

    private void validateOwner(UUID currentUserId, UUID userId) {
        if (!currentUserId.equals(userId)) {
            throw new UserForbiddenException(currentUserId, userId);
        }
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn(
                    "Duplicated email: detail={}",
                    "이미 사용중인 이메일로 유저 생성 실패"
            );
            throw new DuplicatedEmailException(email);
        }
    }
}
