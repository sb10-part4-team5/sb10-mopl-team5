package com.codeit.team5.mopl.auth.security.details.oauth;

import com.codeit.team5.mopl.auth.entity.SocialProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KakaoOAuthUserInfo implements OAuthUserInfo {

    private final Map<String, Object> attributes;

    @Override
    public SocialProvider getProvider() {
        return null;
    }

    @Override
    public String getProviderUserId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        Map<String, Object> account =
                (Map<String, Object>) attributes.get("kakao_account");

        return (String) account.get("email");
    }

    @Override
    public String getName() {
        return "";
    }
}
