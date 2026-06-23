package com.codeit.team5.mopl.user.service;

import com.codeit.team5.mopl.global.exception.ErrorCode;
import com.codeit.team5.mopl.user.dto.request.UserRegisterRequest;
import com.codeit.team5.mopl.user.dto.response.UserResponse;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.DuplicatedEmailException;
import com.codeit.team5.mopl.user.mapper.UserMapper;
import com.codeit.team5.mopl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Transactional
    public UserResponse create(UserRegisterRequest request) {
        validateDuplicateEmail(request.email());

        User user = userMapper.toEntity(request);
        String encodedPassword = passwordEncoder.encode(request.password());
        user.updatePassword(encodedPassword);

        try {
            User savedUser = userRepository.save(user);

            log.info(
                    "User created: 회원이 성공적으로 생성되었습니다. userId={}",
                    savedUser.getId()
            );

            return userMapper.toDto(savedUser);
        } catch (DataIntegrityViolationException e) {
            log.warn(
                    "Duplicated email: 저장 중 이메일 중복 충돌 발생. email={}",
                    maskEmail(request.email()),
                    e
            );
            throw new DuplicatedEmailException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn(
                    "Duplicated email: detail={}",
                    "이미 사용중인 이메일로 유저 생성 실패"
            );
            throw new DuplicatedEmailException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 2) {
            return "***@" + domain;
        }

        return localPart.substring(0, 2) + "***@" + domain;
    }
}
