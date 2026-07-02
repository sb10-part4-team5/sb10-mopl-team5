package com.codeit.team5.mopl.auth.event;

import com.codeit.team5.mopl.auth.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemporaryPasswordEventListener {

    private final MailService mailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTemporaryPasswordIssued(TemporaryPasswordIssuedEvent event) {
        mailService.sendTemporaryPassword(event.email(), event.temporaryPassword());
        log.info("Temporary password email sent successfully");
    }
}
