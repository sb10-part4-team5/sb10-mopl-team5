package com.codeit.team5.mopl.user.service;

import com.codeit.team5.mopl.binarycontent.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.StoragePrefix;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.global.dto.FileResource;
import com.codeit.team5.mopl.global.exception.ErrorCode;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.request.UserUpdateRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.DuplicatedEmailException;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
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
    private final BinaryContentStorage binaryContentStorage;
    private final BinaryContentRepository binaryContentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public UserResponse create(UserRegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

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
    public UserResponse update(UUID userId, UserUpdateRequest request, FileResource image) {
        User user = getUser(userId);

        // TODO: 인증 구현 후 본인 확인(현재 로그인 사용자 == userId) 추가, 불일치 시 403

        BinaryContent profileImage = user.getProfileImage(); // 이미지 미첨부 시 기존 유지
        if (image != null) {
            String key = binaryContentStorage.generateKey(StoragePrefix.PROFILE, user.getId(), image.filename());
            profileImage = binaryContentRepository.save(
                    BinaryContent.pending(binaryContentStorage.toUrl(key)));
            eventPublisher.publishEvent(
                    new BinaryContentUploadEvent(profileImage.getId(), key, image.bytes()));
        }

        user.updateProfile(request.name(), profileImage);

        log.info("User updated: userId={}", userId);
        return userMapper.toDto(user);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: userId={}", userId);
                    return new UserNotFoundException(userId);
                });
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

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
