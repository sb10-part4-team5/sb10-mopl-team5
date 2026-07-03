package com.codeit.team5.mopl.auth.event;

import com.codeit.team5.mopl.auth.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemporaryPasswordEventListener {

    private final MailService mailService;

    @Async
    @Retryable(
            retryFor = MailException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTemporaryPasswordIssued(TemporaryPasswordIssuedEvent event) {
        mailService.sendTemporaryPassword(event.email(), event.temporaryPassword());
        log.info("Temporary password email sent successfully");
    }

    @Recover
    public void recover(MailException e) {
        log.error("Temporary password email send failed after retries", e);
    }
}
