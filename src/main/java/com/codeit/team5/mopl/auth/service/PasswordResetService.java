package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.dto.request.ResetPasswordRequest;
import com.codeit.team5.mopl.auth.event.TemporaryPasswordIssuedEvent;
import com.codeit.team5.mopl.auth.support.EmailNormalizer;
import com.codeit.team5.mopl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// “비밀번호 초기화 요청” 유스케이스 담당
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final TemporaryPasswordService temporaryPasswordService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String normalizeEmail = EmailNormalizer.normalize(request.email());

        userRepository.findByEmail(normalizeEmail)
                .ifPresent(user -> {
                    String tempPassword = temporaryPasswordService.issue(user);

                    eventPublisher.publishEvent(
                            new TemporaryPasswordIssuedEvent(user.getEmail(), tempPassword)
                    );

                    log.info("Password reset requested: userId={}", user.getId());
                });
    }
}
