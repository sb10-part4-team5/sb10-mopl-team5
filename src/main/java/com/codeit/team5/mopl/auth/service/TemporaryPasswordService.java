package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.entity.TemporaryPassword;
import com.codeit.team5.mopl.auth.support.TemporaryPasswordGenerator;
import com.codeit.team5.mopl.auth.repository.TemporaryPasswordRepository;
import com.codeit.team5.mopl.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// “임시 비밀번호 credential” 발급/검증/폐기 정책 담당
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TemporaryPasswordService {

    private final TemporaryPasswordRepository temporaryPasswordRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Transactional
    public String issue(User user) {
        String rawPassword = temporaryPasswordGenerator.generate();
        String passwordHash = passwordEncoder.encode(rawPassword);

        TemporaryPassword temporaryPassword =
                temporaryPasswordRepository.findByUserId(user.getId())
                        .map(existing -> {
                            existing.reissue(passwordHash, Instant.now());
                            return existing;
                        })
                        .orElseGet(() -> TemporaryPassword.create(user, passwordHash, Instant.now()));

        temporaryPasswordRepository.save(temporaryPassword);

        log.info("TemporaryPassword saved: userId={}", user.getId());

        return rawPassword;
    }

    public boolean matches(UUID userId, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }

        return temporaryPasswordRepository.findByUserId(userId)
                .filter(temporaryPassword -> temporaryPassword.isValidAt(Instant.now()))
                .map(temporaryPassword ->
                        passwordEncoder.matches(rawPassword, temporaryPassword.getPasswordHash())
                )
                .orElse(false);
    }

    @Transactional
    public void deleteByUserId(UUID userId) {
        temporaryPasswordRepository.deleteByUserId(userId);
        log.info("TemporaryPassword deleted: userId={}", userId);
    }
}
