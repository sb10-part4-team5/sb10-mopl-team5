package com.codeit.team5.mopl.auth.exception;

import com.codeit.team5.mopl.auth.entity.SocialProvider;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class DuplicateSocialAccountException extends AuthException {

    public DuplicateSocialAccountException(SocialProvider provider) {
        super(HttpStatus.CONFLICT, "이미 존재하는 소셜 계정입니다.", Map.of("provider", provider));
    }
}
