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

        User savedUser = userRepository.save(user);
        log.info("User created: 회원이 성공적으로 생성되었습니다. userId={}, email={}", savedUser.getId(), savedUser.getEmail());

        return userMapper.toDto(savedUser);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("Duplicated email: detail={}", "이미 사용중인 이메일로 유저 생성 실패: email");
            throw new DuplicatedEmailException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }
}
