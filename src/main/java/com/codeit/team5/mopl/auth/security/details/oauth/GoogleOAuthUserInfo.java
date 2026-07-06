package com.codeit.team5.mopl.auth.security.details.oauth;

import com.codeit.team5.mopl.auth.entity.SocialProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GoogleOAuthUserInfo implements OAuthUserInfo {

    private final Map<String, Object> attributes;

    @Override
    public SocialProvider getProvider() {
        return null;
    }

    @Override
    public String getProviderUserId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return "";
    }
}
