package com.codeit.team5.mopl.auth.entity;

import java.util.Arrays;

public enum SocialProvider {
    GOOGLE,
    KAKAO;

    public static SocialProvider from(String registrationId) {
        return Arrays.stream(values())
                .filter(provider -> provider.name().equalsIgnoreCase(registrationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 OAuth provider 입니다: " + registrationId
                ));
    }
}
