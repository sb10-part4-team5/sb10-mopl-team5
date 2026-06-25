package com.codeit.team5.mopl.user.service;

import com.codeit.team5.mopl.binarycontent.BinaryContentStorage;
import com.codeit.team5.mopl.binarycontent.StorageDirectory;
import com.codeit.team5.mopl.binarycontent.entity.BinaryContent;
import com.codeit.team5.mopl.binarycontent.event.BinaryContentUploadEvent;
import com.codeit.team5.mopl.binarycontent.repository.BinaryContentRepository;
import com.codeit.team5.mopl.global.dto.FileRequest;
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
    public UserResponse update(UUID userId, UserUpdateRequest request, FileRequest image) {
        User user = getUser(userId);

        // TODO: 인증 구현 후 본인 확인(현재 로그인 사용자 == userId) 추가, 불일치 시 403

        user.updateName(request.name());
        if (image != null) {
            user.updateProfileImage(storeProfileImage(user.getId(), image));
        }

        log.info("User updated: userId={}", userId);
        return userMapper.toDto(user);
    }

    private BinaryContent storeProfileImage(UUID userId, FileRequest image) {
        // TODO(고아 정리): 기존 profileImage를 삭제 대상 상태로 표시 후 배치로 정리 (DB/S3 누적 방지)
        String key = binaryContentStorage.generateKey(StorageDirectory.PROFILE, userId, image.filename());
        BinaryContent profileImage = binaryContentRepository.save(
                BinaryContent.pending(binaryContentStorage.toUrl(key)));

        eventPublisher.publishEvent(
                new BinaryContentUploadEvent(profileImage.getId(), key, image.bytes()));
        // TODO(업로드 실패 대응): 비동기 업로드 실패 시 보상 트랜잭션으로 프로필 이미지 롤백

        return profileImage;
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
