package com.codeit.team5.mopl.auth.service.model;

public record MailMessage(
        // 수신자 이메일
        String emailAddr,
        // 이메일 제목
        String subject,
        // 이메일 본문
        String content
) {

}
