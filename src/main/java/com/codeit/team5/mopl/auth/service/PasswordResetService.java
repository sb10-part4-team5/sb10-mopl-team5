package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.dto.request.ResetPasswordRequest;
import com.codeit.team5.mopl.user.entity.User;
import com.codeit.team5.mopl.user.exception.UserNotFoundException;
import com.codeit.team5.mopl.user.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// “비밀번호 초기화 요청” 유스케이스 담당
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final TemporaryPasswordService temporaryPasswordService;
    private final MailService mailService;

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String normalizeEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmail(normalizeEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizeEmail));

        String tempPassword = temporaryPasswordService.issue(user);

        sendTemporaryPasswordAfterCommit(user.getEmail(), tempPassword);
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private void sendTemporaryPasswordAfterCommit(String email, String temporaryPassword) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        mailService.sendTemporaryPassword(email, temporaryPassword);
                    }
                }
        );
    }
}
