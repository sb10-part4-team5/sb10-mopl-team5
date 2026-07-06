package com.codeit.team5.mopl.auth.security.details.oauth;

import com.codeit.team5.mopl.auth.entity.SocialProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KakaoOAuthUserInfo implements OAuthUserInfo {

    private final Map<String, Object> attributes;

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public String getProviderUserId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        return null;
    }

    @Override
    public String getName() {
        Map<String, Object> properties =
                (Map<String, Object>) attributes.get("properties");

        return String.valueOf(properties.get("nickname"));
    }
}
