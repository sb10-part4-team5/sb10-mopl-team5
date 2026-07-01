package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.entity.TemporaryPassword;
import com.codeit.team5.mopl.auth.exception.TemporaryPasswordNotFoundException;
import com.codeit.team5.mopl.auth.generator.TemporaryPasswordGenerator;
import com.codeit.team5.mopl.auth.repository.TemporaryPasswordRepository;
import com.codeit.team5.mopl.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        temporaryPasswordRepository.deleteByUserId(user.getId());

        String rawPassword = temporaryPasswordGenerator.generate();
        String passwordHash = passwordEncoder.encode(rawPassword);

        TemporaryPassword temporaryPassword =
                TemporaryPassword.create(user, passwordHash, Instant.now());

        temporaryPasswordRepository.save(temporaryPassword);

        return rawPassword;
    }

    public boolean matches(UUID userId, String rawPassword) {
        return temporaryPasswordRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .filter(temporaryPassword -> temporaryPassword.isValidAt(Instant.now()))
                .map(temporaryPassword ->
                        passwordEncoder.matches(rawPassword, temporaryPassword.getPasswordHash())
                )
                .orElse(false);
    }
}
