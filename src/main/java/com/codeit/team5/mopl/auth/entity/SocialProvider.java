package com.codeit.team5.mopl.auth.entity;

import com.codeit.team5.mopl.auth.exception.UnsupportedOAuthProviderException;
import java.util.Arrays;

public enum SocialProvider {
    GOOGLE,
    KAKAO;

    public static SocialProvider from(String registrationId) {
        return Arrays.stream(values())
                .filter(provider -> provider.name().equalsIgnoreCase(registrationId))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOAuthProviderException(registrationId));
    }
}
