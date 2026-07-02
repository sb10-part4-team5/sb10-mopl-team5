package com.codeit.team5.mopl.auth.dto.request;

public record EmailSendRequest(
        // 수신자 이메일
        String emailAddr,
        // 이메일 제목
        String subject,
        // 이메일 본문
        String content
) {

}
