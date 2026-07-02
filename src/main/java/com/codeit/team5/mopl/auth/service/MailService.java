package com.codeit.team5.mopl.auth.service;

import com.codeit.team5.mopl.auth.service.model.MailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void send(MailMessage request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(request.emailAddr());
            message.setSubject(request.subject());
            message.setText(request.content());

            mailSender.send(message);

            log.info("Email sent: to={}", request.emailAddr());
        } catch (MailException e) {
            log.warn("Email send failed: to={}", request.emailAddr(), e);
            throw e;
        }
    }

    public void sendTemporaryPassword(String email, String temporaryPassword) {
        send(new MailMessage(
                email,
                "[MOPL] 임시 비밀번호 안내",
                """
                안녕하세요. MOPL입니다.
    
                요청하신 임시 비밀번호는 아래와 같습니다.
    
                임시 비밀번호: %s
    
                이 비밀번호는 발급 후 3분 동안만 사용할 수 있습니다.
                로그인 후 반드시 새 비밀번호로 변경해 주세요.
                """.formatted(temporaryPassword)
        ));
    }
}
